package com.banjos.dosalarm.receiver;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.NotificationJobScheduler;

import java.util.Arrays;
import java.util.List;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    private void showNotification(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!isUserWAntsNotifications(sharedPreferences)) {
            Log.d("NotificationReceiver", "user doesn't want to receive notifications");
            return;
        }
        Log.d("NotificationReceiver", "about to send user a notification");

        List<String> checkList = gtChecklist(sharedPreferences, context);

        StringBuilder sb = new StringBuilder();
        for (String str:checkList) {
            if (str != null && !str.equals("")) {
                sb.append("\n* " + str);
            }
        }
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,  NotificationJobScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.candles)
                .setLargeIcon(bitmap)
                .setContentTitle(context.getString(R.string.notification_candle_lighting_title, "15"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_body)+ ": " + sb.toString()));

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
        notificationManager.notify(1, builder.build());
    }

    private boolean isUserWAntsNotifications(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("enable_pre_shabbat_checklist_notifications", true);
    }
    
    private List<String> gtChecklist(SharedPreferences sharedPreferences, Context context) {
        String dosAlarm = sharedPreferences.getBoolean("notification_checklist_dosalarm", true)? context.getString(R.string.notification_checklist_dishwasher) :"";
        String refrigerator = sharedPreferences.getBoolean("notification_checklist_refrigerator", true)? context.getString(R.string.notification_checklist_refrigerator) :"";
        String dishwasher = sharedPreferences.getBoolean("notification_checklist_dishwasher", true)? context.getString(R.string.notification_checklist_dishwasher) :"";
        String clock = sharedPreferences.getBoolean("notification_checklist_electricity", true)? context.getString(R.string.notification_checklist_electricity) :"";
        String airConditioner = sharedPreferences.getBoolean("notification_checklist_air_conditioner", true)? context.getString(R.string.notification_checklist_air_conditioner) :"";
        String shower = sharedPreferences.getBoolean("notification_checklist_shower", true)? context.getString(R.string.notification_checklist_shower) :"";
        String candles = sharedPreferences.getBoolean("notification_checklist_candles", true)?context.getString(R.string.notification_checklist_candles) :"";
        String phone = sharedPreferences.getBoolean("notification_checklist_phone", true)? context.getString(R.string.notification_checklist_phone) :"";

        return Arrays.asList(dosAlarm, refrigerator, dishwasher, clock, airConditioner, shower, candles, phone);

    }
}
