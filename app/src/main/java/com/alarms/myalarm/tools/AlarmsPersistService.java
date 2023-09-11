package com.alarms.myalarm.tools;

import android.content.Context;
import android.content.SharedPreferences;

import com.alarms.myalarm.types.Alarm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AlarmsPersistService {

    private Gson gson;
    private Context context;
    public AlarmsPersistService(Context context) {
        this.context = context;
        this.gson = new Gson();
    }


    public Map<Integer, Alarm> getAlarms() {

        SharedPreferences preferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String alarmsJson = preferences.getString("alarms", "");
        Map<Integer, Alarm> allAlarms = new HashMap<>();

        if (!alarmsJson.isEmpty()) {
            Gson gson = new Gson();
            Type listType = new TypeToken<Map<Integer, Alarm>>() {}.getType();
            allAlarms = gson.fromJson(alarmsJson, listType);
        }
        return allAlarms;
    }

    public void saveAlarms(Map<Integer, Alarm> allAlarms) {
        SharedPreferences preferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String json = new Gson().toJson(allAlarms);
        editor.putString("alarms", json);
        editor.apply();
    }
}
