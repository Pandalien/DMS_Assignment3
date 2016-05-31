package dmsassignment3.carpool;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences.*;
import android.nfc.*;
import android.nfc.tech.*;
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

    // following field valid for Passengers only:
    // (One driver can have many passengers, but a passenger can have only one driver).
    User driver;


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
    ViewGroup passengerControls;

    // start controls
    TextView proximityLabelStart;
    TextView proximityLabelEnd;
    EditText proximityEditText;
    Button beginButton;
    Button cancelButton;

    // live controls
    ListView userListView; // list of other users logged in to the system
    Button endButton;

    // passenger controls
    TextView passengerTransactionTextView;
    TextView driverUsernameTextView;
    Button passengerCancelButton;

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


    //variables for NFC
    NfcAdapter mNfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    String[][] techList;


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


        user = null;
        driver = null;


        youMarker = null;
        destMarker = null;

        cookieJar = new java.net.CookieManager();
        cookieJar.getCookieStore().removeAll();


        userMarkerHashMap = new HashMap();
        userHashMap = new HashMap();


        // UI controls
        startControls = (ViewGroup) findViewById(R.id.startControls);
        liveControls = (ViewGroup) findViewById(R.id.liveControls);
        passengerControls = (ViewGroup) findViewById(R.id.passengerControls);

        // start controls
        beginButton = (Button) findViewById(R.id.beginButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        proximityLabelStart = (TextView) findViewById(R.id.proximityLabelStart);
        proximityLabelEnd = (TextView) findViewById(R.id.proximityLabelEnd);
        proximityEditText = (EditText) findViewById(R.id.proximityEditText);

        beginButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        // live controls
        endButton = (Button) findViewById(R.id.endButton);
        endButton.setOnClickListener(this);

        userListView = (ListView) findViewById(R.id.userListView);
        userList = new ArrayList<User>();
        userListAdapter = new UserArrayAdapter(this, R.layout.user_list_view_item_layout, userList);
        userListAdapter.setActionButtonListener(this);
        userListView.setAdapter(userListAdapter);
        userListView.setOnItemClickListener(this);

        // passenger controls
        passengerCancelButton = (Button) findViewById(R.id.passengerCancelButton);
        passengerTransactionTextView = (TextView) findViewById(R.id.passengerTransactionTextView);
        driverUsernameTextView = (TextView) findViewById(R.id.driverUsernameTextView);

        passengerCancelButton.setOnClickListener(this);


        // must be called last, at the end of the constructor
        updateControls();
        copyUserpassToSharedPrefs();

        //setup Nfc Listener
        setupNfcListener();
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
        enableNfcListener(false);
        super.onPause();
    } // onPause

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();
        enableNfcListener(true);
//        if (googleApiClient.isConnected())
    }


    private void setupNfcListener() {
        //call this in onCreate

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try{
            tag.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            tech.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        intentFiltersArray = new IntentFilter[] { tag, ndef, tech };

        techList = new String[][] { new String[] { NfcA.class.getName(),
                NfcB.class.getName(), NfcF.class.getName(),
                NfcV.class.getName(), IsoDep.class.getName(),
                MifareClassic.class.getName(),
                MifareUltralight.class.getName(), Ndef.class.getName() } };
    } // setupNfcListener

    private void enableNfcListener(boolean e){
        //in the onResume method, you should enable this
        //and disable in onPause
        if (mNfcAdapter == null) {
            return;
        }
        if(e){
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techList);
        }
        else{
            mNfcAdapter.disableForegroundDispatch(this);
        }
    } // enableNfcListener

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // reag TagTechnology object...
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // read NDEF message...
            String nfcMessage = null;
            //get NFC tag name
            //Intent intent = getIntent();
            //Check mime type, get ndef message  from intent and display the message in text view
            if(intent.getType() != null && intent.getType().equals("application/aut.dms.carpooler")) {
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                NdefRecord record = msg.getRecords()[0];
                nfcMessage = new String(record.getPayload());
            }

            // -----
            // Check NFC message matches the driver's username:
            // If positive match, transmit a transactionCollected message for this passenger

            if (nfcMessage == null)
                Toast.makeText(this, "Failed to read tag.", Toast.LENGTH_LONG).show();
            else if (driver != null && user != null) {
                if (driver.getUsername().equals(nfcMessage)) {
                    if (user.getStatus() == User.PASSENGER_PENDING)
                        transactionCollected(driver.getUserID(), user.getLat(), user.getLng());
                    else if (user.getStatus() == User.PASSENGER_COLLECTED)
                        transactionCompleted(driver.getUserID(), user.getLat(), user.getLng());
                }
                else
                  Toast.makeText(this, "Driver does not match.", Toast.LENGTH_LONG).show();
            }

            // -----
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

        }
    } // onNewIntent




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
        } else if (view == passengerCancelButton) {
            transactionCancelled(user.getUserID());
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
    public void onActionButtonClick(int position, User passenger) {
        userList.remove(position);
        // Driver clicked Collect
        if (passenger.getStatus() == User.PASSENGER) {
            transactionPending(passenger.getUserID());
        }
        // Driver clicked Cancel
        else if (passenger.getStatus() == User.PASSENGER_PENDING) {
            transactionCancelled(passenger.getUserID());
        }
        // Driver clicked End
        else if (passenger.getStatus() == User.PASSENGER_COLLECTED)
            transactionCompleted(passenger.getUserID(), youMarker.getPosition().latitude, youMarker.getPosition().longitude);
        userList.add(position, passenger);
        userListAdapter.notifyDataSetChanged();
    } // onActionButtonClick


    public void updateControls() {

        String title = getResources().getString(R.string.app_name);

        if (driver == null && user == null) {
            // start controls
            startControls.setVisibility(View.VISIBLE);
            liveControls.setVisibility(View.GONE);
            passengerControls.setVisibility(View.GONE);

            beginButton.setEnabled(destMarker != null);

            if (usertype == User.DRIVER) {
                proximityLabelStart.setText(R.string.proximity_label_for_driver_start);
                proximityLabelEnd.setText(R.string.proximity_label_for_driver_end);
            } else if (usertype == User.PASSENGER) {
                proximityLabelStart.setText(R.string.proximity_label_for_passenger_start);
                proximityLabelEnd.setText(R.string.proximity_label_for_passenger_end);
            }
        }
        else if (driver == null && user != null) {
            // driver/passenger live controls
            startControls.setVisibility(View.GONE);
            liveControls.setVisibility(View.VISIBLE);
            passengerControls.setVisibility(View.GONE);

            title = title + " - " + user.getUsername();
            if (user.isDriver())
                title = title + " is driving.";
            else
                title = title + " is a passenger.";
        }
        else if (driver != null && user != null) {
            // passenger controls
            startControls.setVisibility(View.GONE);
            liveControls.setVisibility(View.GONE);
            passengerControls.setVisibility(View.VISIBLE);

            if (user.getStatus() == User.PASSENGER_PENDING)
                passengerTransactionTextView.setText("You will be collected by");
            else if (user.getStatus() == User.PASSENGER_COLLECTED)
                passengerTransactionTextView.setText("You have been collected by");
            else if (user.getStatus() == User.PASSENGER_COMPLETED)
                passengerTransactionTextView.setText("You have completed your trip with");

            driverUsernameTextView.setText(driver.getUsername());

            title = title + " - " + user.getUsername();
        }


        setTitle(title);

    } // updateControls


    public boolean isOnline() {
        return user != null && (user.getStatus() > 0);
    }


    // called when user has successfully logged in to the server
    public void loggedIn() {
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

            copyUserpassToSharedPrefs();
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


    public void copyUserpassToSharedPrefs() {
        String username = "";
        String password = "";
        JSONObject userpass = getUserPass();
        if (userpass != null) {
            username = userpass.optString("username");
            password = userpass.optString("password");
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("userpass", password);
        editor.commit();
    } // copyUserpassToSharedPrefs


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
            // update: actually the user object is recreated after login from information sent by the
            // server. This may not be needed here.
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
            if (response.has("user"))
                try {
                    user = new User(response.optJSONObject("user"));
                }
                catch (Exception e) {
                    System.err.println(e);
                }
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
            if (response.has("user"))
                try {
                    user = new User(response.optJSONObject("user"));
                }
                catch (Exception e) {
                    System.err.println(e);
                }
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
            driver = null;
            if (response.has("driver"))
                try {
                    driver = new User(response.getJSONObject("driver"));
                } catch (Exception e) {
                    System.err.println(e);
                }

            // update passenger status:
            if (response.has("status"))
                try {
                    user.setStatus(response.getInt("status"));
                } catch (Exception e) {
                    System.err.println(e);
                }
            else
                user.setStatus(usertype);


            // user list
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e);
                }

            updateControls();
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
            updateControls();
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionPendingComm



    // ------ HTTP command: Cancelled ------


    protected void transactionCancelled(int other_user_id) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "cancelled");
            cmd.put("other_user_id", other_user_id);
            executeComm(cmd, new TransactionCancelledComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionCancelled


    private class TransactionCancelledComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            if (user.getStatus() >= User.PASSENGER_PENDING) {
                user.setStatus(User.PASSENGER);
                driver = null;
            }
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            updateControls();
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionCancelledComm



    // ------ HTTP command: Collected ------
    // Sent by passenger's phone.
    // The NFC/QR match is verified by the phone, if successful then sends this message:


    protected void transactionCollected(int driver_id, double lat, double lng) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "collected");
            cmd.put("driver_id", driver_id);
            cmd.put("lat", lat);
            cmd.put("lng", lng);
            executeComm(cmd, new TransactionInProgressComm());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionCollected


    private class TransactionInProgressComm extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
            user.setStatus(User.PASSENGER_COLLECTED);
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            updateControls();
        }

        protected void error(String result, JSONObject response) {
            if (result != null && result.length() > 0)
                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
        }

    } // TransactionInProgressComm



    // ------ HTTP command: Completed ------
    // Sent by passenger's phone.
    // Can be done by NFC/QR scan also ("tag off"), but could also be a button


    protected void transactionCompleted(int driver_id, double lat, double lng) {
        JSONObject cmd = new JSONObject();
        try {
            cmd.put("function", "completed");
            cmd.put("driver_id", driver_id);
            cmd.put("lat", lat);
            cmd.put("lng", lng);
            executeComm(cmd, new TransactionCompleted());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    } // transactionCompleted


    private class TransactionCompleted extends HttpJsonCommunicator {

        protected void ok(JSONObject response) {
/*
            user.setStatus(User.PASSENGER_COMPLETED);
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            updateControls();
*/
            String msg = "Trip completed.";
            if (driver != null)
                msg = "Your trip with " + driver.getUsername() + " is completed.";
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            finish();
        }

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
