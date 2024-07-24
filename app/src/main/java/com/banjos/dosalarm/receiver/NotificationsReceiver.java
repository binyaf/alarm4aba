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
    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("NOTIFICATION_TYPE");
        preferencesService = new PreferencesService(context);
        clientsLocation = LocationService.getClientLocationDetails(context);
        showNotification(context, type);
    }

    private void showNotification(Context context, String type) {

        NotificationType notificationType = NotificationType.valueOf(type);

        if (notificationType == null) {
            Log.e("NotificationsReceiver", "couldn't send notification, unknown type | type: " + type + " | Not sending notification | ");
            return;
        }

        if (NotificationType.STOP_SHACHARIT_REMINDER == notificationType ||
                NotificationType.STOP_MINCHA_REMINDER == notificationType ||
                NotificationType.STOP_MAARIV_REMINDER == notificationType||
                NotificationType.STOP_CANDLE_LIGHTING_REMINDER == notificationType) {
            stopNotification(context, notificationType);
        } else if (NotificationType.SNOOZE_SHACHARIT_REMINDER == notificationType ||
                NotificationType.SNOOZE_MINCHA_REMINDER == notificationType ||
                NotificationType.SNOOZE_MAARIV_REMINDER == notificationType ||
                NotificationType.SNOOZE_CANDLE_LIGHTING_REMINDER == notificationType) {
            snoozeNotification(context, notificationType);
        } else {
           showNotification(context, notificationType);
        }
    }

    private void showNotification(Context context, NotificationType type) {

        String title = null;
        String text = null;

        if (NotificationType.CANDLE_LIGHTING_REMINDER == type && preferencesService.isCandleLightReminderSelected()) {
            title = prepareCandleLightingTitle(context);
            text = prepareCandleLightNotificationText(context);
        } else if (NotificationType.SHACHARIT_REMINDER == type && preferencesService.isShacharisReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_shacharit_title);
            String sunrise = DateTimesFormats.timeFormat.format( zCal.getSunrise());
            String szksGra = DateTimesFormats.timeFormat.format( zCal.getSofZmanShmaGRA());
            text = context.getString(R.string.prayer_reminder_shacharit_text,sunrise, szksGra);
        } else if (NotificationType.MINCHA_REMINDER == type && preferencesService.isMinchaReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_mincha_title);
            String sunset = DateTimesFormats.timeFormat.format(zCal.getSunset());
            text = context.getString(R.string.prayer_reminder_mincha_text, sunset);
        } else if (NotificationType.MAARIV_REMINDER == type && preferencesService.isMaarivReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_maariv_title);
            text = context.getString(R.string.prayer_reminder_maariv_text);
        }

        if (title == null || text == null) {
            Log.e("NotificationsReceiver", "type: " + type.toString() + " | Not sending notification | title and/or text are empty");
            return;
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
                        "title: " + title + " | text: " + text + " | notification id:" + type.getId());
                notificationManager.notify(type.getId(), builder.build());
            }
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("content://settings/system/alarm_alert"))
                .setSmallIcon(R.drawable.ic_dosalarm_notification)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(R.drawable.cancel_icon, context.getString(R.string.stop), stopPendingIntent)
                .addAction(R.drawable.save_icon, context.getString(R.string.snooze), snoozePendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setAutoCancel(true);

       builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));

        return builder;

    }

    private void playSound(Context context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, Uri.parse("content://settings/system/alarm_alert"));
        }
        mediaPlayer.setLooping(true);  // Loop the sound continuously
        mediaPlayer.start();
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

        Intent intent = new Intent(context, NotificationsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent);
        }
    }
}



