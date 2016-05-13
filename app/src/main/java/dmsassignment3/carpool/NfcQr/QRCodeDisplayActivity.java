package dmsassignment3.carpool.NfcQr;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import dmsassignment3.carpool.R;

public class QRCodeDisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_display);

        // ImageView to display the QR code in.  This should be defined in your Activity's XML layout file
        ImageView imageView = (ImageView) findViewById(R.id.qrCode);

        String qrData = "Data I want to encode in QR code";
        int qrCodeDimention = 500;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
