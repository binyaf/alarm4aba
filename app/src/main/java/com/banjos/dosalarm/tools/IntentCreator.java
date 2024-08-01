package com.banjos.dosalarm.tools;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.banjos.dosalarm.receiver.AlarmReceiver;
import com.banjos.dosalarm.receiver.NotificationsReceiver;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.IntentKeys;
import com.banjos.dosalarm.types.NotificationType;


public class IntentCreator {

    public static PendingIntent getAlarmPendingIntent(Context context, Alarm alarm) {

        int requestCode =  alarm.getId();
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("number",770);
        intent.putExtra(IntentKeys.ALARM, alarm);

        int flags = 0;

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
           flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public static PendingIntent getNotificationPendingIntent(Context context, NotificationType type) {
        // Create an intent for the delete action
        Intent intent = new Intent(context, NotificationsReceiver.class);
        intent.putExtra("NOTIFICATION_TYPE", type.toString());

        int flags = 0;
        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, type.getRequestCode(), intent, flags);
    }

}
