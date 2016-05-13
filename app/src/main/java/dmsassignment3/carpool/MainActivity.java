package dmsassignment3.carpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.view.*;
import android.content.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        // googleApiClient.connect();
        // do we have to restart any child actitivies manually?
    } // onStart

    @Override
    protected void onStop() {
        // googleApiClient.disconnect();
        // do we have to stop any child activities manually?
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


    public void driverButtonClicked(View v) {
//        Toast.makeText(this, "Driver clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DriverActivity.class);
        startActivity(intent);
    }

    public void passengerButtonClicked(View v) {
        Toast.makeText(this, "Passenger clicked", Toast.LENGTH_SHORT).show();
      //  Intent intent = new Intent(this, PassengerActivity.class);
      //  startActivity(intent);
    }

}
