package uk.ac.cam.lima.pebblecompanion;

import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleMessage {
    /**
     * Enumeration of the possible message types for communications with the Pebble.
     * ALERT will provide a new alert for an approaching hazard - requires TYPE, HAZARD_ID, HAZARD_TYPE, HAZARD_DESC and HAZARD_DIST fields.
     * NEW is sent from the Pebble to the phone upon the user reporting a new hazard - requires TYPE and HAZARD_TYPE fields.
     * ACTION is sent from the Pebble when the user responds to an alert - requires TYPE, HAZARD_ID and ACTION fields.
     * IGNORE is sent to the Pebble when an alert is no longer relevant and it should no longer be displayed - requires TYPE and HAZARD_ID fields.
     * UPDATE is sent to the Pebble to update the approximate distance to a hazard for its alert - requires TYPE, HAZARD_ID and HAZARD_DIST fields.
     */
    public enum Type { ALERT, NEW, ACTION, IGNORE, UPDATE }

    /**
     * Enumeration of the possible keys for all messages with the Pebble.
     * TYPE indicates the purpose of the message - value is an ordinal of some PebbleMessageType.
     * HAZARD_ID is the id code for the hazard under concern - value is an integer.
     * HAZARD_TYPE is the title of the hazard - value is a string (max length 15 characters).
     * HAZARD_DESC is the long description of the hazard - value is a string (max length 80 characters).
     * HAZARD_DIST is the approximate distance to the hazard (in metres) - value is an int.
     * ACTION is the action taken by the user on some alert - value is an ordinal of some PebbleActionType.
     */
    public enum Key { TYPE, HAZARD_ID, HAZARD_TYPE, HAZARD_DESC, HAZARD_DIST, ACTION }

    /**
     * Enumeration of the possible active actions taken by the user on some alert.
     * ACK indicates that the user observed and acknowledged the hazard.
     * DIS indicates that the user could not see the hazard and reported its absence.
     * NACK indicates that the user cleared the alert list from their Pebble without acknowledgement or dismissal.
     */
    public enum ActionType { ACK, DIS, NACK }

    /**
     * Builds the appropriate message to start up a new alert on the Pebble.
     * @param h The hazard to alert the user of.
     * @param hazard_dist The approximate distance to the hazard.
     * @return The message ready to be sent by PebbleSender.
     */
    public static PebbleDictionary createAlert(Hazard h, int hazard_dist) {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(Key.TYPE.ordinal(), Type.ALERT.ordinal());
        dict.addInt32(Key.HAZARD_ID.ordinal(), h.getId());
        dict.addString(Key.HAZARD_TYPE.ordinal(), h.getTitle());
        dict.addString(Key.HAZARD_DESC.ordinal(), h.getDescription());
        dict.addInt32(Key.HAZARD_DIST.ordinal(), hazard_dist);
        return dict;
    }

    /**
     * Builds the appropriate message to remove an alert from the Pebble.
     * @param h The hazard whose alert should be removed.
     * @return The message to be sent by PebbleSender.
     */
    public static PebbleDictionary createIgnore(Hazard h) {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(Key.TYPE.ordinal(), Type.IGNORE.ordinal());
        dict.addInt32(Key.HAZARD_ID.ordinal(), h.getId());
        return dict;
    }

    /**
     * Builds the appropriate message to update the approximate distance on the Pebble's alert
     * @param h The hazard whose distance is being updated.
     * @param hazard_dist The new approximate distance.
     * @return The message to be sent by PebbleSender
     */
    public static PebbleDictionary createUpdate(Hazard h, int hazard_dist) {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(Key.TYPE.ordinal(), Type.UPDATE.ordinal());
        dict.addInt32(Key.HAZARD_ID.ordinal(), h.getId());
        dict.addInt32(Key.HAZARD_DIST.ordinal(), hazard_dist);
        return dict;
    }
}
