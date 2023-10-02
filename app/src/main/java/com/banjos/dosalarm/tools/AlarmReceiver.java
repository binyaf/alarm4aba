package com.banjos.dosalarm.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.banjos.dosalarm.activity.MainActivity;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmType;
import com.banjos.dosalarm.types.IntentKeys;

import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {
    private AlarmsPersistService alarmsPersistService;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onReceive(Context context, Intent intent) {


        alarmsPersistService = new AlarmsPersistService(context);

        int alarmDurationSec = 10;
        AlarmType alarmType = AlarmType.REGULAR;
        Alarm alarm = null;

        if (intent.getExtras() != null) {
            alarm = (Alarm) intent.getSerializableExtra((IntentKeys.ALARM));
            alarmDurationSec = alarm.getDuration();
            alarmType = alarm.getType();
        }

        // we will use vibrator first
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(8000);

        String toastText =
                alarmType == AlarmType.MINCHA ? "Sun set is in 15 min! don't forget Mincha!!":"Wake up! Wake up! Wake up!";
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);

        ringtone.play();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ringtone.stop();
            }
        }, alarmDurationSec * 1000);

        // go to the main activity
        removeAlarmFromInternalStorage(alarm);
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);

    }

    private void removeAlarmFromInternalStorage(Alarm alarmToRemove) {
        Map<Integer, Alarm> allAlarms = alarmsPersistService.getAlarms();
        allAlarms.remove(alarmToRemove.getId());
        alarmsPersistService.saveAlarms(allAlarms);
    }
}
