package dmsassignment3.carpool.NfcQr;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;

import dmsassignment3.carpool.R;

public class WriteNfcTag extends AppCompatActivity implements View.OnClickListener {
    private NfcAdapter nfcAdapter;
    private boolean writeModeEnabled;
    private Button writeButton;
    private EditText textField;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_nfc_tag);
        setupActionBar();

        //
        //ImageView iv = (ImageView)findViewById(R.id.imageViewNfc);
        //iv.setImageResource(R.drawable.nfc);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        writeButton = (Button)findViewById(R.id.button1);
        writeButton.setOnClickListener(this);
        textField = (EditText) findViewById(R.id.editText1);
        statusView = (TextView)findViewById(R.id.textView1);
        writeModeEnabled = false;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setTitle("Write Your Name Tag");
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

    @Override
    public void onPause()
    {
        super.onPause();
        disableWriteMode();
    }
    @Override
    public void onNewIntent(Intent intent) {
        if(writeModeEnabled) {
            writeModeEnabled = false;

            // write to newly scanned tag
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(tag);
        }
    }

    @Override
    public void onClick(View arg0) {

        statusView.setText("WRITE MODE ENABLED, HOLD PHONE TO TAG");
        enableWriteMode();
    }

    private boolean writeTag(Tag tag) {

        // record that contains our custom data from textfield, using custom MIME_TYPE
        String textToSend = textField.getText().toString();
        byte[] payload = textToSend.getBytes();
        byte[] mimeBytes = "application/aut.seth".getBytes(Charset.forName("US-ASCII"));
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes,
                new byte[0], payload);
        NdefMessage message = new NdefMessage(new NdefRecord[] { record});

        try {
            // see if tag is already NDEF formatted
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    statusView.setText(" Read-only tag :-( ");
                    return false;
                }

                // work out how much space we need for the data
                int size = message.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    statusView.setText("Tag doesn't have enough free space :-(");
                    return false;
                }

                ndef.writeNdefMessage(message);
                statusView.setText("Tag written successfully.");
                return true;
            } else {
                // attempt to format tag
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        statusView.setText("Tag written successfully!\nClose this app and scan tag.");
                        return true;
                    } catch (IOException e) {
                        statusView.setText("Unable to format tag to NDEF.");
                        return false;
                    }
                } else {
                    statusView.setText("Tag doesn't appear to support NDEF format.");
                    return false;
                }
            }
        } catch (Exception e) {
            statusView.setText("Failed to write tag");
        }

        return false;
    }

    /**
     * Force this Activity to get NFC events first before phone OS
     */
    private void enableWriteMode() {
        writeModeEnabled = true;

        // set up a PendingIntent to open the app when a tag is scanned
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[] { tagDetected };

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }

    private void disableWriteMode() {
        nfcAdapter.disableForegroundDispatch(this);
    }
}
