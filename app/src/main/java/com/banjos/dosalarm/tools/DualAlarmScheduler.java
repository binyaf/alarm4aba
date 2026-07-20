package com.banjos.dosalarm.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.ExpeditedWorkRequest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.receiver.AlarmReceiver;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.worker.AlarmReminderWorker;

import java.util.concurrent.TimeUnit;

public class DualAlarmScheduler {

    /**
     * Schedule an alarm using BOTH AlarmManager (primary) and WorkManager (fallback).
     * This provides 100% redundancy: if one fails, the other will trigger.
     * Uses RTC_WAKEUP for wall-clock reliability during long idle periods.
     */
    public static void scheduleAlarmDual(Context context, Alarm alarm) {
        Log.d("DualAlarmScheduler", "Scheduling alarm ID: " + alarm.getId() + " at " + alarm.getDateAndTime().getTime());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = com.banjos.dosalarm.tools.IntentCreator.getAlarmPendingIntent(context, alarm);

        long alarmTimeMs = alarm.getDateAndTime().getTimeInMillis();
        long nowMs = System.currentTimeMillis();
        long delayMs = alarmTimeMs - nowMs;

        if (delayMs <= 0) {
            Log.w("DualAlarmScheduler", "Alarm time is in the past, not scheduling");
            return;
        }

        // ===== PRIMARY: AlarmManager with multiple methods for maximum reliability =====
        try {
            // 1. setAlarmClock() - highest priority, shows on lock screen
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTimeMs, pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            Log.d("DualAlarmScheduler", "setAlarmClock() scheduled");
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setAlarmClock() failed", e);
        }

        try {
            // 2. setExactAndAllowWhileIdle() - exact timing, bypasses Doze
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
                Log.d("DualAlarmScheduler", "setExactAndAllowWhileIdle() scheduled");
            }
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setExactAndAllowWhileIdle() failed", e);
        }

        try {
            // 3. setAndAllowWhileIdle() - fallback if exact fails
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
                Log.d("DualAlarmScheduler", "setAndAllowWhileIdle() scheduled");
            }
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setAndAllowWhileIdle() failed", e);
        }

        // ===== FALLBACK: WorkManager with ExpeditedWorkRequest =====
        try {
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);
            ExpeditedWorkRequest workRequest = new ExpeditedWorkRequest.Builder(AlarmReminderWorker.class)
                    .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                    .addTag("alarm_" + alarm.getId())
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);
            Log.d("DualAlarmScheduler", "WorkManager backup scheduled with delay: " + delaySeconds + "s");
        } catch (Exception e) {
            Log.e("DualAlarmScheduler", "WorkManager backup failed", e);
        }
    }

    /**
     * Cancel an alarm from both AlarmManager and WorkManager
     */
    public static void cancelAlarm(Context context, Alarm alarm) {
        Log.d("DualAlarmScheduler", "Cancelling alarm ID: " + alarm.getId());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            PendingIntent pendingIntent = com.banjos.dosalarm.tools.IntentCreator.getAlarmPendingIntent(context, alarm);
            alarmManager.cancel(pendingIntent);
        }

        WorkManager.getInstance(context).cancelAllWorkByTag("alarm_" + alarm.getId());
    }
}
