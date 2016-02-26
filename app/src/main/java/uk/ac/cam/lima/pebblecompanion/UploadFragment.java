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


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link UploadFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link UploadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UploadFragment extends Fragment {

    private  ReviewActivity revActivity;

    public void setRevActivityRef(ReviewActivity rev) {
        revActivity = rev;
    }

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
        // TODO: should use constant here for zoom value
        if (revActivity == null) Log.i("UploadFragment", "revActivity not setup");
        if (getMapRef() == null) Log.i("UploadFragment", "map ref is null");
        if (revActivity.getSectionsPagerAdapterRef() == null) Log.i("UploadFragment", "sectionsPagerAdapterRef is null");
        if (revActivity.getSectionsPagerAdapterRef().getCentrePos() == null) Log.i("UploadFragment", "sectionsPagerAdapterRef.centrePos is null");
        getMapRef().animateCamera(CameraUpdateFactory.newLatLngZoom(
                revActivity.getSectionsPagerAdapterRef().getCentrePos(), 11));
    }
}
