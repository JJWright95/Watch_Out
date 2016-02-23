package uk.ac.cam.lima.pebblecompanion;

import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class Hazard implements Comparable<Hazard> {
    private  int id, acks, diss;
    private  String title, description;
    private  double latitude, longitude;
    private  Date reported, expires;
    private  Marker marker;
    private SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static int newid = 0;

    public Hazard(int newacks, int newdiss, String newtitle, String newdescription, double newlat,
                  double newlong){
        acks = newacks;
        diss = newdiss;
        title = newtitle;
        description = newdescription;
        latitude = newlat;
        longitude = newlong;
        reported = new Date();
        expires = new Date(reported.getTime() + 100000);
        id = newid++;
    }

    public Hazard(JSONObject jsonInput) {

        try {
            id = jsonInput.getInt("id");
            acks = jsonInput.getInt("acks");
            diss = jsonInput.getInt("diss");
            title = jsonInput.getString("title");
            description = jsonInput.getString("description");
            latitude = jsonInput.getDouble("latitude");
            longitude = jsonInput.getDouble("longitude");
            reported = dateParser.parse(jsonInput.getString("reported"));
            expires = dateParser.parse(jsonInput.getString("expires"));
        } catch (JSONException je) {

            je.printStackTrace();

        } catch (ParseException pe){

            pe.printStackTrace();

        }

    }

    //LatLng required by maps API to draw pin, use this for drawing pins
    public LatLng getLatLong(){
        return new LatLng(latitude, longitude);
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public String getTitle(){
        return title;
    }

    public String getDescription(){
        return description;
    }

    public int getAcks(){
        return acks;
    }

    public int getId(){
        return id;
    }

    public int getDiss(){
        return diss;
    }

    public JSONObject increaseAcks() {
        return HazardManager.updateHazard(this, "ack");
    }

    public JSONObject increaseDiss(){
        return HazardManager.updateHazard(this, "diss");
    }

    public Date getReported(){
        return reported;
    }

    public Date getExpires(){
        return expires;
    }

    public Marker getMarker() { return marker; }

    public void setTitle(String t){
        title = t;
    }

    public void setDescription(String d){
        description = d;
    }

    public void setMarker(Marker m) { marker = m; }

    public JSONObject toJSON(){
        JSONObject outputJSON = new JSONObject();
        try {
            outputJSON.put("latitude", latitude);
            outputJSON.put("longitude", longitude);
            outputJSON.put("title", title);
            outputJSON.put("id", id);
            outputJSON.put("acks", acks);
            outputJSON.put("diss", diss);
            outputJSON.put("description", description);
            String reportedString = dateParser.format(reported);
            String expiresString = dateParser.format(expires);
            outputJSON.put("reported", reportedString);
            outputJSON.put("expires", expiresString);

        } catch ( JSONException e) {

            e.printStackTrace();

        }

        return outputJSON;
    }

    @Override
    public boolean equals(Object obj) {
        return (id == ((Hazard)obj).id);
    }

    @Override
    public int compareTo(Hazard h) {
        if (id < h.id) return -1;
        else if (id == h.id) return 0;
        return 1;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
