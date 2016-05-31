package dmsassignment3.carpool.NfcQr;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
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
    //variables for NFC
    NfcAdapter mNfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    String[][] techList;

    //other variables
    TextView tvPassenger;
    TextView tvDriver;
    SharedPreferences prefs;
    String user;
    String driver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tagon_tagoff);
        setupActionBar();

        tvPassenger = (TextView) findViewById(R.id.tvPassenger);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        user = prefs.getString("username", "");
        tvPassenger.setText(user);
        tvDriver = (TextView)findViewById(R.id.tvDriver);

        //setup Nfc Listener
        setupNfcListener();
    }

    private void setupNfcListener(){
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
    }

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

    @Override
    protected void onPause() {
        super.onPause();
        enableNfcListener(false);
    }

    @Override
    protected void onResume(){
        super.onResume();
        enableNfcListener(true);
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // reag TagTechnology object...
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // read NDEF message...
            String nfcMessage;
            //get NFC tag name
            //Intent intent = getIntent();
            //Check mime type, get ndef message  from intent and display the message in text view
            if(intent.getType() != null && intent.getType().equals("application/aut.dms.carpooler")) {
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                NdefRecord record = msg.getRecords()[0];
                nfcMessage = new String(record.getPayload());
            }else{
                nfcMessage = "Failed to get NFC messages";
            }
            Toast.makeText(this, nfcMessage, Toast.LENGTH_LONG).show();
            tvDriver.setText(nfcMessage);
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

        }
    }
}
