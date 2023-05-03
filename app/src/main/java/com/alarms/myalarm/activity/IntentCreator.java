package com.alarms.myalarm.activity;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.AlarmReceiver;

public class IntentCreator {
    public static PendingIntent getAlarmPendingIntent(AppCompatActivity activity, Application application, int alarmDurationSec) {

        Intent intent = new Intent(activity, AlarmReceiver.class);
        intent.putExtra("alarmDurationSec", alarmDurationSec);

        // we call broadcast using pendingIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getBroadcast(
                    application,0,//REQUEST_CODE,
                    intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            return PendingIntent.getBroadcast(application,0,//REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT   );
        }
    }
}
