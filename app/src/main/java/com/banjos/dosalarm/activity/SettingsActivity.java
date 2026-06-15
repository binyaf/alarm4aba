package com.banjos.dosalarm.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.worker.NotificationWorker;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.title_activity_settings);
        }
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

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .build();
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            updateLocationSummaries();
            updateMainLocationSummary();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            String[] countryKeys = {"location", "location_usa", "location_canada", "location_uk", "location_france"};
            
            // Only attach listeners to country preferences that exist in the current root
            for (String key : countryKeys) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    pref.setOnPreferenceChangeListener(this);
                }
            }
            
            // Refresh summaries based on the MASTER location
            updateLocationSummaries();
            updateMainLocationSummary();
        }

        private void updateMainLocationSummary() {
            Preference screen = findPreference("location_screen");
            if (screen != null) {
                String activeLocationCode = getPreferenceManager().getSharedPreferences().getString("location", "JLM_IL");
                int resId = getResources().getIdentifier(activeLocationCode, "string", getContext().getPackageName());
                if (resId != 0) {
                    screen.setSummary(getResources().getString(resId));
                } else {
                    screen.setSummary(activeLocationCode);
                }
            }
        }

        private void updateLocationSummaries() {
            String activeLocation = getPreferenceManager().getSharedPreferences().getString("location", "JLM_IL");
            String[] countryKeys = {"location", "location_usa", "location_canada", "location_uk", "location_france"};

            for (String key : countryKeys) {
                ListPreference pref = findPreference(key);
                if (pref == null) continue;

                // Always ensure the master "location" preference reflects the active location
                if ("location".equals(key)) {
                    // The master 'location' preference holds the active location value.
                    // For display, show the selected city when it belongs to the Israel list;
                    // otherwise display "Not selected" (without clearing the master value).
                    CharSequence[] israelValues = pref.getEntryValues();
                    boolean inIsrael = false;
                    if (israelValues != null) {
                        for (CharSequence v : israelValues) {
                            if (v != null && v.toString().equals(activeLocation)) {
                                inIsrael = true;
                                break;
                            }
                        }
                    }
                    // Keep the master value in sync, but adjust the summary for display
                    pref.setValue(activeLocation);
                    if (inIsrael) {
                        pref.setSummary("%s");
                    } else {
                        pref.setSummary("Not selected");
                    }
                    continue;
                }

                // For per-country lists, mark the one that contains the activeLocation as selected
                boolean isActive = false;
                CharSequence[] values = pref.getEntryValues();
                if (values != null) {
                    for (CharSequence val : values) {
                        if (val != null && val.toString().equals(activeLocation)) {
                            isActive = true;
                            break;
                        }
                    }
                }

                if (isActive) {
                    pref.setValue(activeLocation);
                    pref.setSummary("%s");
                } else {
                    // Clear the per-country preference so it doesn't show as selected
                    pref.setValue(null);
                    pref.setSummary("Not selected");
                }
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key != null && key.startsWith("location")) {
                String newLocationValue = (String) newValue;
                
                // Save the new master location and clear only the per-country keys
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                if (prefs != null) {
                    SharedPreferences.Editor editor = prefs.edit();
                    // 1. Set the master location to the newly selected value
                    editor.putString("location", newLocationValue);

                    // 2. Clear other country-specific location keys (but do NOT clear the master "location")
                    String[] countryKeys = {"location_usa", "location_canada", "location_uk", "location_france"};
                    for (String cKey : countryKeys) {
                        if (!cKey.equals(key)) {
                            editor.putString(cKey, null);
                        }
                    }
                    editor.apply();
                }

                // 3. We return true to allow the current preference (e.g., location_usa) 
                // to save its own value normally.
                
                // 4. Update the summaries on the next frame to ensure they show the new state
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateLocationSummaries();
                        updateMainLocationSummary();
                    });
                }
                
                return true;
            }
            return true;
        }
    }
}

