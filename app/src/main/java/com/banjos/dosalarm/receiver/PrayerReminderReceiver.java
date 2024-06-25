package com.banjos.dosalarm.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.banjos.dosalarm.activity.ReminderActivity;

public class PrayerReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Show a notification or a pop-up to the user
        showPopupAlert(context);
    }

    private void showPopupAlert(Context context) {

        Log.d("PrayerReminder", "PrayerReminderReceiver - showPopupAlert ");

        // Acquire a wake lock to ensure the device is awake when showing the activity
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "myapp:mywakelocktag");
        wl.acquire(3000); // Acquire for 3 seconds


        // Create an Intent to start the AlarmActivity
        Intent alarmIntent = new Intent(context, ReminderActivity.class);

        // Add flags to start the activity from a non-activity context
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Start the AlarmActivity
        context.startActivity(alarmIntent);

    }

    public class StopReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle stop action
            // Cancel the alarm, dismiss notification, and perform any necessary cleanup
            // For example:
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(2);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(pendingIntent);
        }
    }

    public class SnoozeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle snooze action
            // Reschedule the alarm for a later time (e.g., 10 minutes later)
            long snoozeTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes in milliseconds

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent);

            // Cancel the current notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(2);
        }
    }
}


