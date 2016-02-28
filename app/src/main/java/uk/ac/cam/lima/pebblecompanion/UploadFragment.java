package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;


public class UploadFragment extends Fragment {

    private static final int REVIEW_ZOOM = 11;

    // reference to ReviewActivity which manages fragments.
    private  ReviewActivity revActivity;

    public void setRevActivityRef(ReviewActivity rev) {
        revActivity = rev;
    }

    // reference to google map in ReviewActivity to allow manipulation - zoom in on hazard under review.
    public GoogleMap getMapRef() {
        return revActivity.getMapRef();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);
        ((FloatingActionButton) rootView.findViewById(R.id.fab_gps)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                revActivity.getSectionsPagerAdapterRef().upload();
            }
        });
        return rootView;
    }

    // callback method to detect which fragment is currently visible to the user
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
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

    // position and zoom map to show all new hazards.
    private void focus() {
        if (revActivity == null) Log.i("UploadFragment", "revActivity not setup");
        if (getMapRef() == null) Log.i("UploadFragment", "map ref is null");
        if (revActivity.getSectionsPagerAdapterRef() == null) {
            Log.i("UploadFragment", "sectionsPagerAdapterRef is null");
        }
        if (revActivity.getSectionsPagerAdapterRef().getCentrePos() == null) {
            Log.i("UploadFragment", "sectionsPagerAdapterRef.centrePos is null");
        }
        getMapRef().animateCamera(CameraUpdateFactory.newLatLngZoom(
                revActivity.getSectionsPagerAdapterRef().getCentrePos(), REVIEW_ZOOM));
    }
}
