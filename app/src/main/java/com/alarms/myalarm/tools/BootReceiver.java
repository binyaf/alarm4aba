package com.alarms.myalarm.tools;

import static android.content.Context.ALARM_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alarms.myalarm.types.Alarm;

import java.util.Map;


public class BootReceiver extends BroadcastReceiver {

    private AlarmsPersistService alarmsPersistService;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            alarmsPersistService = new AlarmsPersistService(context);
            Map<Integer, Alarm> alarms = alarmsPersistService.getAlarms();

            for (int alarmId:alarms.keySet()) {
                Alarm alarm = alarms.get(alarmId);
                alarm.setLabel("after reboot");
            }
            alarmsPersistService.saveAlarms(alarms);

            Log.d("BootReceiver", "device restarted, all alarms are back in place");
        }
    }

}
