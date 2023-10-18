package com.banjos.dosalarm.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.types.AlarmLocation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TimeZone;

public class LocationService {

    public static final String LOCATION_KEY = "location";

    public AlarmLocation getClaientLocationDetails(Context context) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("cities.json");
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();
            String jsonString = new String(bytes);
            final Gson gson = new Gson();

            Type listType = new TypeToken<Map<String, AlarmLocation>>() {}.getType();
            Map<String, AlarmLocation> citiesMap = gson.fromJson(jsonString, listType);
            String defaultCityCode = getDefaultCityCode(context);
            return citiesMap.get(defaultCityCode);
        } catch (IOException e) {
            Log.e("failed reading cities", e.toString());
            return null;
        }
    }

    /*
 get the default location, set one if there is no default location
  */
    private String getDefaultCityCode(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> all = sharedPreferences.getAll();
        Object location = all.get(LOCATION_KEY);
        //want to check that the location is in the correct format
        if (location != null) {
            String locationStr = (String) all.get("location");
            if (locationStr.endsWith("_IL") || locationStr.endsWith("_US") || locationStr.endsWith("_UK")) {
                return locationStr;
            }
        }
        sharedPreferences.edit().putString("location", "JLM_IL").apply();
        return "JLM_IL";
    }

    public GeoLocation getGeoLocationFromAlarmLocation(AlarmLocation alarmLocation) {
        return new GeoLocation(alarmLocation.getCityCode(), alarmLocation.getLatitude(),
                alarmLocation.getLongitude(),
                TimeZone.getTimeZone(alarmLocation.getTimeZone()));
    }

}
