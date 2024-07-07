package com.banjos.dosalarm.tools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
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

        Log.d("NotificationJobScheduler", "Scheduling NotificationWorker job " +
                "(is supposed to be called once, and on every upgrade)");

        createNotificationChannel(context);

        //create the Worker which will try and schedule notifications...
        //TODO not sure why this needs to be in the same class with creating the channel
        //there's a minimum interval limit of 15 minutes for PeriodicWorkRequest
        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.MINUTES,
                        1, TimeUnit.MINUTES).addTag(WORKER_TAG).build();

        WorkManager.getInstance(context).enqueue(notificationWorkRequest);

    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Check if the channel already exists
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                // Define the default alarm sound URI
                Uri alarmSoundUri = Uri.parse("content://settings/system/alarm_alert");

                // Set the sound attributes
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("DosAlarm Notifications Channel");
                channel.setSound(alarmSoundUri, audioAttributes);
                manager.createNotificationChannel(channel);
            }
        }
    }

}
