package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.IOException;
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


    private PebbleReceiver() {}

    /**
     * This method should be called upon booting up the app to initialise the receiver with the
     * main Activity (as required for acquiring the context for registering the receiver). Automatically
     * sets up a {@code PebbleKit.PebbleDataReceiver}, so none should be registered elsewhere in the App.
     *
     * @param mainAct Main Activity of the App.
     */
    public static void startReceiver(final MainActivity mainAct, RunLoop rl) {
        parent = mainAct;
        runloop = rl;
        cm = (ConnectivityManager) parent.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mDataReceiver = new PebbleKit.PebbleDataReceiver(PebbleSender.PEBBLE_APP_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                PebbleKit.sendAckToPebble(context, transactionId);
                switch (PebbleMessage.Type.values()[dict.getInteger(PebbleMessage.Key.TYPE.ordinal()).intValue()]) {
                    case NEW:
                        Hazard newh = new Hazard(0, 0,
                                dict.getString(PebbleMessage.Key.HAZARD_TYPE.ordinal()),
                                "",
                                parent.getLocation().latitude,
                                parent.getLocation().longitude);
                        HazardManager.newHazard(newh);
                        newh.setMarker(parent.mMap.addMarker(new MarkerOptions().position(newh.getLatLong()).title(newh.getTitle())));
                        Log.i("DataReceiver", "Received New Hazard");
                        break;
                    case ACTION:
                        int id = dict.getInteger(PebbleMessage.Key.HAZARD_ID.ordinal()).intValue();
                        switch (PebbleMessage.ActionType.values()[dict.getInteger(PebbleMessage.Key.ACTION.ordinal()).intValue()]) {
                            case ACK:
                                // User acknowledged the hazard
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
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Ack");
                                break;
                            case DIS:
                                // User did not see the hazard
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
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Dismissal");
                                break;
                            case NACK:
                                // User dismissed the alert
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
        PebbleKit.registerReceivedDataHandler(parent.getApplicationContext(), mDataReceiver);
    }

}
