package com.banjos.dosalarm.worker;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.banjos.dosalarm.service.AlarmService;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;

import java.util.Calendar;
import java.util.Map;

public class AlarmReminderWorker extends Worker {

    public AlarmReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("AlarmReminderWorker", "WorkManager callback triggered");
        Context context = getApplicationContext();
        PreferencesService preferencesService = new PreferencesService(context);

        // Find any alarms that should have fired by now
        Map<Integer, Alarm> allAlarms = preferencesService.getAlarms();
        Calendar now = Calendar.getInstance();

        for (Alarm alarm : allAlarms.values()) {
            if (alarm != null && alarm.getDateAndTime().before(now)) {
                Log.d("AlarmReminderWorker", "Alarm " + alarm.getId() + " should fire now via WorkManager fallback");
                
                // Trigger the alarm
                Intent serviceIntent = new Intent(context, AlarmService.class);
                serviceIntent.putExtra("duration", alarm.getDuration());
                ContextCompat.startForegroundService(context, serviceIntent);
                
                // Remove from storage
                allAlarms.remove(alarm.getId());
                preferencesService.saveAlarms(allAlarms);
                
                return Result.success();
            }
        }

        Log.d("AlarmReminderWorker", "No alarms due at this time");
        return Result.success();
    }
}
