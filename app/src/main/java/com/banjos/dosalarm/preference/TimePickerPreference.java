package com.banjos.dosalarm.preference;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import java.util.Locale;

public class TimePickerPreference extends EditTextPreference {

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePickerPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String value = prefs != null ? prefs.getString(getKey(), "08:00") : "08:00";

        // Parse the current time value (format: HH:MM)
        int hour = 8, minute = 0;
        try {
            String[] timeParts = value.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (Exception e) {
            // Use default if parsing fails
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, selectedHour, selectedMinute) -> {
                    // Format the time as HH:MM
                    String timeValue = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);

                    // Save the value
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (sharedPrefs != null) {
                        sharedPrefs.edit().putString(getKey(), timeValue).apply();
                    }

                    // Update the summary
                    setSummary(timeValue);

                    // Notify listeners
                    notifyDependencyChange(shouldDisableDependents());
                    notifyChanged();
                },
                hour,
                minute,
                true // Use 24-hour format
        );

        timePickerDialog.show();
    }

    @Override
    public CharSequence getSummary() {
        // Display the current time value in the summary
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs != null ? prefs.getString(getKey(), "08:00") : "08:00";
    }
}


