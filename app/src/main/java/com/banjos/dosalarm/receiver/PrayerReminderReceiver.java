package com.banjos.dosalarm.receiver;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.NotificationJobScheduler;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.NotificationType;

public class PrayerReminderReceiver extends BroadcastReceiver {

    private SharedPreferences settingsPreferences;

    private PreferencesService preferencesService;

    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("NOTIFICATION_TYPE");
        preferencesService = new PreferencesService(context);
        showNotification(context, type);
    }

    private void showNotification(Context context, String type) {

        NotificationType notificationType = NotificationType.valueOf(type);
        if (NotificationType.STOP_SHACHARIT_REMINDER == notificationType) {
            stopNotification(context, notificationType);
        } else if (NotificationType.SNOOZE_SHACHARIT_REMINDER == notificationType) {
            snoozeNotification(context, notificationType);
        } else {
           showPrayerNotification(context, notificationType);
        }
    }

    private void showPrayerNotification(Context context, NotificationType type) {

        String title = "";
        String text = "";

        if (NotificationType.SHACHARIT_REMINDER == type && preferencesService.isShacharisReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_shacharit_title);
            text = context.getString(R.string.prayer_reminder_shacharit_text);
        } else if (NotificationType.MINCHA_REMINDER == type && preferencesService.isMinchaReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_mincha_title);
            text = context.getString(R.string.prayer_reminder_mincha_text);
        } else if (NotificationType.MAARIV_REMINDER == type && preferencesService.isMaarivReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_maariv_title);
            text = context.getString(R.string.prayer_reminder_maariv_text);
        }

        NotificationCompat.Builder builder = createNotificationBuilder(context, title, text);
        if (builder != null) {
            playSound(context);

            // Show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("PrayerReminderReceiver", "NO permission | notification-type:" + type);
            } else {
                Log.d("PrayerReminderReceiver", "showing notification  | notification type:" + type  +
                        " | notification id:" + type.getId());
                notificationManager.notify(type.getId(), builder.build());
            }
        }
    }

    private NotificationCompat.Builder createNotificationBuilder(Context context, String title, String text) {

        // Create intents for the actions
        PendingIntent stopPendingIntent =
                IntentCreator.getNotificationPendingIntent(context, NotificationType.STOP_SHACHARIT_REMINDER);
        PendingIntent snoozePendingIntent =
                IntentCreator.getNotificationPendingIntent(context, NotificationType.SNOOZE_SHACHARIT_REMINDER);
        PendingIntent deletePendingIntent = IntentCreator.getNotificationPendingIntent(context, NotificationType.STOP_SHACHARIT_REMINDER);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationJobScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("content://settings/system/alarm_alert"))
                .addAction(R.drawable.cancel_icon, context.getString(R.string.stop), stopPendingIntent)
                .addAction(R.drawable.save_icon, context.getString(R.string.snooze), snoozePendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setAutoCancel(true);

        return builder;

    }

    private void playSound(Context context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, Uri.parse("content://settings/system/alarm_alert"));
            mediaPlayer.setLooping(true);  // Loop the sound continuously
            mediaPlayer.start();
        }
    }

    private void stopNotification(Context context, NotificationType notificationType) {
        Log.d("PrayerReminderReceiver", "stopping notification  | notification-type:" + notificationType.getType() +
                " | notification id:" + notificationType.getId());
        dismissNotification(context, notificationType);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    // Method to dismiss a notification
    public void dismissNotification(Context  context, NotificationType notificationType) {

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Dismiss the notification
        if (notificationManager != null) {
            notificationManager.cancel(notificationType.getId());
        }
    }


    //TODO need to test
    private void snoozeNotification(Context context, NotificationType notificationType) {
        Log.d("PrayerReminderReceiver", "snoozing notification  | notification-type:" + notificationType.getType() +
                " | notification id:" + notificationType.getId());
        // Set the snooze time (e.g., 10 minutes)
        long snoozeTimeMillis = System.currentTimeMillis() + 10 * 60 * 1000;

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent);
        }
    }
}



