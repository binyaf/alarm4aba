package com.banjos.dosalarm.tools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.worker.NotificationWorker;

import java.util.concurrent.TimeUnit;

public class NotificationJobScheduler {

    public static final String WORKER_TAG = "dosAlarmDailyNotifications";
    public static final String CHANNEL_ID = "dosAlarmDailyNotificationsChannelId";
    private static final String CHANNEL_NAME = "dosAlarmDailyNotificationsChannelName";
    public static void scheduleDailyNotificationsJob(Context context) {

        Log.d("NotificationJobScheduler", "Scheduling NotificationWorker job (is supposed to be called once, and on every upgrade)");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Check if the channel already exists
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }
        }

        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.HOURS,
                        1, TimeUnit.HOURS).addTag(WORKER_TAG).build();

        WorkManager.getInstance(context).enqueue(notificationWorkRequest);

    }
}
