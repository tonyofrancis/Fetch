package com.tonyodev.fetch2.database;

import android.arch.persistence.room.TypeConverter;
import android.support.v4.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by tonyofrancis on 6/14/17.
 */

public class TypeConverters {

    @TypeConverter
    public Map<String,String> headerStringToList(String headers) {

        Map<String,String> headerMap = new ArrayMap<>();

        try {

            JSONObject jsonObject = new JSONObject(headers);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                headerMap.put(key,jsonObject.getString(key));
            }

        }catch (JSONException e) {
            e.printStackTrace();
        }

        return headerMap;
    }

    @TypeConverter
    public String headerListToString(Map<String,String> headerMap) {

        String headerString;

        try {

            JSONObject headerObject = new JSONObject();

            for (String key : headerMap.keySet()) {
                headerObject.put(key,headerMap.get(key));
            }

            headerString = headerObject.toString();
        }catch (JSONException e) {
            headerString = "{}";
        }

        return headerString;
    }

}
