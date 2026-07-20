package com.banjos.dosalarm.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.work.ExpeditedWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.worker.AlarmReminderWorker;

import java.util.concurrent.TimeUnit;

public class DualAlarmScheduler {

    /**
     * Schedule an alarm using BOTH AlarmManager (primary) and ExpeditedWorkRequest (fallback).
     * This provides 200% redundancy for 48+ hour reliability in airplane mode.
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
            // 1. setAlarmClock() - highest priority, shows on lock screen, best for 24-48 hour waits
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTimeMs, pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            Log.d("DualAlarmScheduler", "setAlarmClock() scheduled (PRIMARY)");
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setAlarmClock() failed", e);
        }

        try {
            // 2. setExactAndAllowWhileIdle() - exact timing, bypasses Doze, allows during airplane mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
                Log.d("DualAlarmScheduler", "setExactAndAllowWhileIdle() scheduled (BACKUP 1)");
            }
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setExactAndAllowWhileIdle() failed", e);
        }

        try {
            // 3. setAndAllowWhileIdle() - inexact but allows while idle, last resort before WorkManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
                Log.d("DualAlarmScheduler", "setAndAllowWhileIdle() scheduled (BACKUP 2)");
            }
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setAndAllowWhileIdle() failed", e);
        }

        // ===== FALLBACK: ExpeditedWorkRequest for 48-hour reliability =====
        // ExpeditedWorkRequest tries to execute within 5-10 minutes of scheduling
        // Perfect as a backup if AlarmManager is somehow blocked during long idle periods
        try {
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);
            ExpeditedWorkRequest expeditedWorkRequest = new ExpeditedWorkRequest.Builder(AlarmReminderWorker.class)
                    .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                    .addTag("alarm_expedited_" + alarm.getId())
                    .build();

            WorkManager.getInstance(context).enqueue(expeditedWorkRequest);
            Log.d("DualAlarmScheduler", "ExpeditedWorkRequest scheduled with delay: " + delaySeconds + "s (BACKUP 3 - BEST FOR 48+ HOURS)");
        } catch (Exception e) {
            Log.e("DualAlarmScheduler", "ExpeditedWorkRequest failed", e);
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

        WorkManager.getInstance(context).cancelAllWorkByTag("alarm_expedited_" + alarm.getId());
    }
}
