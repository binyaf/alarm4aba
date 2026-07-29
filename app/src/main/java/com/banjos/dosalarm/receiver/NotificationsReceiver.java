package com.banjos.dosalarm.receiver;

import static com.banjos.dosalarm.tools.IntentCreator.getNotificationPendingIntent;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.service.AlarmService;
import com.banjos.dosalarm.tools.DateTimesFormats;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.NotificationScheduler;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.NotificationType;
import com.kosherjava.zmanim.ZmanimCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationsReceiver extends BroadcastReceiver {

    private SharedPreferences settingsPreferences;
    private PreferencesService preferencesService;
    private AlarmLocation clientsLocation;

    private static final int NOTIFICATION_ALARM_DURATION_SEC = 20;
    private static final int NOTIFICATION_SNOOZE_DURATION_MIN = 5;

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

        if (notificationType.toString().startsWith("STOP_")) {
            stopNotification(context, notificationType);
        } else if (notificationType.toString().startsWith("SNOOZE_")) {
            snoozeNotification(context, notificationType);
        } else {
            showNotification(context, notificationType);
        }
    }

    private void showNotification(Context context, NotificationType type) {
        if (ZmanimService.isNowAssurBemlacha(clientsLocation)) {
            Log.d("NotificationsReceiver", "type: " + type.toString() + " | Not sending notification - Shabbat/Yom-tov");
            return;
        }

        String title = null;
        String text = null;
        int icon = 0;

        if (NotificationType.CANDLE_LIGHTING_REMINDER == type && preferencesService.isCandleLightReminderSelected()) {
            title = prepareCandleLightingTitle(context);
            text = prepareCandleLightNotificationText(context);
            icon = R.drawable.candles;
        } else if (NotificationType.SHACHARIT_REMINDER == type && preferencesService.isShacharisReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_shacharit_title);
            String sunrise = DateTimesFormats.timeFormat.format(zCal.getSunrise());
            String szksGra = DateTimesFormats.timeFormat.format(zCal.getSofZmanShmaGRA());
            text = context.getString(R.string.prayer_reminder_shacharit_text, sunrise, szksGra);
            icon = R.drawable.sunrise;
        } else if (NotificationType.MINCHA_REMINDER == type && preferencesService.isMinchaReminderSelected()) {
            ZmanimCalendar zCal = ZmanimService.getTodaysZmanimCalendar(clientsLocation);
            title = context.getString(R.string.prayer_reminder_mincha_title);
            String sunset = DateTimesFormats.timeFormat.format(zCal.getSunset());
            text = context.getString(R.string.prayer_reminder_mincha_text, sunset);
            icon = R.drawable.sunset;
        } else if (NotificationType.MAARIV_REMINDER == type && preferencesService.isMaarivReminderSelected()) {
            title = context.getString(R.string.prayer_reminder_maariv_title);
            text = context.getString(R.string.prayer_reminder_maariv_text);
            icon = R.drawable.night;
        }

        if (title == null) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Create Snooze Intent
            NotificationType snoozeType = null;
            if (type == NotificationType.CANDLE_LIGHTING_REMINDER) snoozeType = NotificationType.SNOOZE_CANDLE_LIGHTING_REMINDER;
            else if (type == NotificationType.SHACHARIT_REMINDER) snoozeType = NotificationType.SNOOZE_SHACHARIT_REMINDER;
            else if (type == NotificationType.MINCHA_REMINDER) snoozeType = NotificationType.SNOOZE_MINCHA_REMINDER;
            else if (type == NotificationType.MAARIV_REMINDER) snoozeType = NotificationType.SNOOZE_MAARIV_REMINDER;

            PendingIntent snoozePI = null;
            if (snoozeType != null) {
                snoozePI = IntentCreator.getNotificationPendingIntent(context, snoozeType);
            }

            Intent serviceIntent = new Intent(context, AlarmService.class);
            serviceIntent.putExtra("duration", NOTIFICATION_ALARM_DURATION_SEC);
            serviceIntent.putExtra("notificationId", type.getId());
            serviceIntent.putExtra("title", title);
            serviceIntent.putExtra("text", text);
            serviceIntent.putExtra("icon", icon);
            serviceIntent.putExtra("snoozePI", snoozePI);
            serviceIntent.putExtra("isAlarm", false);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }

    private String prepareCandleLightingTitle(Context context) {
        Date candleLightingTimeToday = ZmanimService.getCandleLightingTimeToday(clientsLocation, context);
        if (candleLightingTimeToday == null) return null;

        long timeDifferenceMillis = candleLightingTimeToday.getTime() - System.currentTimeMillis();
        if (timeDifferenceMillis < 0) return null;

        long minutesDifference = timeDifferenceMillis / (60 * 1000);
        return context.getString(R.string.notification_candle_lighting_title, formatTimeDifference(minutesDifference, context));
    }

    private String prepareCandleLightNotificationText(Context context) {
        List<String> checkList = getCandleLightingChecklist(context);
        if (checkList == null || checkList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(context.getString(R.string.notification_body)).append(":");
        for (String str : checkList) {
            if (str != null && !str.isEmpty()) sb.append("\n* ").append(str);
        }
        return sb.toString();
    }

    private List<String> getCandleLightingChecklist(Context context) {
        List<String> notifications = new ArrayList<>();
        Set<String> values = settingsPreferences.getStringSet("pref_pre_shabbat_notifications_checklist", new HashSet<>());
        for (String notificationKey : values) {
            int resourceId = context.getResources().getIdentifier(notificationKey, "string", context.getPackageName());
            if (resourceId != 0) {
                String notificationStr = context.getString(resourceId);
                if (notificationStr != null) notifications.add(notificationStr);
            }
        }
        return notifications;
    }

    private static String formatTimeDifference(long minutesDifference, Context context) {
        if (minutesDifference < 1) return context.getString(R.string.less_than_a_minute);
        if (minutesDifference == 1) return context.getString(R.string.one_minute);
        if (minutesDifference < 60) return context.getString(R.string.minutes, minutesDifference);
        long hours = minutesDifference / 60;
        long remainingMinutes = minutesDifference % 60;
        if (remainingMinutes == 0) return hours == 1 ? context.getString(R.string.one_hour) : context.getString(R.string.hours, (int)hours);
        return context.getString(R.string.hours, (int)hours) + " " + context.getString(R.string.and_minutes, (int)remainingMinutes);
    }

    private void stopNotification(Context context, NotificationType notificationType) {
        dismissNotification(context, notificationType);
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.stopService(serviceIntent);
    }

    private void dismissNotification(Context context, NotificationType notificationType) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationType.getId());
        }
    }

    private void snoozeNotification(Context context, final NotificationType snoozeNotificationType) {
        NotificationType typeToSnooze = null;
        if (snoozeNotificationType == NotificationType.SNOOZE_MINCHA_REMINDER) typeToSnooze = NotificationType.MINCHA_REMINDER;
        else if (snoozeNotificationType == NotificationType.SNOOZE_MAARIV_REMINDER) typeToSnooze = NotificationType.MAARIV_REMINDER;
        else if (snoozeNotificationType == NotificationType.SNOOZE_SHACHARIT_REMINDER) typeToSnooze = NotificationType.SHACHARIT_REMINDER;
        else if (snoozeNotificationType == NotificationType.SNOOZE_CANDLE_LIGHTING_REMINDER) typeToSnooze = NotificationType.CANDLE_LIGHTING_REMINDER;

        if (typeToSnooze != null) {
            stopNotification(context, typeToSnooze);
            PendingIntent pendingIntent = getNotificationPendingIntent(context, typeToSnooze);
            long snoozeTimeMilli = NOTIFICATION_SNOOZE_DURATION_MIN * 60 * 1000;
            Date notificationTime = new Date(System.currentTimeMillis() + snoozeTimeMilli);
            NotificationScheduler.scheduleNotification(context, pendingIntent, notificationTime, typeToSnooze);
        }
    }
}
