package com.banjos.dosalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.activity.MainActivity;
import com.banjos.dosalarm.service.AlarmService;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.IntentKeys;

import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {
    private PreferencesService preferencesService;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm received!");

        if (intent.getBooleanExtra("is_guardian", false)) {
            Log.d("AlarmReceiver", "Guardian alarm fired. Pre-warming system...");
            // Job is done, process was woken up. 
            // This prevents the OS from considering the app "stale" or "deep-sleeping".
            return;
        }

        preferencesService = new PreferencesService(context);

        // Acquire PARTIAL WakeLock to keep CPU alive but allow screen to stay off
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DosAlarm:AlarmReceived"
        );
        wakeLock.acquire(10000);

        int alarmDurationSec = 10;
        Alarm alarm = null;

        if (intent.getExtras() != null) {
            alarm = (Alarm) intent.getSerializableExtra((IntentKeys.ALARM));
            if (alarm != null) {
                alarmDurationSec = alarm.getDuration();
            }
        }

        // 1. Vibrate
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(8000);
        }

        // 2. Start the Foreground Service to play sound and handle auto-stop
        // This is the "Aggressive" way to ensure it stays alive and stops correctly
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("duration", alarmDurationSec);
        serviceIntent.putExtra("title", context.getString(R.string.app_name));
        serviceIntent.putExtra("text", alarm != null && alarm.getLabel() != null && !alarm.getLabel().isEmpty() ? 
                alarm.getLabel() : "Alarm Ringing");
        serviceIntent.putExtra("isAlarm", true);
        serviceIntent.putExtra("icon", R.drawable.ic_dosalarm_notification);

        ContextCompat.startForegroundService(context, serviceIntent);

        // 3. UI and Persistence
        if (alarm != null) {
            removeAlarmFromInternalStorage(alarm);
        }
        
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }

    private void removeAlarmFromInternalStorage(Alarm alarmToRemove) {
        Map<Integer, Alarm> allAlarms = preferencesService.getAlarms();
        allAlarms.remove(alarmToRemove.getId());
        preferencesService.saveAlarms(allAlarms);
    }

    public static class StopSoundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("StopSoundReceiver", "Stopping alarm via service");
            Intent serviceIntent = new Intent(context, AlarmService.class);
            context.stopService(serviceIntent);
        }
    }
}

