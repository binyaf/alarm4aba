package com.banjos.dosalarm.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.banjos.dosalarm.types.Alarm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlarmsPersistService {

    private final Gson gson;
    private final Context context;

    private SharedPreferences preferences;

    private static final String ALARMS_KEY = "alarms";
    public AlarmsPersistService(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.preferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    public Map<Integer, Alarm> getAlarms() {

        String alarmsJson = preferences.getString(ALARMS_KEY, "");
        Map<Integer, Alarm> alarmsMap = new HashMap<>();

        if (!alarmsJson.isEmpty()) {
            Type listType = new TypeToken<Map<Integer, Alarm>>() {}.getType();
            alarmsMap = gson.fromJson(alarmsJson, listType);
        }
        Log.d("AlarmsPersistService", "retrieved all alarms (Map) | " + alarmsMap.size() + " alarms");
        return alarmsMap;
    }

    /*
    sorted by date (earlier first)
     */
    public List<Alarm> getAlarmsList() {
        Map<Integer, Alarm> alarms = getAlarms();

        if (alarms.size() > 0) {
            List<Alarm> alarmsList
                    = new ArrayList<>(alarms.values());
            Collections.sort(alarmsList, Comparator.comparing(Alarm::getDateAndTime));
            Log.d("AlarmsPersistService", "retrieved all alarms (List) | " + alarmsList.size() + " alarms");
            return alarmsList;
        }
        Log.d("AlarmsPersistService", "retrieved all alarms (Map) | 0 alarms");
        return new ArrayList<>();
    }

    public void saveAlarms(Map<Integer, Alarm> allAlarms) {
        SharedPreferences.Editor editor = preferences.edit();
        String json = gson.toJson(allAlarms);
        editor.putString(ALARMS_KEY, json);
        editor.apply();
        Log.d("AlarmsPersistService", "saved all alarms | " + allAlarms.size());
    }

    public void removeAlarm(Alarm alarmToRemove) {
        Map<Integer, Alarm> alarms = getAlarms();
        alarms.remove(alarmToRemove.getId());
        saveAlarms(alarms);
        Log.d("AlarmsPersistService", "removed alarm");
    }



}
