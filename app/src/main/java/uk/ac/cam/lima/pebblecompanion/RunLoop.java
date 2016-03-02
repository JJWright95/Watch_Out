package uk.ac.cam.lima.pebblecompanion; 
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.maps.model.LatLng;
import java.lang.InterruptedException;
import java.lang.Runnable;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Suraj Patel <suraj-patel-95@outlook.com>
 */

class RunLoop implements Runnable {
    /**
     * Defines whether or not the user has initiated a run.
     * <p>
     * The frequency of GPS requests and cache updates is modified appropriately.
     */
    public enum RunState { ACTIVE, INACTIVE }
    private RunState runState;
    /**
     * Sets the runState of the loop thread.
     *
     * @param rs The state to which the runState is set.
     */
    public void setRunState(RunState rs) { this.runState = rs; }

    private static final int CACHE_TIMEOUT = 240000;
    private static final double CACHE_RADIUS = 1000;
    private static final int LOOP_DELAY_ACTIVE = 5000;
    private static final int LOOP_DELAY_INACTIVE = 5000;
    private static final double WARN_DISTANCE = 15;
    private static final int WARN_DELAY = 60000;

    private LatLng lastCachedLocation;
    private Date lastCachedTime;
    private Set<Hazard> activeHazards; // nearby hazards
    private LinkedHashMap<Hazard,Date> inactiveHazards; // recently warned hazards

    private MainActivity parent;
    private ConnectivityManager cm;

    public RunLoop(MainActivity mainAct) {
        parent = mainAct;
        this.runState = RunState.INACTIVE;
        LatLng currentLocation = null;
        this.lastCachedLocation = currentLocation;
        this.lastCachedTime = new Date();
        this.activeHazards = Collections.synchronizedSet(new LinkedHashSet<Hazard>());
        this.inactiveHazards = new LinkedHashMap<Hazard,Date>();
        cm = (ConnectivityManager) parent.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static double calculateDistance(LatLng n, LatLng m) {
        return 6371000. * Math.acos(Math.sin(n.latitude/57.2958) * Math.sin(m.latitude/57.2958)
                + Math.cos(n.latitude/57.2958) * Math.cos(m.latitude/57.2958)
                * Math.cos((m.longitude - n.longitude)/57.2958));
    }

    /**
     * Allows removal of hazards from the set of active hazards, for the case where the user actively dismisses the hazard.
     */
    public void removeActiveHazard(int hazardID) {
        synchronized(this.activeHazards) {
            for (Iterator<Hazard> it = activeHazards.iterator(); it.hasNext(); ) {
                Hazard h = it.next();
                if (h.getId() == hazardID) {
                    this.inactiveHazards.put(h, new Date());
                    it.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        LatLng currentLocation = parent.getLocation();
        lastCachedLocation = currentLocation;
        try {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                HazardManager.populateHazardSet(ServerInterface.getHazards(currentLocation));
                Message msg = parent.handler.obtainMessage();
                msg.what = 0;
                parent.handler.sendMessage(msg);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        while (true) {
            currentLocation = parent.getLocation();
            Date currentTime = new Date();

            // update the cache if the distance from the last update has equalled or exceeded CACHE_RADIUS or the time from the last update is at least CACHE_TIMEOUT
            if (RunLoop.calculateDistance(this.lastCachedLocation, currentLocation) >= CACHE_RADIUS
                    || currentTime.getTime() - this.lastCachedTime.getTime() >= CACHE_TIMEOUT) {
                try {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    boolean isConnected = activeNetwork != null &&
                            activeNetwork.isConnectedOrConnecting();
                    if (isConnected) {
                        HazardManager.populateHazardSet(ServerInterface.getHazards(currentLocation));
                        Message msg = parent.handler.obtainMessage();
                        msg.what = 0;
                        parent.handler.sendMessage(msg);
                    }
                    lastCachedLocation = currentLocation;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            if (runState == RunState.ACTIVE) {
                // remove hazards from the set of recently warned hazards if at least WARN_DELAY have passed
                for (Iterator<Date> it = this.inactiveHazards.values().iterator(); it.hasNext(); ) {
                    Date nextDate = it.next();
                    if (currentTime.getTime() - nextDate.getTime() >= WARN_DELAY) {
                        it.remove();
                    }
                }

                // (I) update distance for active hazards or (II) move the hazard to inactiveHazards if we are more than WARN_DISTANCE away
                synchronized(this.activeHazards) {
                    for (Iterator<Hazard> it = this.activeHazards.iterator(); it.hasNext(); ) {
                        Hazard h = it.next();
                        double distanceFromH = RunLoop.calculateDistance(h.getLatLong(), currentLocation);
                        /*if (distanceFromH <= WARN_DISTANCE) // (I)
                            PebbleSender.send(PebbleMessage.createUpdate(h, (int) distanceFromH));
                        else { // (II)*/
                        if (distanceFromH > WARN_DISTANCE) {
                            PebbleSender.send(PebbleMessage.createIgnore(h));
                            this.inactiveHazards.put(h, currentTime);
                            it.remove();
                        }
                    }
                }

                // copy hazards to activeHazards if we are at most WARN_DISTANCE away and the hazard is not in inactiveHazards
                for (Hazard h : HazardManager.getHazardSet()) {
                    double distanceFromH = RunLoop.calculateDistance(h.getLatLong(), currentLocation);
                    if (distanceFromH <= WARN_DISTANCE && !this.inactiveHazards.keySet().contains(h) && !this.activeHazards.contains(h)) {
                        PebbleSender.send(PebbleMessage.createAlert(h, (int) distanceFromH));
                        this.activeHazards.add(h);
                    }
                }
            }

            try {
                if (runState == RunState.ACTIVE)
                    Thread.sleep(LOOP_DELAY_ACTIVE);
                else {
                    Thread.sleep(LOOP_DELAY_INACTIVE);
                    // clear cached data so each run is independent of previous runs
                    this.activeHazards = Collections.synchronizedSet(new LinkedHashSet<Hazard>());
                    this.inactiveHazards = new LinkedHashMap<Hazard,Date>();
                }
            } catch (InterruptedException ie) {
                break;
            }
        }
    }
}
