package com.banjos.dosalarm.tools;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.banjos.dosalarm.receiver.AlarmReceiver;
import com.banjos.dosalarm.receiver.PrayerReminderReceiver;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.IntentKeys;


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
        return PendingIntent.getBroadcast(context, requestCode, intent,flags);
    }

    public static PendingIntent getNotificationPendingIntent(Context context, int requestCode) {

        Intent intent = new Intent(context, PrayerReminderReceiver.class);

        int flags = 0;

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public static PendingIntent getPrayerReminderPendingIntent(Context context, int requestCode) {

        Intent intent = new Intent(context, PrayerReminderReceiver.class);

        int flags = 0;

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public static PendingIntent getPrayerReminderStopIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, PrayerReminderReceiver.StopReceiver.class);

        int flags = 0;

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);

    }

    public static PendingIntent getPrayerReminderSnoozeIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, PrayerReminderReceiver.SnoozeReceiver.class);

        int flags = 0;

        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);

    }

}
