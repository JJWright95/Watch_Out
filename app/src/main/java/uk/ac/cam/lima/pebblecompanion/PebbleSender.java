package uk.ac.cam.lima.pebblecompanion;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code PebbleSender} class handles connections to some connected Pebble (if one is connected).
 * No objects of this class can be created. The static functions are shared over the App as there should
 * only be a single channel of data being sent to the Pebble from an App.
 *
 * @author Will Simmons
 * @since 07/02/2016
 */
public class PebbleSender {
    private static PebbleKit.PebbleNackReceiver mNackReceiver;
    private static PebbleKit.PebbleAckReceiver mAckReceiver;
    private static Map<Integer, PebbleDictionary> messages;
    private static int currentSendMessageId = 0;
    private static int currentQueueMessageId = 0;
    private static AppCompatActivity parent;
    public final static UUID PEBBLE_APP_UUID =
            UUID.fromString("172e12e2-bba3-469c-bef5-4af9b88bfd96");

    private PebbleSender() {}

    /**
     * This method should be called upon booting up the app to initialise the sender with the
     * main Activity (as required for acquiring the context for sending to the Pebble). Automatically
     * sets up a {@code PebbleKit.PebbleAckReceiver} and a {@code PebbleKit.PebbleNackReceiver}, so
     * these should not be registered elsewhere in the App.
     *
     * @param mainAct Main Activity of the App.
     */
    public static void startSender(AppCompatActivity mainAct) {
        if (parent != null) {
            parent = mainAct; return;
        }
        parent = mainAct;

        messages = new HashMap<Integer, PebbleDictionary>();

        mNackReceiver = new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                boolean connected = PebbleKit.isWatchConnected(parent.getApplicationContext());
                if (!connected) {
                    Log.i("DataSender", "Pebble Disconnected. Emptying message queue");
                    stopSending();
                    return;
                }
                Log.i("DataSender", transactionId + " was nacked");
                if (transactionId == currentSendMessageId)
                    PebbleKit.sendDataToPebbleWithTransactionId(parent.getApplicationContext(),
                            PEBBLE_APP_UUID,
                            messages.get(currentSendMessageId),
                            currentSendMessageId);
            }
        };

        mAckReceiver = new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                if (transactionId == currentSendMessageId) {
                    Log.i("DataSender", "Sent " + currentSendMessageId);
                    currentSendMessageId = (currentSendMessageId + 1) % 256;
                    if (currentSendMessageId != currentQueueMessageId) {
                        try {
                            Thread.sleep(1000, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        PebbleKit.sendDataToPebbleWithTransactionId(parent.getApplicationContext(),
                                PEBBLE_APP_UUID,
                                messages.get(currentSendMessageId),
                                currentSendMessageId);
                    }
                }
            }
        };

        PebbleKit.registerReceivedNackHandler(parent.getApplicationContext(), mNackReceiver);
        PebbleKit.registerReceivedAckHandler(parent.getApplicationContext(), mAckReceiver);
    }

    public static void stopSending() {
        currentSendMessageId = currentQueueMessageId;
    }

    /**
     * <h2>send</h2>
     * Send a prepared PebbleDictionary message (either ALERT or IGNORE) to the Pebble.
     * Handles queueing of these and resending to cope with dropped messages.
     *
     * @param data The PebbleDictionary ALERT or IGNORE message to be sent to the Pebble.
     */
    public static void send(PebbleDictionary data) {
        messages.put(currentQueueMessageId, data);
        Log.i("DataSender", "Attempting to send " + currentQueueMessageId);
        if (currentSendMessageId == currentQueueMessageId) {
            PebbleKit.sendDataToPebbleWithTransactionId(parent.getApplicationContext(),
                    PEBBLE_APP_UUID, data,
                    currentSendMessageId);
        }
        currentQueueMessageId = (currentQueueMessageId + 1) % 256;
    }
}