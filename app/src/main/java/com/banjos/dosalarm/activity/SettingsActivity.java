package com.banjos.dosalarm.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.banjos.dosalarm.R;

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

            SwitchPreference switchPreferenceEnableNotifications = (SwitchPreference) findPreference("enable_pre_shabbat_checklist_notifications");

            ListPreference timeBeforeShabbatList = (ListPreference) findPreference("pref_notification_time_before_shabbat");
            MultiSelectListPreference multiSelectNotificationDetails =
                    (MultiSelectListPreference) findPreference("pref_pre_shabbat_notifications_checklist");

            timeBeforeShabbatList.setEnabled(switchPreferenceEnableNotifications.isChecked());
            multiSelectNotificationDetails.setEnabled(switchPreferenceEnableNotifications.isChecked());

            // Add a listener to SwitchPreference to update CheckBoxPreference state
            switchPreferenceEnableNotifications.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (boolean) newValue;
                timeBeforeShabbatList.setEnabled(isChecked);
                multiSelectNotificationDetails.setEnabled(isChecked);
                return true;
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}

