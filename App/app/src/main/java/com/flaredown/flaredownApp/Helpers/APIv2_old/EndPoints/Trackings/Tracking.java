package com.flaredown.flaredownApp.Helpers.APIv2_old.EndPoints.Trackings;

import android.support.annotation.Nullable;

import com.flaredown.flaredownApp.Helpers.APIv2_old.EndPoints.CheckIns.TrackableType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An element of the JSON returned from the trackings endpoint.
 */
public class Tracking {
    private Integer id;
    private String created_at;
    private String updated_at;
    private Integer user_id;
    private Integer trackable_id;
    private TrackableType trackable_type;
    private String start_at;
    private String end_at;

    public Tracking(){}

    public Tracking(JSONObject jsonObject) throws JSONException{
        this.id = jsonObject.optInt("id");
        this.created_at = jsonObject.optString("created_at");
        this.updated_at = jsonObject.optString("updated_at");
        this.user_id = jsonObject.optInt("user_id");
        this.trackable_id = jsonObject.optInt("trackable_id");
        this.trackable_type = determineType(jsonObject.optString("trackable_type"));
        this.start_at = jsonObject.optString("start_at");
        this.end_at = jsonObject.optString("end_at");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getTrackable_id() {
        return trackable_id;
    }

    public void setTrackable_id(int trackable_id) {
        this.trackable_id = trackable_id;
    }

    public TrackableType getTrackable_type() {
        return trackable_type;
    }

    public void setTrackable_type(TrackableType trackable_type) {
        this.trackable_type = trackable_type;
    }

    public String getStart_at() {
        return start_at;
    }

    public void setStart_at(String start_at) {
        this.start_at = start_at;
    }

    public String getEnd_at() {
        return end_at;
    }

    public void setEnd_at(String end_at) {
        this.end_at = end_at;
    }

    /**
     * Returns the JSON object representation of the tracking
     * @return JSON representation for Tracking object
     */
    public JSONObject toJson() throws JSONException {
        JSONObject tracking = new JSONObject();
        JSONObject output = new JSONObject();

        output.put("id", this.id);
        output.put("created_at", this.created_at);
        output.put("updated_at", this.updated_at);
        output.put("user_id", this.user_id);
        output.put("trackable_id", String.valueOf(this.trackable_id));
        output.put("trackable_type", this.trackable_type.getTrackingsFormattedType());
        output.put("start_at", this.start_at);
        output.put("end_at", this.end_at);

        tracking.put("tracking",output);
        return tracking;
    }

    @Nullable
    private TrackableType determineType(String type){
        switch (type.toLowerCase()){
            case "condition":
                return TrackableType.CONDITION;
            case "symptom":
                return TrackableType.SYMPTOM;
            case "treatment":
                return TrackableType.TREATMENT;
            default:
                return null;
        }
    }
}