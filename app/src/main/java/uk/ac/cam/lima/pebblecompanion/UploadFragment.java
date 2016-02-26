package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        focus();
    }

    // position and zoom map to show all new hazards.
    private void focus() {
        // TODO: should use constant here for zoom value
        getMapRef().animateCamera(CameraUpdateFactory.newLatLngZoom(
                revActivity.getSectionsPagerAdapterRef().getCentrePos(), 11));
    }
}
