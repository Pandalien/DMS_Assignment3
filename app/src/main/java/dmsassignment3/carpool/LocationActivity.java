package dmsassignment3.carpool;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.location.Location;
import android.webkit.CookieManager;
import android.widget.Toast;
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
import java.util.Map.*;




/*
  LocationActivity is the base class for DriverActivity and PassengerActivity.
  Handles user location updates and user session with the Carpool server.
  There should be only one instance of a Driver or Passenger Activity at any time.
 */

public class LocationActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {

    public static String USERFILENAME = "User";
    static int REQUEST_CODE_LOGIN_ACTIVITY = 1;

    URL postURL;

    int userstatus;

    User user;

    GoogleApiClient googleApiClient;
    GoogleMap googleMap;
    LocationRequest locationRequest;
    Marker youMarker;
    Marker destMarker;


    Activity activity;

    java.net.CookieManager cookieJar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_location);

        // Using Google's Location API docs:
        // https://developers.google.com/android/reference/com/google/android/gms/location/LocationListener
        // https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi
        // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient
        // http://developer.android.com/training/location/retrieve-current.html
        // http://developer.android.com/training/location/receive-location-updates.html
        // https://developers.google.com/maps/documentation/android-api/start#the_maps_activity_java_file

        if (googleApiClient == null)
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
                    .build();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locationRequest.setInterval(4000);
        locationRequest.setNumUpdates(1); // initial location refresh

        activity = this;
        postURL = null;

        userstatus = User.OFFLINE;

        user = new User();


        youMarker = null;
        destMarker = null;

        cookieJar = new java.net.CookieManager();
    } // onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();
    } // onDestroy

    @Override
    protected void onRestart() {
        super.onRestart();
    } // onRestart

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    } // onStart

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    } // onStop

    @Override
    protected void onPause() {
        super.onPause();

    } // onPause

    @Override
    protected void onResume() {
        super.onResume();
//        if (googleApiClient.isConnected())
    }


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
    public void onLocationChanged(Location location) {
        if (googleMap != null) {
            LatLng moveToLoc = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(moveToLoc, 15);
            googleMap.moveCamera(cameraUpdate);

            if (youMarker != null && isOnline())
                youMarker.setPosition(moveToLoc);
            else if (youMarker == null)
                youMarker = googleMap.addMarker(new MarkerOptions()
                        .position(moveToLoc)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (isOnline()) {
            // send updated location coords to carpool server
            locationUpdate(location.getLatitude(), location.getLongitude());
        }
    } // onLocationChanged

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //      if (this.googleMap == null)
        //          googleMap.addMarker(new MarkerOptions().position(new LatLng(-36.85, 174.76)).title("Auckland"));
        this.googleMap = googleMap;
        if (googleMap != null) {
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            googleMap.setOnMapClickListener(this);
        }
    } // onMapReady


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


    public void updateControls() {
    } // updateControls


    public int getUserType() {
        return User.OFFLINE;
    }

    public boolean isOnline() {
        return user.isDriver() || user.isPassenger();
    }

    // overridden by descendent; called when user has successfully logged in to the server
    public void loggedIn() {
//        setTitle("Carpooler - " + user.getUsername());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String refreshStr = prefs.getString("location_refresh_interval", "4000");

        stopLocationUpdates();
        locationRequest.setInterval(Integer.parseInt(refreshStr));
        startLocationUpdates();
    } // loggedIn

    public void locationUpdated() {
    }

    public void updateUserList(JSONObject jsonUserList) {}


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
        intent.putExtra("usertype", getUserType());
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
            cmd.put("usertype", getUserType());
            if (youMarker != null) {
                cmd.put("lat", youMarker.getPosition().latitude);
                cmd.put("lng", youMarker.getPosition().longitude);
            }
            if (destMarker != null) {
                cmd.put("dest_lat", destMarker.getPosition().latitude);
                cmd.put("dest_lng", destMarker.getPosition().longitude);
            }
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
            finish();
        }

        protected void error(String result, JSONObject response) {
            // should probably log the error here
        }

    } // LocationUpdateComm



    // ------ HTTP command: Update Location ------


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
            if (response.has("userlist"))
                try {
                    updateUserList(response.getJSONObject("userlist"));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
        }

        protected void error(String result, JSONObject response) {
            // should perhaps silently log the error here
        }

    } // LocationUpdateComm



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
    // Automatically handles cookies, saving them to the LocationActivity's cookieJar member.
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
                Toast.makeText(activity, "onPostExecute Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } // onPostExecute

        // override ok and error handlers in subclasses for custom processing (in the main thread)
        protected abstract void ok(JSONObject response);
        protected abstract void error(String result, JSONObject response);

    } // HttpJsonCommunicator


} // LocationActivity
