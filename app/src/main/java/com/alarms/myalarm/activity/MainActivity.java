package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView dateTimeText;
    private DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/YYY HH:mm");
    private Calendar alarmDateAndTime;
    private Button createAlarmBtn;

    private Button editAlarmBtn;
    private Button deleteAlarmBtn;
    private AlarmManager alarmManager;
    private int alarmDurationSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteAlarmBtn = findViewById(R.id.deleteAlarmBtn);
        createAlarmBtn = findViewById(R.id.createAlarmBtn);
        editAlarmBtn = findViewById(R.id.editAlarmBtn);
        dateTimeText = findViewById(R.id.dateTimeText);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            alarmDateAndTime = (Calendar) getIntent().getSerializableExtra("alarmDateAndTime");
            alarmDurationSec = getIntent().getIntExtra("alarmDurationSec", 20);
        }

        if (alarmDateAndTime != null) {
            String html = "<font color=" + Color.GRAY + "  size=3 >Alarm will start at: </font><br>" +
                    "<font size=30 color=" + Color.BLACK + "><b> " + dateTimeFormat.format(alarmDateAndTime.getTime()) +
                    "</b></font>";
            dateTimeText.setText(Html.fromHtml(html));
           // dateTimeText.setText("Alarm will start at: " + dateTimeFormat.format(alarmDateAndTime.getTime()));
            createAlarmBtn.setVisibility(View.INVISIBLE);
            editAlarmBtn.setVisibility(View.VISIBLE);
            deleteAlarmBtn.setVisibility(View.VISIBLE);
            dateTimeText.setVisibility(View.VISIBLE);
        } else {
            alarmDateAndTime = createCalendarWithDefaultValues();
            alarmDurationSec = 20;
        }

        createAlarmBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SetAlarmActivity.class);
            i.putExtra("alarmDateAndTime", alarmDateAndTime);
            i.putExtra("alarmDurationSec", alarmDurationSec);
            startActivity(i);
        });

        editAlarmBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SetAlarmActivity.class);
            i.putExtra("alarmDateAndTime", alarmDateAndTime);
            i.putExtra("alarmDurationSec", alarmDurationSec);
            startActivity(i);
        });

        deleteAlarmBtn.setOnClickListener(v -> {
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            alarmDateAndTime = createCalendarWithDefaultValues();
            createAlarmBtn.setVisibility(View.VISIBLE);
            editAlarmBtn.setVisibility(View.INVISIBLE);
            deleteAlarmBtn.setVisibility(View.INVISIBLE);
            dateTimeText.setVisibility(View.INVISIBLE);
            PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this, getApplication(), alarmDurationSec);

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("ALARM", "Alarm was canceled");
            }
        });
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
