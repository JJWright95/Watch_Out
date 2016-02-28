package uk.ac.cam.lima.pebblecompanion;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ReviewActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    // The {@link ViewPager} that will host the section contents.
    private ViewPager mViewPager;

    private GoogleMap mMap;

    private ConnectivityManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // add map fragment to screen layout and initialise map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // returns a reference to the sectionsPageAdapter so that review activity can access fragments
    // controlled by the section page adapter.
    public SectionsPagerAdapter getSectionsPagerAdapterRef() {
        return mSectionsPagerAdapter;
    }

    // when app regains foreground update set of new hazards from hazard manager.
    @Override
    public void onResume() {
        super.onResume();
        mSectionsPagerAdapter.newHazards = HazardManager.getNewHazardSet();
    }

    // callback method to initialise the google map object
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    // returns reference to the google map object to enable manipulation
    public GoogleMap getMapRef() {
        return mMap;
    }

    /* A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the hazards to be reviewed.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        // generate required fragments and store in arraylist.
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            for (Hazard h : newHazards) {
                HazardReviewFragment newFrag = new HazardReviewFragment();
                newFrag.setRevActivityRef(ReviewActivity.this);
                newFrag.setHazard(h);
                frags.add(newFrag);
            }
            lastFrag = new UploadFragment();
            lastFrag.setRevActivityRef(ReviewActivity.this);
        }

        // load fragment at the specified position.
        @Override
        public Fragment getItem(int position) {
            if (position < frags.size()) {
                return frags.get(position);
            } else {
                return lastFrag;
            }
        }

        @Override
        public int getCount() {
            // Show 1 page for each new hazard and the final upload screen
            return newHazards.size()+1;
        }

        // get position on map to fit all new hazards on screen
        public LatLng getCentrePos() {
            double lat = 0;
            double lon = 0;
            for (int i=0; i<newHazards.size(); i++) {
                lat += frags.get(i).getHazard().getLatitude();
                lon += frags.get(i).getHazard().getLongitude();
            }
            return new LatLng(lat / newHazards.size(), lon / newHazards.size());
        }

        // upload new hazards to database
        public void upload() {
            for (int i = 0; i< newHazards.size(); i++) {
                frags.get(i).getHazard().setDescription(frags.get(i).getDescription());
            }
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                for (Hazard h : newHazards) {
                    try {
                        AsyncTask<Hazard, Void, Void> uploadTask = new AsyncTask<Hazard, Void, Void>() {
                            @Override
                            protected Void doInBackground(Hazard... h) {
                                try {
                                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                                    boolean isConnected = activeNetwork != null &&
                                            activeNetwork.isConnectedOrConnecting();
                                    if (isConnected) {
                                        ServerInterface.uploadHazards(HazardManager.newHazardJSON(h[0]));
                                        Log.i("ReviewActivity", "Send new hazard");
                                    }
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                                return null;
                            }
                        };
                        uploadTask.execute(h);
                        uploadTask.get();
                    } catch (InterruptedException inte) {
                        inte.printStackTrace();
                    } catch (ExecutionException exe) {
                        exe.printStackTrace();
                    } catch (ClassCastException cce) {
                        cce.printStackTrace();
                    }
                }
            }
            // close activity since review of hazards complete
            finish();
        }

        // fragment to upload new hazards to database.
        UploadFragment lastFrag;
        // list of fragments for hazards under review
        ArrayList<HazardReviewFragment> frags = new ArrayList<HazardReviewFragment>();
        Set<Hazard> newHazards = HazardManager.getNewHazardSet();
    }
}
