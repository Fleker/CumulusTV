package com.felkertech.n.cumulustv;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONChannel {
    private String number;
    private String name;
    private String url;
    private String logo;
    //TODO Support for genres
    public JSONChannel(JSONObject jsonObject) {
        try {
            number = jsonObject.getString("number");
            name = jsonObject.getString("name");
            url = jsonObject.getString("url");
            if(jsonObject.has("logo"))
                logo = jsonObject.getString("logo");
            else
                logo = "";
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public JSONChannel(String number, String name, String url, String logo) {
        this.number = number;
        this.name = name;
        this.url = url;
        this.logo = logo;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getLogo() {
        return logo;
    }
    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("number", getNumber());
        object.put("name", getName());
        object.put("logo", getLogo());
        object.put("url", getUrl());
        return object;
    }
}
