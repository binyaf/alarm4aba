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
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.DateTimesFormats;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.NotificationJobScheduler;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.NotificationType;
import com.kosherjava.zmanim.ZmanimCalendar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationsReceiver extends BroadcastReceiver {

    private SharedPreferences settingsPreferences;

    private PreferencesService preferencesService;

    private static MediaPlayer mediaPlayer;

    private AlarmLocation clientsLocation;

    private static final int NOTIFICATION_ALARM_DURATION_SEC = 20;

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("NOTIFICATION_TYPE");
        preferencesService = new PreferencesService(context);
        clientsLocation = LocationService.getClientLocationDetails(context);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNotification(context, type);
    }

    private void showNotification(Context context, String type) {

        NotificationType notificationType = NotificationType.valueOf(type);

        if (notificationType == null) {
            Log.e("NotificationsReceiver", "couldn't send notification, unknown type | type: " + type + " | Not sending notification | ");
            return;
        }

        if (notificationType.toString().startsWith("STOP_")) {
            stopNotification(context, notificationType);
        } else if (notificationType.toString().startsWith("SNOOZE_")) {
            snoozeNotification(context, notificationType);
        } else {
           showNotification(context, notificationType);
        }
    }

    private void showNotification(Context context, NotificationType type) {

        String title = null;
        String text = null;
        NotificationCompat.Builder builder = null;
        if (NotificationType.CANDLE_LIGHTING_REMINDER == type && preferencesService.isCandleLightReminderSelected()) {
            title = prepareCandleLightingTitle(context);
            text = prepareCandleLightNotificationText(context);
            builder = createNotificationBuilder(context, title, text,
                    NotificationType.STOP_CANDLE_LIGHTING_REMINDER, NotificationType.SNOOZE_CANDLE_LIGHTING_REMINDER);
        } else if (NotificationType.SHACHARIT_REMINDER == type && preferencesService.isShacharisReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_shacharit_title);
            String sunrise = DateTimesFormats.timeFormat.format( zCal.getSunrise());
            String szksGra = DateTimesFormats.timeFormat.format( zCal.getSofZmanShmaGRA());
            text = context.getString(R.string.prayer_reminder_shacharit_text,sunrise, szksGra);
            builder = createNotificationBuilder(context, title, text,
                    NotificationType.STOP_SHACHARIT_REMINDER, NotificationType.SNOOZE_SHACHARIT_REMINDER);
        } else if (NotificationType.MINCHA_REMINDER == type && preferencesService.isMinchaReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_mincha_title);
            String sunset = DateTimesFormats.timeFormat.format(zCal.getSunset());
            text = context.getString(R.string.prayer_reminder_mincha_text, sunset);

            builder = createNotificationBuilder(context, title, text,
                   NotificationType.STOP_MINCHA_REMINDER, NotificationType.SNOOZE_MINCHA_REMINDER);
        } else if (NotificationType.MAARIV_REMINDER == type && preferencesService.isMaarivReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_maariv_title);
            text = context.getString(R.string.prayer_reminder_maariv_text);
            builder = createNotificationBuilder(context, title, text,
                    NotificationType.STOP_MAARIV_REMINDER, NotificationType.SNOOZE_MAARIV_REMINDER);
        }

        if (title == null || builder == null) {
            Log.e("NotificationsReceiver", "type: " + type.toString() + " | Not sending notification | " +
                    "title or builder are null");
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("NotificationsReceiver", "NO permission | notification-type:" + type);
        } else {
            Log.d("NotificationsReceiver", "showing notification  | notification type:" + type  +
                    " | title: " + title + " | text: " + text + " | notification id:" + type.getId());
            playSound(context);
            notificationManager.notify(type.getId(), builder.build());
        }
    }

    private String prepareCandleLightingTitle(Context context) {

        Date candleLightingTimeToday = ZmanimService.getCandleLightingTimeToday(clientsLocation, context);

        if (candleLightingTimeToday == null) {
            return null;
        }

        // Calculate the difference in minutes
        long timeDifferenceMillis = candleLightingTimeToday.getTime() - System.currentTimeMillis();

        if (timeDifferenceMillis < 0) {
            Log.d("NotificationsReceiver", "wanted to send a notification after shabbat candle lighting for some reason");
            return null;
        }
        long minutesDifference = timeDifferenceMillis / (60 * 1000);

        // Present the difference in a human-readable format
        String formattedDifference = formatTimeDifference(minutesDifference, context);

        return context.getString(R.string.notification_candle_lighting_title, formattedDifference);
    }

    private String prepareCandleLightNotificationText(Context context) {

        List<String> checkList = getCandleLightingChecklist(context);

        if (checkList == null || checkList.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(context.getString(R.string.notification_body)).append(":");
        for (String str:checkList) {
            if (str != null && !str.equals("")) {
                sb.append("\n* " + str);
            }
        }

        return sb.toString();
    }

    private List<String> getCandleLightingChecklist(Context context) {
        List notifications = new ArrayList();
        Set<String> values = settingsPreferences.getStringSet("pref_pre_shabbat_notifications_checklist", new HashSet<>());

        for (String notificationKey : values) {
            int resourceId = context.getResources().getIdentifier(notificationKey, "string", context.getPackageName());
            if (resourceId != 0) {
                String notificationStr = context.getString(resourceId);
                notifications.add(notificationStr);
            }
        }
        return notifications;
    }

    private static String formatTimeDifference(long minutesDifference, Context context) {
        if (minutesDifference < 1) {
            return context.getString(R.string.less_than_a_minute);
        } else if (minutesDifference == 1) {
            return context.getString(R.string.one_minute);
        } else if (minutesDifference < 60) {
            return context.getString(R.string.minutes, minutesDifference);
        } else {
            long hours = minutesDifference / 60;
            long remainingMinutes = minutesDifference % 60;

            if (remainingMinutes == 0) {
                return hours == 1 ? context.getString(R.string.one_hour) : context.getString(R.string.hours, hours);
            } else {
                return context.getString(R.string.hours, hours) + " " + context.getString(R.string.and_minutes, remainingMinutes);
            }
        }
    }

   /* private NotificationCompat.Builder createNotificationBuilder(Context context, String title, String text,
                                                                 NotificationType stopReminderType, NotificationType snoozeReminderType) {

        if (title == null) {
            return null;
        }
        // Create intents for the actions
        PendingIntent stopPendingIntent = IntentCreator.getNotificationPendingIntent(context, stopReminderType);
        PendingIntent snoozePendingIntent =
                IntentCreator.getNotificationPendingIntent(context, snoozeReminderType);

        PendingIntent deletePendingIntent = IntentCreator.getNotificationPendingIntent(context, stopReminderType);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationJobScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("content://settings/system/alarm_alert"))
                .setSmallIcon(R.drawable.ic_dosalarm_notification)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(R.drawable.ic_dosalarm_notification, context.getString(R.string.stop), stopPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.snooze), snoozePendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(Color.GRAY)
                .setAutoCancel(true);

       builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));

        return builder;

    }*/

    private NotificationCompat.Builder createNotificationBuilder(Context context, String title, String text,
                                                                    NotificationType stopReminderType,
                                                                    NotificationType snoozeReminderType) {

        if (title == null) {
            return null;
        }

        RemoteViews notificationLayoutExpanded = new RemoteViews(context.getPackageName(), R.layout.notification_layout_expanded);

        notificationLayoutExpanded.setTextViewText(R.id.notification_title, title);
        notificationLayoutExpanded.setTextViewText(R.id.notification_text, text);

        // Create intents for the actions
        PendingIntent stopPendingIntent = IntentCreator.getNotificationPendingIntent(context, stopReminderType);
        PendingIntent snoozePendingIntent =
                IntentCreator.getNotificationPendingIntent(context, snoozeReminderType);

        PendingIntent deletePendingIntent = IntentCreator.getNotificationPendingIntent(context, stopReminderType);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationJobScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dosalarm_notification)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayoutExpanded)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(R.drawable.ic_dosalarm_notification, context.getString(R.string.stop), stopPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.snooze), snoozePendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(Color.GRAY)
                .setContentTitle(title)
                .setAutoCancel(true);

        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));

        // Set the PendingIntents to the buttons in the custom layout
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.notification_stop, stopPendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.notification_snooze, snoozePendingIntent);

        return builder;

    }
    private void playSound(Context context) {

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            // Fall back to notification sound if alarm sound is not available
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        mediaPlayer = MediaPlayer.create(context, alarmSound);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        int flags = 0;
        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        //the time since last boot + the duration.
        long stopAlarmAtMillis = SystemClock.elapsedRealtime() + (NOTIFICATION_ALARM_DURATION_SEC * 1000);

        Intent stopAlarmIntent = new Intent(context, NotificationsReceiver.StopSoundReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 444, stopAlarmIntent, flags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, stopAlarmAtMillis , pendingIntent);

    }

    public static class StopSoundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("StopSoundReceiver", " Notifications Receiver - STOP ALARM");
            stopSound();
        }
    }

    private void stopNotification(Context context, NotificationType notificationType) {
        Log.d("NotificationsReceiver", "stopping notification  | notification-type:" + notificationType.getType() +
                " | notification id:" + notificationType.getId() + " | request-code: " + notificationType.getRequestCode());
       dismissNotification(context, notificationType);
       stopSound();
    }

    private static void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void dismissNotification(Context  context, NotificationType notificationType) {

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Dismiss the notification
        if (notificationManager != null) {
            notificationManager.cancel(notificationType.getId());
        }
    }

    private void snoozeNotification(Context context, NotificationType notificationType) {
        Log.d("NotificationReceiver", "snoozing notification  | notification-type:" + notificationType.getType() +
                " | notification id:" + notificationType.getId());

        stopNotification(context, notificationType);
        // Set the snooze time  5 minutes)
       long snoozeTimeMillis = System.currentTimeMillis() + 5 * 60 * 1000;

        Intent intent = new Intent(context, NotificationsReceiver.class);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, snoozePendingIntent);
        }
    }
}



