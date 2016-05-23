package dmsassignment3.carpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.MapFragment;

public class PassengerActivity extends LocationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

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
    } // onStart

    @Override
    protected void onStop() {
        super.onStop();
    } // onStop

    @Override
    protected void onPause() {
        super.onPause();
    } // onPause

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public int getUserType() {
        return User.PASSENGER;
    }

}
