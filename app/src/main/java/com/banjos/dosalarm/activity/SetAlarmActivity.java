package com.banjos.dosalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.AlarmsPersistService;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmType;
import com.banjos.dosalarm.types.IntentKeys;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SetAlarmActivity extends AppCompatActivity {

    private AlarmManager alarmManager;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private TextView dateText;
    private TextView timeText;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy", Locale.US);
    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private NumberPicker numberPicker;
    private EditText alarmLabelEditText;
    private AlarmsPersistService alarmsPersistService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_alarm_screen);
        alarmsPersistService = new AlarmsPersistService(getApplicationContext());
        TextView title = findViewById(R.id.addEditAlarmTitle);
        final Alarm alarm;

        if (getIntent() != null && getIntent().getSerializableExtra(IntentKeys.ALARM) != null) {
            alarm = (Alarm) getIntent().getSerializableExtra(IntentKeys.ALARM);
            title.setText(R.string.edit_alarm);
        }  else {
            Calendar alarmDateAndTime = createCalendarWithDefaultValues();
            int defaultDuration = getDefaultDuration();
            alarm = new Alarm(AlarmType.REGULAR, defaultDuration,alarmDateAndTime);
            title.setText(R.string.add_alarm);
        }

        Calendar alarmDateAndTime = alarm.getDateAndTime();

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        dateText = findViewById(R.id.selectDateText);
        dateText.setText(dateFormat.format(alarmDateAndTime.getTime()));
        timeText = findViewById(R.id.selectTimeText);
        timeText.setText(timeFormat.format(alarmDateAndTime.getTime()));
        numberPicker = findViewById(R.id.numberPicker);
        alarmLabelEditText = findViewById(R.id.alarmLabelTextView);
        alarmLabelEditText.setText(alarm.getLabel());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            numberPicker.setTextColor(Color.BLACK);
        }

        Map<Integer, Integer> alarmDurationMap = greateAlarmDurationMap();

        String[] alarmDisplayedValues = new String[]{"5", "10", "15","20"
                ,"25","30","40","50","60","70","80","90","100","110","120"};
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(alarmDisplayedValues.length - 1);
        numberPicker.setDisplayedValues(alarmDisplayedValues);
        numberPicker.setValue(alarmDurationMap.get(alarm.getDuration()));

        timeText.setOnClickListener(v -> {
            timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        alarmDateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        alarmDateAndTime.set(Calendar.MINUTE, minute);
                        String timeTxt = timeFormat.format(alarmDateAndTime.getTime());
                        timeText.setText(timeTxt);
                        Log.d("TAG", "Selected time: " + timeTxt);
                    }, alarmDateAndTime.get(Calendar.HOUR_OF_DAY),
                    alarmDateAndTime.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        dateText.setOnClickListener(v -> {
            datePickerDialog = new DatePickerDialog(this,
                    (view, year, monthOfYear, dayOfMonth) -> {
                        alarmDateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        alarmDateAndTime.set(Calendar.YEAR, year);
                        alarmDateAndTime.set(Calendar.MONTH, monthOfYear);
                        String dateTxt = dateFormat.format(alarmDateAndTime.getTime());
                        Log.d("TAG", "Selected date:" + dateTxt);
                        dateText.setText(dateTxt);

                    }, alarmDateAndTime.get(Calendar.YEAR),
                    alarmDateAndTime.get(Calendar.MONTH), alarmDateAndTime.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
            datePickerDialog.show();
        });

        Button saveAlarm = findViewById(R.id.addAlarmOk);
        saveAlarm.setOnClickListener(v -> {

            if (alarmDateAndTime.after(Calendar.getInstance())) {
                int value = numberPicker.getValue();
                alarm.setDuration(Integer.valueOf(alarmDisplayedValues[value]));

                String labelStr = alarmLabelEditText.getText().toString();
                if (labelStr != null) {
                    if (labelStr.length() < 25) {
                        alarm.setLabel(labelStr);
                    } else {
                        labelStr.substring(0, 24);
                        alarm.setLabel(labelStr);
                    }
                }

                saveAlarm(alarm);

               Intent i = new Intent(this, MainActivity.class);
               startActivity(i);

            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.warning))
                        .setMessage(Html.fromHtml("alarm is in the past"))
                        .setIcon(R.drawable.warning_icon)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).create();
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    private Map<Integer, Integer> greateAlarmDurationMap() {
        Map<Integer, Integer> alarmDurationMap = new TreeMap<Integer, Integer>();
        alarmDurationMap.put(5, 0);
        alarmDurationMap.put(10,1);
        alarmDurationMap.put(15, 2);
        alarmDurationMap.put(20, 3);
        alarmDurationMap.put(25, 4);;
        alarmDurationMap.put(30, 5);
        alarmDurationMap.put(40, 6);
        alarmDurationMap.put(50, 7);
        alarmDurationMap.put(60, 8);
        alarmDurationMap.put(70, 9);
        alarmDurationMap.put(80, 10);
        alarmDurationMap.put(90, 11);
        alarmDurationMap.put(100, 12);
        alarmDurationMap.put(110, 13);
        alarmDurationMap.put(120, 14);

        return alarmDurationMap;
    }

    private int getDefaultDuration() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> all = sharedPreferences.getAll();
        return all.get("alarm_duration") != null ? Integer.valueOf((String)all.get("alarm_duration")) :20;

    }

    private void saveAlarm(Alarm alarm) {

        if (alarm.getId() == 0) {
            alarm.setId(UUID.randomUUID().hashCode());
        }

        createActualAlarm(alarm);

        // save alarm on local file
        saveAlarmToSharePreferences(alarm);

    }

    private void createActualAlarm(Alarm alarm) {
        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(getApplicationContext(), alarm);

        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(alarm.getDateAndTime().getTimeInMillis(), pendingIntent);
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

    }

    private void saveAlarmToSharePreferences(Alarm alarm) {

        Map<Integer, Alarm> allAlarms = alarmsPersistService.getAlarms();
        allAlarms.put(alarm.getId(), alarm);
        alarmsPersistService.saveAlarms(allAlarms);

    }

    private Calendar createCalendarWithDefaultValues() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 10);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        return cal;
    }

}