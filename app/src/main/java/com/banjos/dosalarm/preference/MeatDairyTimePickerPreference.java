package com.banjos.dosalarm.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;

import java.util.Locale;

public class MeatDairyTimePickerPreference extends EditTextPreference {

    private static final String[] MINUTE_VALUES = {
            "00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"
    };

    public MeatDairyTimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MeatDairyTimePickerPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String value = prefs != null ? prefs.getString(getKey(), "06:00") : "06:00";

        int hour = 6;
        int minute = 0;
        try {
            String[] timeParts = value.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (Exception ignored) {
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_duration_picker, null);
        dialogView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        final NumberPicker pickerHours = dialogView.findViewById(R.id.picker_hours);
        final NumberPicker pickerMinutes = dialogView.findViewById(R.id.picker_minutes);

        pickerHours.setMinValue(1);
        pickerHours.setMaxValue(6);
        if (hour < 1) hour = 1;
        if (hour > 6) hour = 6;
        pickerHours.setValue(hour);

        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(MINUTE_VALUES.length - 1);
        pickerMinutes.setDisplayedValues(MINUTE_VALUES);

        int minuteIndex = Math.round(minute / 5.0f);
        if (minuteIndex >= MINUTE_VALUES.length) minuteIndex = MINUTE_VALUES.length - 1;
        if (minuteIndex < 0) minuteIndex = 0;

        if (hour == 6) {
            pickerMinutes.setValue(0);
            pickerMinutes.setEnabled(false);
        } else {
            pickerMinutes.setValue(minuteIndex);
            pickerMinutes.setEnabled(true);
        }

        pickerHours.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal == 6) {
                pickerMinutes.setValue(0);
                pickerMinutes.setEnabled(false);
            } else {
                pickerMinutes.setEnabled(true);
            }
        });

        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    int selectedHour = pickerHours.getValue();
                    int selectedMinuteIndex = pickerMinutes.getValue();
                    int selectedMinute = (selectedHour == 6) ? 0 : Integer.parseInt(MINUTE_VALUES[selectedMinuteIndex]);

                    String timeValue = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (sharedPrefs != null) {
                        sharedPrefs.edit().putString(getKey(), timeValue).apply();
                    }

                    setSummary(formatSummary(getContext(), timeValue));

                    notifyDependencyChange(shouldDisableDependents());
                    notifyChanged();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public CharSequence getSummary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String value = prefs != null ? prefs.getString(getKey(), "06:00") : "06:00";
        return formatSummary(getContext(), value);
    }

    public static String formatSummary(Context context, String value) {
        int hour = 6;
        int minute = 0;
        try {
            String[] timeParts = value.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (Exception ignored) {
        }

        // Clamp values
        int totalMinutes = hour * 60 + minute;
        if (totalMinutes < 60) {
            hour = 1;
            minute = 0;
        } else if (totalMinutes > 360) {
            hour = 6;
            minute = 0;
        }

        String hoursText = (hour == 1)
                ? context.getString(R.string.one_hour)
                : String.format(context.getString(R.string.hours), hour);

        if (minute == 0) {
            return hoursText;
        }

        String minutesText = (minute == 1)
                ? context.getString(R.string.and_one_minute)
                : String.format(context.getString(R.string.and_minutes), minute);

        return hoursText + " " + minutesText;
    }
}
