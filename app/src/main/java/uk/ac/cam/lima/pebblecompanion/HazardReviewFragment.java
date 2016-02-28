package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class HazardReviewFragment extends Fragment {

    private static final int FOCUS_ZOOM = 13;

    View rootView;

    private static ReviewActivity revActivity;
    private Hazard newHazard;

    public void setHazard(Hazard newHaz) {
        newHazard = newHaz;
    }

    public Hazard getHazard() {return newHazard;}

    public void setRevActivityRef(ReviewActivity rev) {
        revActivity = rev;
    }

    public GoogleMap getMapRef() {
        return revActivity.getMapRef();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_review, container, false);
        // set hazard title
        TextView textViewToChange = (TextView) rootView.findViewById(R.id.title);
        while (newHazard == null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                }
            }, 1000);
        }
        textViewToChange.setText(newHazard.getTitle());
        return rootView;
    }

    // callback method to detect whether fragment is visible to user.
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (getMapRef() == null) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (getMapRef() != null) {
                            focus();
                        }
                    }
                }, 500);
                if (getMapRef() != null) {
                    focus();
                }
            } else {
                focus();
            }
        }
    }

    // position and zoom map to show current hazard under review
    private void focus() {
        getMapRef().animateCamera(CameraUpdateFactory.newLatLngZoom(
                newHazard.getLatLong(), FOCUS_ZOOM));
        switch (newHazard.getTitle()) {
            case "Road Works" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_warning_orange_24dp)));
                break;
            case "Pothole" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_move_to_inbox_orange_24dp)));
                break;
            case "Road Closure" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_car_orange_24dp)));
                break;
            case "Flooding" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pool_orange_24dp)));
                break;
            case "Traffic Accident" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_car_orange_24dp)));
                break;
            case "Broken Glass" :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_bar_orange_24dp)));
                break;
            default :
                getMapRef().addMarker(new MarkerOptions()
                        .position(newHazard.getLatLong())
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_error_orange_24dp)));
                break;
        }

    }

    // return hazard description entered by user
    public String getDescription() {
        EditText descriptionField = (EditText) rootView.findViewById(R.id.hazard_description);
        return descriptionField.getText().toString();
    }
}
