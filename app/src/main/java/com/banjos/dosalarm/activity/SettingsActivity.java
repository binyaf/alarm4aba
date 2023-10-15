package com.banjos.dosalarm.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.banjos.dosalarm.R;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SettingsActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            //  Get the SwitchPreference object.
            SwitchPreference switchPreference = (SwitchPreference) findPreference("enable_pre_shabbat_checklist_notifications");

            PreferenceCategory checkBoxCategory = findPreference("pref_shabbat_checklist_notifications_category");
            ListPreference notificationTimeListPreference = (ListPreference) findPreference("pref_notification_time_before_shabbat");

            // Set the initial state of CheckBoxPreference based on the SwitchPreference
            updateCheckBoxesState(switchPreference.isChecked(), checkBoxCategory);
            notificationTimeListPreference.setEnabled(switchPreference.isChecked());

            // Add a listener to SwitchPreference to update CheckBoxPreference state
            switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (boolean) newValue;
                updateCheckBoxesState(isChecked, checkBoxCategory);
                notificationTimeListPreference.setEnabled(isChecked);
                return true;
            });
        }

        private void updateCheckBoxesState(boolean checked, PreferenceCategory checkBoxCategory) {
            // Iterate through CheckBoxPreference items and enable/disable them
            for (int i = 0; i < checkBoxCategory.getPreferenceCount(); i++) {
                Preference preference = checkBoxCategory.getPreference(i);
                if (preference instanceof CheckBoxPreference) {
                    CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                    checkBoxPreference.setEnabled(checked);
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        private Map<Integer, Integer> getNotificationTimeMap() {
            Map<Integer, Integer> alarmDurationMap = new TreeMap<Integer, Integer>();
            alarmDurationMap.put(15, 0);
            alarmDurationMap.put(30,1);
            alarmDurationMap.put(45, 2);
            alarmDurationMap.put(60, 3);
            alarmDurationMap.put(75, 4);;
            alarmDurationMap.put(90, 5);
            alarmDurationMap.put(105, 6);
            alarmDurationMap.put(120, 7);
            alarmDurationMap.put(150, 8);
            alarmDurationMap.put(180, 9);
            alarmDurationMap.put(210, 10);
            alarmDurationMap.put(240, 11);
            return alarmDurationMap;
        }

    }
}

