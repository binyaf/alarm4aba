package com.banjos.dosalarm.receiver;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.NotificationJobScheduler;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.NotificationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationReceiver extends BroadcastReceiver {

    private SharedPreferences settingsPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNotification(context, NotificationType.CANDLE_LIGHTING);
    }

    private void showNotification(Context context, NotificationType type) {

        boolean isUserWantsNotifications =
                settingsPreferences.getBoolean("enable_pre_shabbat_checklist_notifications", true);

        if (!isUserWantsNotifications) {
            Log.d("NotificationReceiver", "Not sending notification | user doesn't want to receive notifications");
            return;
        }

        String notificationTitle = prepareNotificationTitle(context);

        if (notificationTitle == null) {
            Log.d("NotificationReceiver", "Not sending notification | Today has no candle lighting");
            return;
        }

        String notificationText = prepareNotificationText(context);
        Log.d("NotificationReceiver", "notification body:" + notificationText);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context,  NotificationJobScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.candles)
                .setContentTitle(notificationTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_MESSAGE);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(type.getId(), builder.build());
    }

    private String prepareNotificationTitle(Context context) {
        AlarmLocation clientsLocation = LocationService.getClientLocationDetails(context);

        Date candleLightingTimeToday = ZmanimService.getCandleLightingTimeToday(clientsLocation, context);

        if (candleLightingTimeToday == null) {
            return null;
        }

        // Calculate the difference in minutes
        long timeDifferenceMillis = candleLightingTimeToday.getTime() - System.currentTimeMillis();

        if (timeDifferenceMillis < 0) {
            Log.d("NotificationReceiver", "wanted to send a notification after shabbat candle lighting for some reason");
            return null;
        }
        long minutesDifference = timeDifferenceMillis / (60 * 1000);

        // Present the difference in a human-readable format
        String formattedDifference = formatTimeDifference(minutesDifference, context);

        String title = context.getString(R.string.notification_candle_lighting_title, formattedDifference);
        Log.d("NotificationReceiver", "notification title:" + title);
        return title;
    }

    private String prepareNotificationText(Context context) {

        List<String> checkList = gtChecklist(context);

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


    private List<String> gtChecklist(Context context) {
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

}
