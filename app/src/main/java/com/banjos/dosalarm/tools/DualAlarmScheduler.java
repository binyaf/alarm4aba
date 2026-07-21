package com.banjos.dosalarm.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.worker.AlarmReminderWorker;

import java.util.concurrent.TimeUnit;

public class DualAlarmScheduler {

    /**
     * Schedule an alarm using TRIPLE redundancy for 48+ hour reliability in airplane mode.
     * 1. setAlarmClock (Primary - highest priority system level)
     * 2. setExactAndAllowWhileIdle (Guardian - fires 1 hour before to "wake up" the system)
     * 3. WorkManager (Safety net - ensures app stays in system scheduler)
     */
    public static void scheduleAlarmDual(Context context, Alarm alarm) {
        Log.d("DualAlarmScheduler", "Scheduling alarm ID: " + alarm.getId() + " at " + alarm.getDateAndTime().getTime());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent primaryIntent = com.banjos.dosalarm.tools.IntentCreator.getAlarmPendingIntent(context, alarm);

        long alarmTimeMs = alarm.getDateAndTime().getTimeInMillis();
        long nowMs = System.currentTimeMillis();
        long delayMs = alarmTimeMs - nowMs;

        if (delayMs <= 0) {
            Log.w("DualAlarmScheduler", "Alarm time is in the past, not scheduling");
            return;
        }

        // ===== 1. PRIMARY: AlarmManager.setAlarmClock() =====
        // This is the MOST aggressive method. It tells the system to treat this as
        // a user-visible alarm clock (like the system clock). It bypasses Doze,
        // App Standby, and Airplane mode. It also shows the alarm icon in status bar.
        try {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTimeMs, primaryIntent);
            alarmManager.setAlarmClock(alarmClockInfo, primaryIntent);
            Log.d("DualAlarmScheduler", "setAlarmClock() scheduled (PRIMARY)");
        } catch (SecurityException e) {
            Log.e("DualAlarmScheduler", "setAlarmClock failed", e);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, primaryIntent);
            }
        }

        // ===== 2. GUARDIAN: setExactAndAllowWhileIdle() (1 Hour Before) =====
        // We set a second "Guardian" alarm to fire 1 hour before the main alarm.
        // This "pre-warms" the app and the system, ensuring the app is alive and 
        // hasn't been completely frozen/cached away by vendor optimizations.
        if (delayMs > TimeUnit.HOURS.toMillis(1)) {
            long guardianTimeMs = alarmTimeMs - TimeUnit.HOURS.toMillis(1);
            // We use a different request code for the guardian so it doesn't overwrite the primary
            Intent guardianIntent = new Intent(context, com.banjos.dosalarm.receiver.AlarmReceiver.class);
            guardianIntent.putExtra("is_guardian", true);
            guardianIntent.putExtra(com.banjos.dosalarm.types.IntentKeys.ALARM, alarm);
            PendingIntent guardianPI = PendingIntent.getBroadcast(context, alarm.getId() + 10000, 
                    guardianIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, guardianTimeMs, guardianPI);
                Log.d("DualAlarmScheduler", "Guardian scheduled 1h before (SECONDARY)");
            }
        }

        // ===== 3. SAFETY NET: WorkManager =====
        try {
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);
            OneTimeWorkRequest backupWorkRequest = new OneTimeWorkRequest.Builder(AlarmReminderWorker.class)
                    .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                    .addTag("alarm_backup_" + alarm.getId())
                    .build();

            WorkManager.getInstance(context).enqueue(backupWorkRequest);
        } catch (Exception e) {
            Log.e("DualAlarmScheduler", "WorkManager fallback failed", e);
        }
    }

    /**
     * Cancel an alarm from all redundant systems
     */
    public static void cancelAlarm(Context context, Alarm alarm) {
        Log.d("DualAlarmScheduler", "Cancelling alarm ID: " + alarm.getId());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            PendingIntent primaryIntent = com.banjos.dosalarm.tools.IntentCreator.getAlarmPendingIntent(context, alarm);
            alarmManager.cancel(primaryIntent);
            
            // Cancel Guardian
            Intent guardianIntent = new Intent(context, com.banjos.dosalarm.receiver.AlarmReceiver.class);
            PendingIntent guardianPI = PendingIntent.getBroadcast(context, alarm.getId() + 10000, 
                    guardianIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(guardianPI);
        }

        WorkManager.getInstance(context).cancelAllWorkByTag("alarm_backup_" + alarm.getId());
    }
}
