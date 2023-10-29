package com.banjos.dosalarm.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;

import java.util.Map;


public class BootReceiver extends BroadcastReceiver {

    private PreferencesService preferencesService;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "onReceive");
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            preferencesService = new PreferencesService(context);
            Map<Integer, Alarm> alarms = preferencesService.getAlarms();

            for (int alarmId:alarms.keySet()) {
                Alarm alarm = alarms.get(alarmId);
                createActualAlarm(alarm, context);
            }
            preferencesService.saveAlarms(alarms);

            Log.d("BootReceiver", "device restarted, all alarms are back in place");
        }
    }

    private void createActualAlarm(Alarm alarm, Context context) {

        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(context, alarm);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(alarm.getDateAndTime().getTimeInMillis(), pendingIntent);

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

    }

}
