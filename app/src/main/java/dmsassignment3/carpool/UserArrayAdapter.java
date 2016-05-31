package dmsassignment3.carpool;

import android.app.*;
import android.content.*;
import android.widget.*;
import android.view.*;
import android.view.View.*;

import java.util.*;

/**
 * Created by macbookair on 28/05/16.
 *
 * ref:
 * https://www.javacodegeeks.com/2013/09/android-listview-with-adapter-example.html
 * http://www.c-sharpcorner.com/UploadFile/9e8439/create-custom-listener-on-button-in-listitem-listview-in-a/
 */
public class UserArrayAdapter extends ArrayAdapter<User> {

    ActionButtonListener actionButtonListener;

    public interface ActionButtonListener {
        void onActionButtonClick(int position, User user);
    }

    public void setActionButtonListener(ActionButtonListener actionButtonListener) {
        this.actionButtonListener = actionButtonListener;
    }

    private Context context;
    private int resource;
    private List<User> objects = null;

    public UserArrayAdapter(Context context, int resource, List<User> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    } // UserArrayAdapter

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
        }

        final User user = objects.get(position);

        TextView usernameTextView = (TextView) convertView.findViewById(R.id.driverUsernameTextView);
        TextView statusTextView = (TextView) convertView.findViewById(R.id.statusTextView);
        Button actionButton = (Button) convertView.findViewById(R.id.actionButton);

        usernameTextView.setText(user.getUsername());

        // default
        statusTextView.setText("Offline");
        actionButton.setVisibility(View.INVISIBLE);

        // but should be one of these:
        if (user.getStatus() == User.DRIVER) {
            statusTextView.setText("driver");
            actionButton.setVisibility(View.INVISIBLE);
        }
        else if (user.getStatus() == User.PASSENGER) {
            statusTextView.setText("requesting a lift");
            actionButton.setText("Collect");
            actionButton.setVisibility(View.VISIBLE);
        }
        else if (user.getStatus() == User.PASSENGER_PENDING) {
            statusTextView.setText("lift pending");
            actionButton.setText("Cancel");
            actionButton.setVisibility(View.VISIBLE);
        }
        else if (user.getStatus() == User.PASSENGER_COLLECTED) {
            statusTextView.setText("lift in progress");
            actionButton.setText("End");
            actionButton.setVisibility(View.VISIBLE);
        }
        else if (user.getStatus() == User.PASSENGER_COMPLETED) {
            statusTextView.setText("Completed");
            actionButton.setVisibility(View.INVISIBLE);
        }

        if (actionButton.getVisibility() == View.VISIBLE) {
            actionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (actionButtonListener != null)
                        actionButtonListener.onActionButtonClick(position, user);
                }
            });
            convertView.setTag(actionButton);
        }

        return convertView;
    } // getView

} // UserArrayAdapter
