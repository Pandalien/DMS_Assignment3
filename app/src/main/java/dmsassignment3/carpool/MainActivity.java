package dmsassignment3.carpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.view.*;
import android.content.*;

import java.io.FileOutputStream;

import dmsassignment3.carpool.NfcQr.TagonTagoffActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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


    public void driverButtonClicked(View v) {
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra("usertype", User.DRIVER);
        startActivity(intent);
    }

    public void passengerButtonClicked(View v) {
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra("usertype", User.PASSENGER);
        startActivity(intent);
    }

    public void onLogging(View view) {
        Intent intent = new Intent(this, TagonTagoffActivity.class);
        startActivity(intent);
    }

    public void onSettingsClicked(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onHistoryClicked(View view) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }
}
