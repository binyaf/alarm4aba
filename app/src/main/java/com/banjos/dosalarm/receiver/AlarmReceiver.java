package com.banjos.dosalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.banjos.dosalarm.activity.MainActivity;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmType;
import com.banjos.dosalarm.types.IntentKeys;

import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {
    private PreferencesService preferencesService;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onReceive(Context context, Intent intent) {

        preferencesService = new PreferencesService(context);

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

        Toast.makeText(context, "", Toast.LENGTH_LONG).show();
        Uri alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSoundUri == null) {
            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        Ringtone ringtone = RingtoneManager.getRingtone(context, alarmSoundUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            ringtone.setAudioAttributes(audioAttributes);
        } else {
            // For versions before Lollipop, there's no need to set audio attributes
        }
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
        Map<Integer, Alarm> allAlarms = preferencesService.getAlarms();
        allAlarms.remove(alarmToRemove.getId());
        preferencesService.saveAlarms(allAlarms);
    }
}
