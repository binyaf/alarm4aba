package com.alarms.myalarm.tools;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.types.Alarm;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;


public class IntentCreator {

    public static PendingIntent getAlarmPendingIntent(AppCompatActivity activity, Application application,
                                                     Alarm alarm) {

        int requestCode =  alarm.getId();
        Intent intent = new Intent(activity, AlarmReceiver.class);
        intent.putExtra("number",770);
        intent.putExtra(IntentKeys.ALARM, alarm);

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getBroadcast(
                    application, requestCode,
                    intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getBroadcast(application, requestCode,
                    intent, PendingIntent.FLAG_IMMUTABLE);
        }
    }
}
