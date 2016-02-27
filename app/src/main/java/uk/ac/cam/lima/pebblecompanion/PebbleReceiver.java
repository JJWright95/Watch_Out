package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * The {@code PebbleReceiver} class handles connections from some connected Pebble (if one is connected).
 * No objects of this class can be created. The static functions are shared over the App as there should
 * only be a single channel of data being sent from the Pebble to an App.
 *
 * @author Will Simmons
 * @since 04/02/2016
 */
public class PebbleReceiver {
    private static PebbleKit.PebbleDataReceiver mDataReceiver;
    private static MainActivity parent;
    private static RunLoop runloop;
    private static ConnectivityManager cm;
    private static Date lastTransactionTime;


    private PebbleReceiver() {}

    /**
     * This method should be called upon booting up the app to initialise the receiver with the
     * main Activity (as required for acquiring the context for registering the receiver). Automatically
     * sets up a {@code PebbleKit.PebbleDataReceiver}, so none should be registered elsewhere in the App.
     *
     * @param mainAct Main Activity of the App.
     */
    public static void startReceiver(final MainActivity mainAct, RunLoop rl) {
        // Setup static fields
        parent = mainAct;
        runloop = rl;
        cm = (ConnectivityManager) parent.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        lastTransactionTime = new Date();

        // Define the data receiver module
        mDataReceiver = new PebbleKit.PebbleDataReceiver(PebbleSender.PEBBLE_APP_UUID) {

            // Called when the phone receives a message from the Pebble
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                PebbleKit.sendAckToPebble(context, transactionId);

                // Filter out duplicate messages
                synchronized(lastTransactionTime) {
                    Date currentTime = new Date();
                    long timeDiff = currentTime.getTime() - lastTransactionTime.getTime();
                    Log.i("Receiver", "timeDiff: " + timeDiff);
                    if (timeDiff <= 2000) return;
                    lastTransactionTime = currentTime;
                }

                // Determine the type of message
                switch (PebbleMessage.Type.values()[dict.getInteger(PebbleMessage.Key.TYPE.ordinal()).intValue()]) {
                    case NEW:
                        // Create the new hazard object
                        Hazard newh = new Hazard(0, 0,
                                dict.getString(PebbleMessage.Key.HAZARD_TYPE.ordinal()),
                                "",
                                parent.getLocation().latitude,
                                parent.getLocation().longitude);
                        HazardManager.newHazard(newh);

                        // Add the marker onto the map with the correct icon
                        switch (newh.getTitle()) {
                            case "Road Works" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_warning_orange_24dp))));
                                break;
                            case "Pothole" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_move_to_inbox_orange_24dp))));
                                break;
                            case "Road Closure" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_block_orange))));
                                break;
                            case "Flooding" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pool_orange_24dp))));
                                break;
                            case "Traffic Accident" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_car_orange_24dp))));
                                break;
                            case "Broken Glass" :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_bar_orange_24dp))));
                                break;
                            default :
                                newh.setMarker(parent.mMap.addMarker(new MarkerOptions()
                                        .position(newh.getLatLong())
                                        .title(newh.getTitle())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_error_orange_24dp))));
                        }
                        Log.i("DataReceiver", "Received New Hazard");
                        break;

                    case ACTION:
                        // Identify the hazard in question and the action performed
                        int id = dict.getInteger(PebbleMessage.Key.HAZARD_ID.ordinal()).intValue();
                        switch (PebbleMessage.ActionType.values()[dict.getInteger(PebbleMessage.Key.ACTION.ordinal()).intValue()]) {
                            case ACK: // User acknowledged the hazard
                                // Send update to server
                                try {
                                    AsyncTask<JSONObject, Void, Void> uploadTask = new AsyncTask<JSONObject, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(JSONObject... updateObject) {
                                            try {
                                                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                                                boolean isConnected = activeNetwork != null &&
                                                        activeNetwork.isConnectedOrConnecting();
                                                if (isConnected) {
                                                    ServerInterface.uploadHazards(updateObject[0]);
                                                    Log.i("DataReceiver", "Sent Update");
                                                }
                                            } catch (IOException ioe) {
                                                ioe.printStackTrace();
                                            }
                                            return null;
                                        }
                                    };
                                    uploadTask.execute(HazardManager.getHazardByID(id).increaseAcks());
                                    uploadTask.get();
                                } catch (InterruptedException inte) {
                                    inte.printStackTrace();
                                } catch (ExecutionException exe) {
                                    exe.printStackTrace();
                                } catch (ClassCastException cce) {
                                    cce.printStackTrace();
                                }

                                // Move the hazard to inactiveHazards
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Ack");
                                break;
                            case DIS: // User did not see the hazard
                                // Send update to server
                                try {
                                    AsyncTask<JSONObject, Void, Void> uploadTask = new AsyncTask<JSONObject, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(JSONObject... updateObject) {
                                            try {
                                                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                                                boolean isConnected = activeNetwork != null &&
                                                        activeNetwork.isConnectedOrConnecting();
                                                if (isConnected) {
                                                    ServerInterface.uploadHazards(updateObject[0]);
                                                    Log.i("DataReceiver", "Sent Update");
                                                }
                                            } catch (IOException ioe) {
                                                ioe.printStackTrace();
                                            }
                                            return null;
                                        }
                                    };
                                    uploadTask.execute(HazardManager.getHazardByID(id).increaseDiss());
                                    uploadTask.get();
                                } catch (InterruptedException inte) {
                                    inte.printStackTrace();
                                } catch (ExecutionException exe) {
                                    exe.printStackTrace();
                                } catch (ClassCastException cce) {
                                    cce.printStackTrace();
                                }

                                // Move the hazard to inactiveHazards
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Dismissal");
                                break;

                            case NACK: // User dismissed the alert by pressing back
                                // Move the hazard to inactiveHazards
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Nack");
                                break;
                        }
                        break;
                    default:

                        break;
                }
            }
        };

        // Start receiving
        PebbleKit.registerReceivedDataHandler(parent.getApplicationContext(), mDataReceiver);
    }

}
