package dmsassignment3.carpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.*;
import android.content.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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

    public void scanToolbar(View view) {
        new IntentIntegrator(this).setCaptureActivity(ToolbarCaptureActivity.class).initiateScan();
        Intent intent = new Intent(this, PointDistActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Scanned");
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
