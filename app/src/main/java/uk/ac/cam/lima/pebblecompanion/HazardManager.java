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
    private static Set<Hazard> newHazards = new HashSet<Hazard>();
    private static Set<Hazard> hazardSet = new HashSet<Hazard>();

    public static Set<Hazard> getHazardSet(){
        return hazardSet;
    }
    public static Set<Hazard> getNewHazardSet() {
        return newHazards;
    }

    //Location currLoc = new Location(52.213388, 0.102448);

    private static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private HazardManager(){

    }

    public static void populateHazardSet(JSONObject input){
        int numberOfHazards;
        JSONArray results;
        try {
            numberOfHazards = input.getInt("size");
            results = input.getJSONArray("hazards");
            for (int i = 1; i <= numberOfHazards; i++) {
                JSONObject thisHazardJSON = results.getJSONObject(i - 1);
                Hazard newHazard = new Hazard((JSONObject) thisHazardJSON.get("hazard" + i));
                System.out.println(newHazard.getLatitude());
                hazardSet.add(newHazard);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject updateHazard(Hazard update, String ackOrDiss){
        JSONArray upArray = new JSONArray();
        JSONObject innerOb = new JSONObject();
        int id = update.getId();
        String expires = dateParser.format(update.getExpires());
        /*if (ackOrDiss == "ack"){
            expires = dateParser.format(update.getExpires().getTime() + 10000);
        } if (ackOrDiss == "diss"){
            expires = dateParser.format(update.getExpires().getTime() - 10000);
        }*/
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

    public static void newHazard(Hazard newHazard) {
        newHazards.add(newHazard);
    }

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
