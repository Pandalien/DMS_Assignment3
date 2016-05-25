package dmsassignment3.carpool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.view.View.*;
import android.content.*;

public class LoginActivity extends AppCompatActivity implements OnClickListener, RadioGroup.OnCheckedChangeListener {

    RadioGroup loginRadioGroup;
    RadioButton loginRadioButton;
    RadioButton createAccountRadioButton;
    EditText usernameEditText;
    EditText passwordEditText;
    EditText verifyPasswordEditText;
    TextView verifyPasswordTextView;
    Button okButton;
    Button cancelButton;

    int usertype;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginRadioGroup = (RadioGroup) findViewById(R.id.loginRadioGroup);
        loginRadioButton = (RadioButton) findViewById(R.id.loginRadioButton);
        createAccountRadioButton = (RadioButton) findViewById(R.id.createAccountRadioButton);
        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        verifyPasswordEditText = (EditText) findViewById(R.id.verifyPasswordEditText);
        verifyPasswordTextView = (TextView) findViewById(R.id.verifyPasswordTextView);
        okButton = (Button) findViewById(R.id.okButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        loginRadioGroup.setOnCheckedChangeListener(this);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        String function = extras.getString("function");
        usernameEditText.setText(extras.getString("username"));
        usertype = extras.getInt("usertype");

        loginRadioGroup.check(function.equals("createaccount") ? R.id.createAccountRadioButton : R.id.loginRadioButton);

        updateControls();
    } // onCreate

    public void updateControls() {
        int verifyPasswordVisibility = loginRadioButton.isChecked() ? View.INVISIBLE : View.VISIBLE;
        verifyPasswordTextView.setVisibility(verifyPasswordVisibility);
        verifyPasswordEditText.setVisibility(verifyPasswordVisibility);
    } // updateControls

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateControls();
    } // onCheckedChanged

    @Override
    public void onClick(View view) {
        if (view == okButton) {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String vpassword = verifyPasswordEditText.getText().toString();
            if (createAccountRadioButton.isChecked() && !password.equals(vpassword))
                Toast.makeText(this, "Password verification mismatch.", Toast.LENGTH_LONG).show();
            else {
                Intent intent = new Intent(this, LocationActivity.class);
                intent.putExtra("function", createAccountRadioButton.isChecked() ? "createaccount" : "login");
                intent.putExtra("username", username);
                intent.putExtra("password", password);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        else if (view == cancelButton) {
            setResult(RESULT_CANCELED);
            finish();
        }
    } // onClick

} // LoginActivity
