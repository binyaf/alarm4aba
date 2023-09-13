package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;
import com.alarms.myalarm.tools.AlarmsPersistService;
import com.alarms.myalarm.tools.IntentCreator;
import com.alarms.myalarm.types.Alarm;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SetAlarmActivity extends AppCompatActivity {

    private AlarmManager alarmManager;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;

    private TextView dateText;
    private TextView timeText;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy", Locale.US);
    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US);
    private NumberPicker numberPicker;

    private AlarmsPersistService alarmsPersistService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_alarm_screen);
        alarmsPersistService = new AlarmsPersistService(getApplicationContext());
        TextView title = findViewById(R.id.addEditAlarmTitle);
        final Alarm alarm;
      //  Bundle extras = getIntent().getExtras();

        if (getIntent() != null && getIntent().getSerializableExtra(IntentKeys.ALARM) != null) {
            alarm = (Alarm) getIntent().getSerializableExtra(IntentKeys.ALARM);
            title.setText(R.string.edit_alarm);
        }  else {
            Calendar alarmDateAndTime = createCalendarWithDefaultValues();
            alarm = new Alarm(AlarmType.REGULAR, 20,alarmDateAndTime);
            title.setText(R.string.add_alarm);
        }

        Calendar alarmDateAndTime = alarm.getDateAndTime();
        int alarmDuration = alarm.getDuration();

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        dateText = findViewById(R.id.selectDateText);
        dateText.setText(dateFormat.format(alarmDateAndTime.getTime()));
        timeText = findViewById(R.id.selectTimeText);
        timeText.setText(timeFormat.format(alarmDateAndTime.getTime()));
        numberPicker = findViewById(R.id.numberPicker);
        numberPicker.setTextColor(Color.BLACK);

        String[] alarmDurationValues = new String[]{"5", "10", "15","20"
                ,"25","30","40","50","60","70","80","90","100","110","120"};
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(alarmDurationValues.length - 1);
        numberPicker.setDisplayedValues(alarmDurationValues);
        numberPicker.setValue((alarmDuration/10)-1);

        timeText.setOnClickListener(v -> {
            timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        alarmDateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        alarmDateAndTime.set(Calendar.MINUTE, minute);
                        String timeTxt = timeFormat.format(alarmDateAndTime.getTime());
                        timeText.setText(timeTxt);
                        Log.d("TAG", "Selected time: " + timeTxt);
                    }, alarmDateAndTime.get(Calendar.HOUR_OF_DAY),
                    alarmDateAndTime.get(Calendar.MINUTE), false);
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
                alarm.setDuration(Integer.parseInt(alarmDurationValues[value]));

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

    private void saveAlarm(Alarm alarm) {

        if (alarm.getId() == 0) {
            alarm.setId(UUID.randomUUID().hashCode());
        }

        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this, getApplication(), alarm);
        Log.d("ALARM", "now: " + dateTimeFormat.format(Calendar.getInstance().getTime()) + " | " +
                "alarm: " + dateTimeFormat.format(alarm.getDateAndTime().getTime())  + " | " +
                "type: " + alarm.getType().toString());

        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(alarm.getDateAndTime().getTimeInMillis(), pendingIntent);
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

        // save alarm on local file
        saveAlarmToSharePreferences(alarm);

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