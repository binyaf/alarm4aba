package com.banjos.dosalarm.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
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

    private static MediaPlayer mediaPlayer;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onReceive(Context context, Intent intent) {

        preferencesService = new PreferencesService(context);

        int alarmDurationSec = 10;

        Alarm alarm = null;

        if (intent.getExtras() != null) {
            alarm = (Alarm) intent.getSerializableExtra((IntentKeys.ALARM));
            alarmDurationSec = alarm.getDuration();
        }

        // we will use vibrator first
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(8000);

        Toast.makeText(context, "", Toast.LENGTH_LONG).show();

        playSound(context, alarmDurationSec);

      /*  Uri alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSoundUri == null) {
            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        Ringtone ringtone = RingtoneManager.getRingtone(context, alarmSoundUri);

        // For versions before Lollipop, there's no need to set audio attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            ringtone.setAudioAttributes(audioAttributes);
        }

        ringtone.play();

        new Handler().postDelayed(() -> ringtone.stop(), alarmDurationSec * 1000);
*/
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

    public static void playSound(final Context context, long duration) {

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            // Fall back to notification sound if alarm sound is not available
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        mediaPlayer = MediaPlayer.create(context, alarmSound);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        int flags = 0;
        // we call broadcast using pendingIntent 33 >= 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        //the time since last boot + the duration.
        long stopAlarmAtMillis = SystemClock.elapsedRealtime() + (duration * 1000);

        Intent stopAlarmIntent = new Intent(context, StopSoundReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 333, stopAlarmIntent, flags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, stopAlarmAtMillis , pendingIntent);
    }

    public static class StopSoundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("StopSoundReceiver", "Alarm Receiver - STOP ALARM");
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }
}
