package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SetAlarmActivity extends AppCompatActivity {

    private Button addAlarmBtn;
    private AlarmManager alarmManager;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private Calendar alarmDateAndTime;
    private TextView dateText;
    private TextView timeText;
    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYY");
    private DateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/YYY HH:mm:ss");
    private NumberPicker numberPicker;
    private int alarmDurationSec;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_alarm_screen);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            alarmDateAndTime = (Calendar) getIntent().getSerializableExtra("alarmDateAndTime");
            alarmDurationSec = getIntent().getIntExtra("alarmDurationSec", 20);
        } else {
            alarmDateAndTime = Calendar.getInstance();
            alarmDurationSec = 20;
        }

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        dateText = findViewById(R.id.selectDateText);
        dateText.setText(dateFormat.format(alarmDateAndTime.getTime()));
        timeText = findViewById(R.id.selectTimeText);
        timeText.setText(timeFormat.format(alarmDateAndTime.getTime()));
        numberPicker = findViewById(R.id.numberPicker);

        numberPicker.setTextColor(Color.BLACK);
        String[] alarmDurationValues = new String[]{"10","20","30","40","50","60","70","80","90","100","110","120"};
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(alarmDurationValues.length - 1);
        numberPicker.setDisplayedValues(alarmDurationValues);
        numberPicker.setValue((alarmDurationSec/10)-1);

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

        addAlarmBtn = findViewById(R.id.addAlarmOk);
        addAlarmBtn.setOnClickListener(v -> {
            int value = numberPicker.getValue();
            alarmDurationSec = Integer.parseInt(alarmDurationValues[value]);

            PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this, getApplication(), alarmDurationSec);
            Log.d("TAG", "Alarm was set to " + dateTimeFormat.format(alarmDateAndTime.getTime()));
            Calendar now = Calendar.getInstance();
            Log.d("TAG", "Time now is " + dateTimeFormat.format(now.getTime()));
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDateAndTime.getTimeInMillis(), pendingIntent);
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("alarmDateAndTime",alarmDateAndTime);
            i.putExtra("alarmDurationSec", alarmDurationSec);
            startActivity(i);
        });

    }
}