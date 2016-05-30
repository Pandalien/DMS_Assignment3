package dmsassignment3.carpool;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.location.Location;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.preference.*;
import android.net.*;
import android.text.*;

import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.*;

import java.net.*;
import java.io.*;
import java.util.*;

/*
  LocationActivity handles user location updates and user session with the Carpool server.
  There should be only one instance of a LocationActivity at any time.
  The usertype intent bundle variable is used to select whether this session is for a
  driver or a passenger.
 */

public class LocationActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener,
        OnClickListener, AdapterView.OnItemClickListener, UserArrayAdapter.ActionButtonListener {

    public static String USERFILENAME = "User";
    static int REQUEST_CODE_LOGIN_ACTIVITY = 1;

    URL postURL;

    int usertype; // the target user type for this activity (User.DRIVER or User.PASSENGER)

    User user; // the current user of this app

    // following two fields valid for Passengers only:
    // (One driver can have many passengers, but a passenger can have only one driver).
    int transaction_id;
    int driver_id;


    // map
    GoogleApiClient googleApiClient;
    GoogleMap googleMap;
    LocationRequest locationRequest;
    Marker youMarker;
    Marker destMarker;

    // map update flags
    boolean firstMapUpdate;


    // UI controls
    ViewGroup startControls;
    ViewGroup liveControls;

    // start controls
    TextView proximityLabelStart;
    TextView proximityLabelEnd;
    EditText proximityEditText;
    Button beginButton;
    Button cancelButton;

    // live controls
    ListView userListView; // list of other users logged in to the system
    Button endButton;
    Button submitButton; // used by subclass to submit a special function for the selected user

    // the other users on the system which meet the criteria to show in the list,
    // see CarpoolerEJB.getUserList() on server side.
    List<User> userList;
    UserArrayAdapter userListAdapter;

    // maintain the users and markers in lookup tables
    HashMap<String, Marker> userMarkerHashMap;
    HashMap<String, User> userHashMap;

    // pointer to this activity for inner classes
    Activity activity;

    // keep cookies here between http requests
    java.net.CookieManager cookieJar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Using Google's Location API docs:
        // https://developers.google.com/android/reference/com/google/android/gms/location/LocationListener
        // https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi
        // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient
        // http://developer.android.com/training/location/retrieve-current.html
        // http://developer.android.com/training/location/receive-location-updates.html
        // https://developers.google.com/maps/documentation/android-api/start#the_maps_activity_java_file


        firstMapUpdate = true;

        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
                    .build();

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationRequest = newLocationRequest(1, 0);
        activity = this;
        postURL = null;

        Bundle extras = getIntent().getExtras();
        usertype = extras.getInt("usertype");


        user = new User();
        transaction_id = 0;
        driver_id = 0;


        youMarker = null;
        destMarker = null;

        cookieJar = new java.net.CookieManager();
        cookieJar.getCookieStore().removeAll();


        userMarkerHashMap = new HashMap();
        userHashMap = new HashMap();


        // UI controls
        startControls = (ViewGroup) findViewById(R.id.startControls);
        liveControls = (ViewGroup) findViewById(R.id.liveControls);
        beginButton = (Button) findViewById(R.id.beginButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        endButton = (Button) findViewById(R.id.endButton);

        proximityLabelStart = (TextView) findViewById(R.id.proximityLabelStart);
        proximityLabelEnd = (TextView) findViewById(R.id.proximityLabelEnd);
        proximityEditText = (EditText) findViewById(R.id.proximityEditText);

        beginButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        endButton.setOnClickListener(this);

        userListView = (ListView) findViewById(R.id.userListView);
        userList = new ArrayList<User>();
        userListAdapter = new UserArrayAdapter(this, R.layout.user_list_view_item_layout, userList);
//        userListAdapter.setActionButtonListener(this);
        userListView.setAdapter(userListAdapter);
        userListView.setOnItemClickListener(this);


        // must be called last, at the end of the constructor
        updateControls();
    } // onCreate

    @Override
    protected void onDestroy() {
        System.out.println("onDestroy");
        super.onDestroy();
    } // onDestroy

    @Override
    protected void onRestart() {
        System.out.println("onRestart");
        super.onRestart();
    } // onRestart

    @Override
    protected void onStart() {
        System.out.println("onStart");
        super.onStart();
        googleApiClient.connect();
    } // onStart

    @Override
    protected void onStop() {
        System.out.println("onStop");
        googleApiClient.disconnect();
        super.onStop();
    } // onStop

    @Override
    protected void onPause() {
        System.out.println("onPause");
        super.onPause();
    } // onPause

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();
//        if (googleApiClient.isConnected())
    }

    protected LocationRequest newLocationRequest(int numUpdates, long millis) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (numUpdates > 0)
            locationRequest.setNumUpdates(numUpdates);
        else
            locationRequest.setInterval(millis);
        return locationRequest;
    } // newLocationRequest


    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    } // startLocationUpdates

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    } // stopLocationUpdates


    // ConnectionCallback interface methods:
    // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
    } // onConnected

    @Override
    public void onConnectionSuspended(int cause) {
        stopLocationUpdates();
    } // onConnectionSuspended


    @Override
    public void onMapReady(GoogleMap googleMap) {
        //      if (this.googleMap == null)
        //          googleMap.addMarker(new MarkerOptions().position(new LatLng(-36.85, 174.76)).title("Auckland"));
        this.googleMap = googleMap;
        if (googleMap != null) {
            try {
                googleMap.setMyLocationEnabled(true);
            }
            catch (SecurityException e) {
                System.err.println(e.getMessage());
            }
            googleMap.setOnMapClickListener(this);
        }
    } // onMapReady


    @Override
    public void onLocationChanged(Location location) {
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());

        if (googleMap != null) {

            // only reposition camera depending on flags
            if (firstMapUpdate) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(point, 15);
                googleMap.moveCamera(cameraUpdate);
                firstMapUpdate = false;
            }

            // always update the youMarker
            if (youMarker != null)
                youMarker.setPosition(point);
            else
                youMarker = googleMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (isOnline()) {
            // send updated location coords to carpool server
            locationUpdate(point.latitude, point.longitude);
        }
    } // onLocationChanged


    @Override
    public void onMapClick(LatLng point) {
        if (!isOnline()) {
            // in set destination mode:
            if (destMarker != null)
                destMarker.setPosition(point);
            else
                destMarker = googleMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            updateControls();
        }
    } // onMapClick


    @Override
    public void onClick(View view) {
        if (view == beginButton) {
            login("login");
        } else if (view == cancelButton) {
            finish();
        } else if (view == endButton) {
            logout();
        }
    } // onClick


    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        User user = userList.get(position);
        if (user == null)
            return;

        Marker marker = userMarkerHashMap.get(user.getUsername());
        if (marker == null)
            return;

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(user.getLat(), user.getLng()), 15));
    } // onItemClick


    @Override
    public void onActionButtonClick(int position, User user) {
        userList.remove(position);
        // Driver clicked Collect
        if (user.getStatus() == User.PASSENGER) {
            transactionPending(user.getUserID());
        }
        // Driver clicked Cancel
        else if (user.getStatus() == User.PASSENGER_PENDING) {
            transactionCancelled(user.getTransactionId());
        }
        // Driver clicked End
        else if (user.getStatus() == User.PASSENGER_COLLECTED)
            transactionCompleted(user.getTransactionId(), youMarker.getPosition().latitude, youMarker.getPosition().longitude);
        userList.add(position, user);
        userListAdapter.notifyDataSetChanged();
    } // onActionButtonClick


    public void updateControls() {
        beginButton.setEnabled(destMarker != null);
        startControls.setVisibility(isOnline() ? View.GONE : View.VISIBLE);
        liveControls.setVisibility(isOnline() ? View.VISIBLE : View.GONE);

        if (usertype == User.DRIVER) {
            proximityLabelStart.setText(R.string.proximity_label_for_driver_start);
            proximityLabelEnd.setText(R.string.proximity_label_for_driver_end);
        }
        else if (usertype == User.PASSENGER) {
            proximityLabelStart.setText(R.string.proximity_label_for_passenger_start);
            proximityLabelEnd.setText(R.string.proximity_label_for_passenger_end);
        }
    } // updateControls


    public boolean isOnline() {
        return user.isDriver() || user.isPassenger();
    }


    // called when user has successfully logged in to the server
    public void loggedIn() {
        setTitle("Carpooler - " + user.getUsername());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String refreshStr = prefs.getString("location_refresh_interval", "4000");

        stopLocationUpdates();
        locationRequest = newLocationRequest(0, Integer.parseInt(refreshStr));
        startLocationUpdates();

        user.setStatus(usertype);
        updateControls();
    } // loggedIn

    public void loggedOut() {
        user.setStatus(User.OFFLINE);
        finish();
    } // loggedOut


    public void locationUpdated() {
    }

    public void updateUserList(JSONObject jsonUserList) {

        // create new user hash map from the list
        userHashMap.clear();

        if (jsonUserList != null) {
            Iterator<String> keys = jsonUserList.keys();
            while (keys.hasNext())
                try {
                    String key = keys.next();
                    User user = new User(jsonUserList.getJSONObject(key));
                    userHashMap.put(key, user);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

            // remove old users from userMarkerHashMap and also the google map
            for (String key : userMarkerHashMap.keySet())
                if (userHashMap.get(key) == null) {
                    Marker marker = userMarkerHashMap.get(key);
                    marker.remove();
                    userMarkerHashMap.remove(key);
                }

            // update or add new markers as required to match userHashMap
            for (User user : userHashMap.values()) {
                String key = user.getUsername();
                LatLng point = new LatLng(user.getLat(), user.getLng());
                Marker marker = userMarkerHashMap.get(key);
                if (marker != null)
                    marker.setPosition(point);
                else {
                    marker = googleMap.addMarker(new MarkerOptions()
                            .position(point)
                            .title(key)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                    userMarkerHashMap.put(key, marker);
                }
            }
        }

        // finally, update the UI
        userList.clear();
        userList.addAll(userHashMap.values());
        userListAdapter.notifyDataSetChanged();
    } // updateUserList



    // HTTP communication functions:
    /*
    Login procedure:
    1. if login details are stored locally, use them and call Login on the server.
    2. if they are not stored or in case of error, show LoginActivity prompt.
    3. if user wishes to create a new account, call CreateAccount on the server,
       else call Login.
    4. On a successful Login, a session cookie should be returned by the server.

    Continuous session:
    1. Set the session cookie.
    2. Upload up-to-date latitude/longitude for this user.
    3. Receive back a list of Passengers or Drivers, depending on whether the user is a Driver or
       Passenger.

    Special requests:
    1. If Passenger is selected, send Collect message to server.
    2. If Driver or Passenger cancels, send Cancel message to server.
    -> or wait until next loc/update message?

    Transaction Table needs to keep an entry for whether a driver is to collect a passenger (so that
    the passenger is removed from the list for other drivers), tagged on action, and tagged off action
    (which could remove the entry from the Transaction Table after updating User points and so on).
     */


    public static JSONObject readJSONObject(InputStream in) {
        JSONObject result = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                stringBuilder.append(line);
            result = new JSONObject(stringBuilder.toString());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return result;
    } // readJSONObject


    public static void writeJSONObject(OutputStream out, JSONObject jsonObject) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(jsonObject.toString());
            writer.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // writeJSONObject


    // --- Username/password handling: ---
    // Username & password is stored locally in file accessible only to the app.
    // If anything is missing or a login fails, user will be asked to enter a username/password,
    // or to create a new account.


    public JSONObject getUserPass() {
        JSONObject result = null;
        try {
            FileInputStream in = openFileInput(USERFILENAME);
            result = readJSONObject(in);
            in.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        if (result != null && result.has("username") && result.has("password"))
            return result;
        else
            return null;
    } // getUserPass


    public void saveUserPass(String username, String password) {
        try {
            FileOutputStream out = openFileOutput(USERFILENAME, Context.MODE_PRIVATE);
            JSONObject userpass = new JSONObject();
            userpass.put("username", username);
            userpass.put("password", password);
            writeJSONObject(out, userpass);
            out.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // saveUserPass


    public void askUserPass(String function, String username) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("function", function);
        intent.putExtra("username", username);
        intent.putExtra("usertype", usertype);
        startActivityForResult(intent, REQUEST_CODE_LOGIN_ACTIVITY);
    } // askUserPass


    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent intent) {
        if (reqCode == REQUEST_CODE_LOGIN_ACTIVITY && resCode == RESULT_OK) {
            Bundle bundle = intent.getExtras();
            String function = bundle.getString("function", "");
            String username = bundle.getString("username", "");
            String password = bundle.getString("password", "");
            saveUserPass(username, password);
            executeComm(makeLoginCommand(function, username, password),
                    function.equals("createaccount") ? new CreateAccountComm() : new LoginComm());
        } else if (reqCode == REQUEST_CODE_LOGIN_ACTIVITY && resCode == RESULT_CANCELED) {
            finish();
        }
    } // onActivityResult



    // ------ HTTP command: Login or Create Account ------


    protected JSONObject makeLoginCommand(String function, String username, String password) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", function);
            cmd.put("username", username);
            cmd.put("password", password);
            cmd.put("usertype", usertype);
            if (youMarker != null) {
                cmd.put("lat", youMarker.getPosition().latitude);
                cmd.put("lng", youMarker.getPosition().longitude);
            }
            if (destMarker != null) {
                cmd.put("dest_lat", destMarker.getPosition().latitude);
                cmd.put("dest_lng", destMarker.getPosition().longitude);
            }

            double proximity;
            try {
                proximity = Double.parseDouble(proximityEditText.getText().toString());
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                proximity = 1;
            }
            cmd.put("proximity", proximity);


            // update/create a new user object using the appropriate fields from this login command
            // if the log-in is successful the status is updated by loggedIn()
            user = new User(cmd);

        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
        return cmd;
    } // makeLoginCommand


    protected void login(String function) {
        JSONObject userpass = getUserPass();
        if (userpass != null) {
            JSONObject cmd = makeLoginCommand(
                    function,
                    userpass.optString("username"),
                    userpass.optString("password"));
            executeComm(cmd,
                    function.equals("createaccount") ? new CreateAccountComm() : new LoginComm());
        } else
            askUserPass(function, "");
    } // login


    // create a new subclass for each function call to the server,
    // for example, this class handles the CreateAccount response:
    private class LoginComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            loggedIn();
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
            JSONObject userpass = getUserPass();
            askUserPass("login", userpass.optString("username"));
        }

    } // LoginComm


    private class CreateAccountComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            loggedIn();
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
            JSONObject userpass = getUserPass();
            askUserPass("createaccount", userpass.optString("username"));
        }

    } // CreateAccounComm



    // ------ HTTP command: Logout ------


    protected void logout() {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "logout");
            executeComm(cmd, new LogoutComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // logout


    private class LogoutComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            loggedOut();
        }

        protected void error(String result, JSONObject response) {
            // should probably log the error here
        }

    } // LogoutComm



    // ------ HTTP command: Update Location ------
    // sends location and receives back other information tailored for the user


    protected void locationUpdate(double lat, double lng) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "locationupdate");
            cmd.put("lat", lat);
            cmd.put("lng", lng);
            executeComm(cmd, new LocationUpdateComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // locationUpdate


    private class LocationUpdateComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            // these fields are returned to passenger in case of a valid transaction
            if (response.has("transaction"))
                try {
                    JSONObject jsonTransaction = response.getJSONObject("transaction");
                    transaction_id = jsonTransaction.optInt("transaction_id", 0);
                    driver_id = jsonTransaction.optInt("driver_id", 0);
                }
                catch (Exception e) {
                    System.err.println(e);
                }

            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e);
                }
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // LocationUpdateComm



    // ------ HTTP command: Pending ------
    // Sent by Driver's phone


    protected void transactionPending(int passenger_id) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "pending");
            cmd.put("passenger_id", passenger_id);
            executeComm(cmd, new TransactionPendingComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionPending


    private class TransactionPendingComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionPendingComm



    // ------ HTTP command: Cancelled ------


    protected void transactionCancelled(int transaction_id) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "cancelled");
            cmd.put("transaction_id", transaction_id);
            executeComm(cmd, new TransactionCancelledComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionCancelled


    private class TransactionCancelledComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionCancelledComm



    // ------ HTTP command: In Progress ------
    // Sent by passenger's phone.
    // The NFC/QR match is verified by the phone, if successful then sends this message:


    protected void transactionInProgress(int transaction_id, double lat, double lng) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "command");
            cmd.put("transaction_id", transaction_id);
            cmd.put("lat", lat);
            cmd.put("lng", lng);
            executeComm(cmd, new TransactionInProgressComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionInProgress


    private class TransactionInProgressComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {}

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionInProgressComm



    // ------ HTTP command: Completed ------
    // Sent by passenger's phone also.
    // Can be done by NFC/QR scan also ("tag off"), but could also be a button.


    protected void transactionCompleted(int transaction_id, double lat, double lng) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "command");
            cmd.put("transaction_id", transaction_id);
            cmd.put("lat", lat);
            cmd.put("lng", lng);
            executeComm(cmd, new TransactionCompleted());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionCompleted


    private class TransactionCompleted extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {}

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionCompleted


/*
    // ------ HTTP command: Template ------


    protected void command() {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "command");
            executeComm(cmd, new HTTPComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // command


    private class HTTPComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {}

        protected void error(String result, JSONObject response) {}

    } // HTTPComm
*/


    // --- HTTP base routines ---


    protected URL getPostURL() {
        URL url = null;
        try {
            // get the host:port from the shared preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String http_server = prefs.getString("http_server", "Local").toLowerCase() + "_addr";
            String hostport = prefs.getString(http_server, "localhost:8080");
            url = new URL("http://" + hostport + "/CarpoolServer/Carpooler");
        } catch (Exception e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return url;
    } // getPostURL


    // use this method to execute a new HTTP task.
    // sends back any cookies received from the last command.
    protected void executeComm(JSONObject cmd, HttpJsonCommunicator handler) {
        URL url = postURL != null ? postURL : getPostURL();
        String cookies = null;
        if (cookieJar.getCookieStore().getCookies().size() > 0)
            cookies = TextUtils.join(";",  cookieJar.getCookieStore().getCookies());
        handler.execute(url, cmd, cookies);
    } // executeComm


    // HttpJsonCommunicator abstract base class:

    // based on code example from page 120,
    // modified to pass a JSON Object in a POST message body to the URL,
    // and expects a JSON Object back.
    // To use, make a subclass of HttpJsonCommunicator and override ok() and error() handlers.
    // Call using executeComm function above.
    // Automatically handles cookies, saving them to the LocationActivity's cookieJar.
    // ref:
    // https://developer.android.com/reference/android/os/AsyncTask.html
    // http://stackoverflow.com/questions/16150089/how-to-handle-cookies-in-httpurlconnection-using-cookiemanager
    private abstract class HttpJsonCommunicator extends AsyncTask<Object, Void, JSONObject> {

        protected JSONObject makeResult(String name, String message) {
            JSONObject jsonResult = new JSONObject();
            try {
                jsonResult.put(name, message);
            } catch (Exception e) {
                return null;
            }
            return jsonResult;
        } // makeResult


        protected List<String> cookiesHeader;

        protected void onPreExecute() {
            cookiesHeader = null;
        }

        // method executed for task in a new thread
        protected JSONObject doInBackground(Object... objects) {
            try {
                if (objects == null || objects.length < 1)
                    return makeResult("LocalError", "No URL specified.");
                if (objects.length < 2)
                    return makeResult("LocalError", "No message body.");

                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected())
                    return makeResult("LocalError", "No network connection.");

            } catch (Exception e) {
                return null;
            }

            URL url = (URL) objects[0];
            JSONObject jsonObject = (JSONObject) objects[1];
            String cookies = (String)objects[2];

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                if (cookies != null && cookies.length() > 0)
                  urlConnection.setRequestProperty("Cookie", cookies);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                // post to the server
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(jsonObject.toString());
                writer.close();

                // read response from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder responsebody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    responsebody.append(line);

                jsonObject = new JSONObject(responsebody.toString());

                cookiesHeader = urlConnection.getHeaderFields().get("Set-Cookie");
            } catch (Exception e) {
                try {
                    jsonObject = makeResult("LocalException", e.getMessage());
                } catch (Exception je) {
                    jsonObject = null;
                }
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return jsonObject;
        } // doInBackground

        // method executed in UI thread once task completed
        protected void onPostExecute(JSONObject response) {
            // save any cookies from the server to our local cookieJar
            if (cookiesHeader != null)
                for (String cookie: cookiesHeader)
                    cookieJar.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));

            // some default handling of our JSON response body:
            try {
                if (response == null)
                    Toast.makeText(activity, "No response.", Toast.LENGTH_LONG).show();
                else if (response.has("LocalError"))
                    Toast.makeText(activity, "Local Error: " + response.optString("LocalError"), Toast.LENGTH_LONG).show();
                else if (response.has("LocalException"))
                    Toast.makeText(activity, "Local Exception: " + response.optString("LocalException"), Toast.LENGTH_LONG).show();
                else if (response.has("result")) {
                    String result = response.getString("result");
                    if (result.equals("OK"))
                        ok(response);
                    else
                        error(result, response);
                } else
                    Toast.makeText(activity, response.toString(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(activity, "onPostExecute Exception: " + e.toString(), Toast.LENGTH_LONG).show();
            }
        } // onPostExecute

        // override ok and error handlers in subclasses for custom processing (in the main thread)
        protected abstract void ok(JSONObject response);
        protected abstract void error(String result, JSONObject response);

    } // HttpJsonCommunicator


} // LocationActivity
