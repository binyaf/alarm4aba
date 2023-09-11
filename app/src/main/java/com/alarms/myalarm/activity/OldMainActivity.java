package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;
import com.alarms.myalarm.tools.IntentCreator;
import com.alarms.myalarm.tools.LocationService;
import com.alarms.myalarm.types.Alarm;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;

public class OldMainActivity extends AppCompatActivity {

    private Calendar alarmDateAndTime;

    private AlarmManager alarmManager;

    private int alarmDurationSec;

    private static final int minchaAlarmDuration = 10;

    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("OldMainActivity", "onCreate");
        locationService = new LocationService();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Button createAlarmBtn = findViewById(R.id.createAlarmBtn);
        View createMinchaAlarmBtn = findViewById(R.id.createMinchaAlarmBtn);

        createAlarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetAlarmActivity.class);
            alarmDateAndTime = createCalendarWithDefaultValues();
            intent.putExtra(IntentKeys.ALARM, new Alarm(AlarmType.REGULAR, 20, alarmDateAndTime));
            startActivity(intent);
        });

        createMinchaAlarmBtn.setOnClickListener(v -> {
            Log.d("MainActivity", "createMinchaAlarmBtn");
            alarmDateAndTime = createCalendarBeforeSunset();

            PendingIntent pendingIntent = getPendingIntentForMincha(alarmDateAndTime);

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(this.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
            }

            AlarmManager.AlarmClockInfo alarmClockInfo =
                    new AlarmManager.AlarmClockInfo(alarmDateAndTime.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

            Intent i = new Intent(this, MainActivity.class);

            i.putExtra(IntentKeys.ALARM, new Alarm(AlarmType.MINCHA, minchaAlarmDuration, alarmDateAndTime));
            startActivity(i);

        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Alarm alarm = (Alarm) getIntent().getSerializableExtra((IntentKeys.ALARM));
            AlarmType alarmType = alarm.getType();
            if (alarmType == AlarmType.MINCHA) {

                // Create a TextView with a clickable link
                TextView textView = new TextView(this);
                textView.setText("Click to Open Online Mincha Siddur");
                textView.setGravity(Gravity.CENTER);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setOnClickListener(v -> {

                    String url = "http://www.onlinesiddur.com/minc/";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    this.startActivity(intent);
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Mincha!")
                        .setView(textView)
                        .setIcon(R.drawable.warning_icon)
                        .setPositiveButton("OK", (dialog, which) -> {

                            PendingIntent pendingIntent = getPendingIntentForMincha(alarmDateAndTime);

                            alarmManager.cancel(pendingIntent);
                            Log.d("ALARM", "Alarm was canceled");

                            dialog.dismiss();

                        });


                builder.create().show();
            }
        }
    }


    private PendingIntent getPendingIntentForMincha(Calendar alarmDateAndTime) {
        return IntentCreator.getAlarmPendingIntent(this, getApplication(),
                new Alarm(AlarmType.MINCHA, minchaAlarmDuration, alarmDateAndTime));
    }

    private Calendar createCalendarBeforeSunset() {
        Calendar cal = Calendar.getInstance();

        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(cal);
        GeoLocation gl = locationService.getGeoLocation();
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