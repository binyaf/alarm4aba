package com.alarms.myalarm.tools;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.types.AlarmType;

public class IntentCreator {
    public static PendingIntent getAlarmPendingIntent(AppCompatActivity activity, Application application,
                                                      int alarmDurationSec, AlarmType alarmType) {

        Intent intent = new Intent(activity, AlarmReceiver.class);
        intent.putExtra("alarmDurationSec", alarmDurationSec);
        intent.putExtra("alarmType", alarmType);
        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getBroadcast(
                    application,0,//REQUEST_CODE,
                    intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getBroadcast(application,0,//REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT   );
        }
    }
}
