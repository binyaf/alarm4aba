package com.banjos.dosalarm.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

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

    //here are the preferences of the 'settings' page
    private SharedPreferences preferences;

    //here are the user preferences from arround the app
    private SharedPreferences sharedPreferencesSettings;
    private static final String ALARMS_KEY = "alarms";
    private static final String MY_PREFERENCES_KEY = "MyPrefs";

    private static final String REMINDERS_SHACHARIT_SELECTED = "remindersShacharitEnabled";
    private static final String REMINDERS_MINCHA_SELECTED = "remindersMinchaEnabled";
    private static final String REMINDERS_MAARIV_SELECTED = "remindersMaarivEnabled";
    private static final String REMINDERS_CANDLE_LIGHT_SELECTED = "remindersCandleLightEnabled";
    private static final String REMINDERS_SHACHARIT_MINUTES_BEFORE_SUNRISE = "pref_shacharit_notification_time_before_sunrise";
    private static final String REMINDERS_MINCHA_MINUTES_BEFORE_SUNSET = "pref_mincha_notification_time_before_sunset";
    private static final String REMINDERS_MAARIV_MINUTES_AFTER_SUNSET = "pref_maariv_notification_time_after_sunset";
    private static final String REMINDERS_CANDLE_MINUTES_BEFORE_SHABBAT = "pref_notification_time_before_shabbat";

    public PreferencesService(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.preferences = context.getSharedPreferences(MY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.sharedPreferencesSettings = PreferenceManager.getDefaultSharedPreferences(context);
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
    public boolean isCandleLightReminderSelected() {
        return preferences.getBoolean(REMINDERS_CANDLE_LIGHT_SELECTED, false);
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
    public void candleLightReminderSwitched(boolean newValue) {
        preferences.edit().putBoolean(REMINDERS_CANDLE_LIGHT_SELECTED, newValue).apply();
    }

    public int getShacharitMinutesBeforeSunriseForReminder() {
        String val =  sharedPreferencesSettings.getString(REMINDERS_SHACHARIT_MINUTES_BEFORE_SUNRISE, "40");
        return Integer.valueOf(val);
    }

    public int getMinchaMinutesBeforeSunsetForReminder() {
        String val = sharedPreferencesSettings.getString(REMINDERS_MINCHA_MINUTES_BEFORE_SUNSET, "20");
        return Integer.valueOf(val);
    }

    public int getMaarivMinutesAfterSunsetForReminder() {
        String val = sharedPreferencesSettings.getString(REMINDERS_MAARIV_MINUTES_AFTER_SUNSET, "20");
        return Integer.valueOf(val);
    }

    public int getCandleLightingMinutesBeforeShabbatForReminder() {
        String val =  sharedPreferencesSettings.getString(REMINDERS_CANDLE_MINUTES_BEFORE_SHABBAT, "40");
        return Integer.valueOf(val);
    }

    public void setShacharitMinutesBeforeSunriseForReminder(int min) {
        sharedPreferencesSettings.edit().putInt(REMINDERS_SHACHARIT_MINUTES_BEFORE_SUNRISE, min);
    }

    public void setMinchaMinutesBeforeSunsetForReminder(int min) {
        sharedPreferencesSettings.edit().putInt(REMINDERS_MINCHA_MINUTES_BEFORE_SUNSET, min);
    }

    public void setMaarivMinutesAfterSunsetForReminder(int min) {
        sharedPreferencesSettings.edit().putInt(REMINDERS_MAARIV_MINUTES_AFTER_SUNSET, min);
    }

    public void setCandleLightingMinutesBeforeShabbatForReminder(int min) {
        sharedPreferencesSettings.edit().putInt(REMINDERS_CANDLE_MINUTES_BEFORE_SHABBAT, min);
    }


}



