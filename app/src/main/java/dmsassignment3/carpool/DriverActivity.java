package dmsassignment3.carpool;

import android.os.Bundle;
import android.view.*;
import android.view.View.*;
import android.widget.*;

import com.google.android.gms.maps.*;

import org.json.*;

import java.util.*;

public class DriverActivity extends LocationActivity implements OnClickListener {

    ViewGroup startControls;
    ViewGroup liveControls;

    // start controls
    Button beginDriverButton;
    Button cancelDriverButton;

    // live controls
    ListView passengerListView;
    Button endDriverButton;

    List<User> passengerList;
    ArrayAdapter<User> listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startControls = (ViewGroup)findViewById(R.id.startControls);
        liveControls = (ViewGroup)findViewById(R.id.liveControls);

        beginDriverButton = (Button)findViewById(R.id.beginDriverButton);
        cancelDriverButton = (Button)findViewById(R.id.cancelDriverButton);

        endDriverButton = (Button)findViewById(R.id.endDriverButton);


        beginDriverButton.setOnClickListener(this);
        cancelDriverButton.setOnClickListener(this);
        endDriverButton.setOnClickListener(this);


        passengerListView = (ListView)findViewById(R.id.passengerListView);
        passengerList = new ArrayList<User>();
        listAdapter = new ArrayAdapter<User>(this, android.R.layout.simple_list_item_1, passengerList);
        passengerListView.setAdapter(listAdapter);

        updateControls();
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
    } // onResume


    public void updateControls() {
        beginDriverButton.setEnabled(destMarker != null);
        startControls.setVisibility(isOnline() ? View.GONE : View.VISIBLE);
        liveControls.setVisibility(isOnline() ? View.VISIBLE : View.GONE);
    } // updateControls


    @Override
    public void onClick(View view) {
        if (view == beginDriverButton) {
            login("login");
        }
        else if (view == cancelDriverButton) {
            finish();
        }
        else if (view == endDriverButton) {
            logout();
        }
    } // onClick


    @Override
    public int getUserType() {
        return User.DRIVER;
    }


    @Override
    public void loggedIn() {
        super.loggedIn();
        user.setStatus(User.DRIVER);
        updateControls();
    }

    @Override
    public void locationUpdated() {
    }

    @Override
    public void updateUserList(JSONObject jsonUserList) {
        passengerList.clear();
        Iterator<String> keys = jsonUserList.keys();
        while (keys.hasNext())
            try {
                passengerList.add(new User(jsonUserList.getJSONObject(keys.next())));
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
        listAdapter.notifyDataSetChanged();
    }

}
