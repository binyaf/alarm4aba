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

public class PreferencesService {

    private final Gson gson;
    private final Context context;
    private SharedPreferences preferences;
    private static final String ALARMS_KEY = "alarms";
    private static final String MY_PREFERENCES_KEY = "MyPrefs";

    private static final String REMINDERS_SHACHARIT_SELECTED = "remindersShacharitEnabled";
    private static final String REMINDERS_MINCHA_SELECTED = "remindersMinchaEnabled";
    private static final String REMINDERS_MAARIV_SELECTED = "remindersMaarivEnabled";

    public PreferencesService(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.preferences = context.getSharedPreferences(MY_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public SharedPreferences getMyPreferences() {

        if (context != null) {
            return context.getSharedPreferences(MY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        } else {
            return null;
        }
    }

    public boolean isTestMode() {
        return getMyPreferences().getBoolean("testMode", false);
    }

    public Map<Integer, Alarm> getAlarms() {

        String alarmsJson = preferences.getString(ALARMS_KEY, "");
        Map<Integer, Alarm> alarmsMap = new HashMap<>();

        if (!alarmsJson.isEmpty()) {
            Type listType = new TypeToken<Map<Integer, Alarm>>() {
            }.getType();
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

    public boolean isMinchaReminderSelected() {
        return preferences.getBoolean(REMINDERS_MINCHA_SELECTED, false);
    }

    public boolean isMaarivReminderSelected() {
        return preferences.getBoolean(REMINDERS_MAARIV_SELECTED, false);
    }

    public boolean isShacharisReminderSelected() {
        return preferences.getBoolean(REMINDERS_SHACHARIT_SELECTED, false);
    }

    public void shacharitReminderSwitched(boolean newValue) {
        preferences.edit().putBoolean(REMINDERS_SHACHARIT_SELECTED, newValue).apply();
    }

    public void minchaReminderSwitched(boolean newValue) {
        preferences.edit().putBoolean(REMINDERS_MINCHA_SELECTED, newValue).apply();
    }

    public void maarivReminderSwitched(boolean newValue) {
        preferences.edit().putBoolean(REMINDERS_MAARIV_SELECTED, newValue).apply();
    }
}

