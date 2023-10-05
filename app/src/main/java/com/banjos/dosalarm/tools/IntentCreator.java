package com.banjos.dosalarm.tools;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.IntentKeys;


public class IntentCreator {

    public static PendingIntent getAlarmPendingIntent(Context context, Alarm alarm) {

        int requestCode =  alarm.getId();
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("number",770);
        intent.putExtra(IntentKeys.ALARM, alarm);

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getBroadcast(
                    context, requestCode,
                    intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getBroadcast(context, requestCode,
                    intent, PendingIntent.FLAG_IMMUTABLE);
        }
    }

}
