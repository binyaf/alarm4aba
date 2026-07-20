package com.banjos.dosalarm.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.banjos.dosalarm.tools.DualAlarmScheduler;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;

import java.util.Calendar;
import java.util.Map;


public class BootReceiver extends BroadcastReceiver {

    private PreferencesService preferencesService;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "onReceive: " + (intent.getAction() != null ? intent.getAction() : "null"));
        
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            preferencesService = new PreferencesService(context);
            Map<Integer, Alarm> alarms = preferencesService.getAlarms();

            Log.d("BootReceiver", "Device restarted, rescheduling " + alarms.size() + " alarms");
            
            Calendar now = Calendar.getInstance();
            for (Alarm alarm : alarms.values()) {
                if (alarm != null && alarm.getDateAndTime().after(now)) {
                    Log.d("BootReceiver", "Rescheduling alarm ID: " + alarm.getId());
                    // Use dual scheduler for maximum reliability after boot
                    DualAlarmScheduler.scheduleAlarmDual(context, alarm);
                }
            }
            preferencesService.saveAlarms(alarms);
            Log.d("BootReceiver", "All alarms rescheduled after boot");
        }
    }
}
