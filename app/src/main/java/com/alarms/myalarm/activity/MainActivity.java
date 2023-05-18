package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;
import com.alarms.myalarm.tools.IntentCreator;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private Calendar alarmDateAndTime;

    private AlarmManager alarmManager;

    private int alarmDurationSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Button createAlarmBtn = findViewById(R.id.createAlarmBtn);
        View createMinchaAlarmBtn = findViewById(R.id.createMinchaAlarmBtn);

        createAlarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetAlarmActivity.class);
            alarmDateAndTime = createCalendarWithDefaultValues();
            alarmDurationSec = 20;
            intent.putExtra(IntentKeys.ALARM_CALENDAR, alarmDateAndTime);
            intent.putExtra(IntentKeys.ALARM_DURATION, alarmDurationSec);
            intent.putExtra(IntentKeys.ALARM_TYPE, AlarmType.REGULAR);
            startActivity(intent);
        });

        createMinchaAlarmBtn.setOnClickListener(v -> {
            Log.d("MainActivity", "createMinchaAlarmBtn");
            alarmDateAndTime = createCalendarBeforeSunset();
            alarmDurationSec = 20;

            PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this, getApplication(),
                    alarmDurationSec, AlarmType.MINCHA);

            AlarmManager.AlarmClockInfo alarmClockInfo =
                    new AlarmManager.AlarmClockInfo(alarmDateAndTime.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

            Intent i = new Intent(this, AlarmDetailsActivity.class);

            i.putExtra(IntentKeys.ALARM_CALENDAR, alarmDateAndTime);
            i.putExtra(IntentKeys.ALARM_DURATION, alarmDurationSec);
            i.putExtra(IntentKeys.ALARM_TYPE, AlarmType.MINCHA);
            startActivity(i);

        });
    }

    private Calendar createCalendarBeforeSunset() {
        Calendar cal = Calendar.getInstance();

        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(cal);
        GeoLocation gl = new GeoLocation("Jerusalem",   31.7683, 35.2137, 800, TimeZone.getTimeZone("Asia/Jerusalem"));
        zcal.setGeoLocation(gl);

        cal.setTime(zcal.getSunset());
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE)  -15);

        return cal;
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
