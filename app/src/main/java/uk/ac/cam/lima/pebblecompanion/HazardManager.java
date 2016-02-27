package uk.ac.cam.lima.pebblecompanion;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.Iterator;


public final class HazardManager {

    //Creates set to store Hazards that have been created by the user before they are uploaded
    private static Set<Hazard> newHazards = new HashSet<Hazard>();

    //Creates set to store Hazards that have been downloaded form the server
    private static Set<Hazard> hazardSet = new HashSet<Hazard>();

    public static Set<Hazard> getHazardSet(){
        return hazardSet;
    }
    public static Set<Hazard> getNewHazardSet() {
        return newHazards;
    }

    public static void resetNewHazardSet() { newHazards = new HashSet<Hazard>(); }

    private static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private HazardManager(){

    }

    //Downloads hazards from the server and adds Hazard objects to the HazardSet
    public static void populateHazardSet(JSONObject input){
        int numberOfHazards;
        JSONArray results;
        try {
            numberOfHazards = input.getInt("size");
            results = input.getJSONArray("hazards");
            for (int i = 1; i <= numberOfHazards; i++) {
                JSONObject thisHazardJSON = results.getJSONObject(i - 1);
                Hazard newHazard = new Hazard((JSONObject) thisHazardJSON.get("hazard" + i));
                hazardSet.add(newHazard);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //Used to send and acknowledgment or dismassal of a Hazard to the server
    public static JSONObject updateHazard(Hazard update, String ackOrDiss){
        JSONArray upArray = new JSONArray();
        JSONObject innerOb = new JSONObject();
        int id = update.getId();
        String expires = dateParser.format(update.getExpires());
        JSONObject returnJson = new JSONObject();
        try {
            //innerOb.put("expires", expires);
            innerOb.put("response", ackOrDiss);
            innerOb.put("id", id);
            upArray.put(innerOb);
            returnJson.put("update", upArray);
        } catch (JSONException je){
            je.printStackTrace();
        }
        return returnJson;
    }

    //Adds a Hazard to the HazardSet
    public static void newHazard(Hazard newHazard) {
        newHazards.add(newHazard);
    }

    //Used to wrap a Hazard with the new tag so it can be uploaded to the server
    public static JSONObject newHazardJSON(Hazard newHazard){
        JSONArray upArray = new JSONArray();
        upArray.put(newHazard.toJSON());
        JSONObject returnJson = new JSONObject();
        try {
            returnJson.put("new", upArray);
        } catch (JSONException je){
            je.printStackTrace();
        }
        return returnJson;
    }

    //Gets a Hazard by it's ID value from the HazardSet
    public static Hazard getHazardByID(int id){
        Hazard output = null;
        for(Hazard h : hazardSet){
            if (h.getId() == id){
                output = h;
            }
        }
        return output;
    }
}
