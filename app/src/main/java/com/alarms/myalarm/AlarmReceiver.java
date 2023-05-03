package com.alarms.myalarm;

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

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmDurationSec = 20;

        if (intent.getExtras() != null) {
            alarmDurationSec = intent.getIntExtra("alarmDurationSec", 20);
        }
        // we will use vibrator first
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(8000);

        Toast.makeText(context, "Alarm! Wake up! Wake up!", Toast.LENGTH_LONG).show();
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

    }
}
