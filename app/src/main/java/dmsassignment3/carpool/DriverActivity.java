package dmsassignment3.carpool;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class DriverActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnMapReadyCallback, LocationListener {

    GoogleApiClient googleApiClient;
    GoogleMap googleMap;
    LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Using Google's Location API, which differs from what was taught in class on page 135:
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
        locationRequest.setInterval(5000);
    } // onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

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


    // ConnectionCallback interface methods:
    // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
    } // onConnected

    @Override
    public void onConnectionSuspended(int cause) {
        stopLocationUpdates();
    }


    @Override
    public void onLocationChanged(Location location) {
        if (googleMap != null) {
            LatLng moveToLoc = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(moveToLoc);
            googleMap.moveCamera(cameraUpdate);
        }
    } // onLocationChanged

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //      if (this.googleMap == null)
        //          googleMap.addMarker(new MarkerOptions().position(new LatLng(-36.85, 174.76)).title("Auckland"));
        this.googleMap = googleMap;
    } // onMapReady



    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    } // startLocationUpdates

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    } // stopLocationUpdates

}
