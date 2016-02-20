package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.IOException;

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
        mDataReceiver = new PebbleKit.PebbleDataReceiver(PebbleSender.PEBBLE_APP_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                PebbleKit.sendAckToPebble(context, transactionId);
                switch (PebbleMessage.Type.values()[dict.getInteger(PebbleMessage.Key.TYPE.ordinal()).intValue()]) {
                    case NEW:
                        HazardManager.newHazard(new Hazard(0, 0,
                                dict.getString(PebbleMessage.Key.HAZARD_TYPE.ordinal()),
                                "",
                                parent.getLocation().latitude,
                                parent.getLocation().longitude));
                        Log.i("DataReceiver", "Received New Hazard");
                        break;
                    case ACTION:
                        int id = dict.getInteger(PebbleMessage.Key.HAZARD_ID.ordinal()).intValue();
                        switch (PebbleMessage.ActionType.values()[dict.getInteger(PebbleMessage.Key.ACTION.ordinal()).intValue()]) {
                            case ACK:
                                // User acknowledged the hazard
                                //TODO:: Wrap up updates in AsyncTasks to prevent NetworkOnMainThreadException
                                try {
                                    ServerInterface.uploadHazards(HazardManager.getHazardByID(id).increaseAcks());
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                                runloop.removeActiveHazard(id);
                                Log.i("DataReceiver", "Received Ack");
                                break;
                            case DIS:
                                // User did not see the hazard
                                try {
                                    ServerInterface.uploadHazards(HazardManager.getHazardByID(id).increaseDiss());
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
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
