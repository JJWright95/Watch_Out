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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class HazardReviewFragment extends Fragment {

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
        // TODO: should use constant here for zoom value
        getMapRef().animateCamera(CameraUpdateFactory.newLatLngZoom(
                newHazard.getLatLong(), 13));
        getMapRef().addMarker(new MarkerOptions()
                .position(newHazard.getLatLong())
                .flat(true));
    }

    public String getDescription() {
        EditText descriptionField = (EditText) rootView.findViewById(R.id.hazard_description);
        return descriptionField.getText().toString();
    }
}
