package com.banjos.dosalarm.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.banjos.dosalarm.types.Alarm;

import java.util.Map;


public class BootReceiver extends BroadcastReceiver {

    private AlarmsPersistService alarmsPersistService;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "onReceive");
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
