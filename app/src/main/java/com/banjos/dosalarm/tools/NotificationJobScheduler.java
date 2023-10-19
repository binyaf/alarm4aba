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

    public static final String WORKER_TAG = "notifications_job";
    public static void scheduleDailyNotificationsJob(Context context) {

        Log.d("NotificationJobScheduler", "Scheduling NotificationWorker job (is supposed to be called once)");

        String channelId = "dosAlarmDailyNotificationsId";
        CharSequence channelName = "DosAlarmNotificationsChannel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 20, TimeUnit.MINUTES,
                        20, TimeUnit.MINUTES).addTag("notifications_job").build();

        WorkManager.getInstance(context).enqueue(notificationWorkRequest);

    }
}
