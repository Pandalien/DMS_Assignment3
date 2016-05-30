package dmsassignment3.carpool.NfcQr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.Charset;

import dmsassignment3.carpool.R;

public class TagonTagoffActivity extends AppCompatActivity{
    //NfcAdapter mNfcAdapter;
    TextView tvPassenger;
    TextView tvDriver;
    SharedPreferences prefs;
    String user;
    String driver;
    String role;
    int miles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tagon_tagoff);
        setupActionBar();

        tvPassenger = (TextView) findViewById(R.id.tvPassenger);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        user = prefs.getString("username", "");
        tvPassenger.setText(user);

/*
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available, you can use QR scanner", Toast.LENGTH_LONG).show();
            //finish();
            //return;
        }else{
            // Register callback
            mNfcAdapter.setNdefPushMessageCallback(this, this);
        }
*/
        //get NFC tag name
        tvDriver = (TextView)findViewById(R.id.tvDriver);
        Intent intent = getIntent();
        //Check mime type, get ndef message  from intent and display the message in text view
        if(intent.getType() != null && intent.getType().equals("application/aut.dms.carpooler")) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord record = msg.getRecords()[0];
            String driverName = new String(record.getPayload());
            tvDriver.setText(driverName);
        }
    }
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setTitle("Tag on/ Tag off");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onScanQr(View view){
        new IntentIntegrator(this).setCaptureActivity(ToolbarCaptureActivity.class).initiateScan();
    }

    public void onCreateQr(View view){
        Intent intent = new Intent(this, QRCodeDisplayActivity.class);
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
                createCarpoolRecord(result.getContents());
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void createCarpoolRecord(String driverName){
        tvDriver.setText(driverName);
    }

    public void createTagonRecord(View view){
        Toast.makeText(this, "createTagonRecord", Toast.LENGTH_LONG).show();
    }

    public void createTagoffRecord(View view){
        Toast.makeText(this, "createTagoffRecord", Toast.LENGTH_LONG).show();
    }
}
