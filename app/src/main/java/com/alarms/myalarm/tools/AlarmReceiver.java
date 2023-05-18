package com.alarms.myalarm.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.alarms.myalarm.R;
import com.alarms.myalarm.activity.MainActivity;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmDurationSec = 20;
        AlarmType alarmType = AlarmType.REGULAR;

        if (intent.getExtras() != null) {
            alarmDurationSec = intent.getIntExtra(IntentKeys.ALARM_DURATION, 20);
            alarmType = (AlarmType) intent.getSerializableExtra((IntentKeys.ALARM_TYPE));
        }
        // we will use vibrator first
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(8000);

        String toastText =
                alarmType == AlarmType.MINCHA ? "Sun et is in 15 min! don't forget Mincha!!":"Wake up! Wake up!";
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
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);

    }
}
