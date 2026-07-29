package com.banjos.dosalarm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.receiver.AlarmReceiver;

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "ActiveAlarmChannel";
    private static final int NOTIFICATION_ID = 12345;
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AlarmService", "AlarmService started");

        int durationSec = 20;
        String title = "DosAlarm";
        String text = "Active";
        boolean isAlarm = true;
        int icon = R.drawable.ic_dosalarm_notification;
        int notificationId = NOTIFICATION_ID;
        PendingIntent snoozePI = null;

        if (intent != null) {
            durationSec = intent.getIntExtra("duration", 20);
            notificationId = intent.getIntExtra("notificationId", NOTIFICATION_ID);
            if (intent.hasExtra("title")) title = intent.getStringExtra("title");
            if (intent.hasExtra("text")) text = intent.getStringExtra("text");
            isAlarm = intent.getBooleanExtra("isAlarm", true);
            icon = intent.getIntExtra("icon", R.drawable.ic_dosalarm_notification);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                snoozePI = intent.getParcelableExtra("snoozePI", PendingIntent.class);
            } else {
                snoozePI = intent.getParcelableExtra("snoozePI");
            }
        }
        
        final int finalId = notificationId;
        final String finalTitle = title;
        final String finalText = text;
        final int finalIcon = icon;

        // Keep CPU fully awake
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DosAlarm:AlarmService"
        );
        wakeLock.acquire((durationSec + 5) * 1000L);

        // Show the active (ringing) notification
        Notification notification = createRingingNotification(title, text, icon, snoozePI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(finalId, notification,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                            ? android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            : 0);
        } else {
            startForeground(finalId, notification);
        }

        handleAlert(isAlarm);

        // Schedule stop of sound/vibration but keep notification
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            Log.d("AlarmService", "Ringing phase ended. Stopping sound/vibration and keeping notification.");
            stopRinging();
            updateToStaticNotification(finalId, finalTitle, finalText, finalIcon);
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            stopSelf();
        }, durationSec * 1000L);

        return START_NOT_STICKY;
    }

    private void stopRinging() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                Log.e("AlarmService", "Error stopping player", e);
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void updateToStaticNotification(int id, String title, String text, int icon) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_layout_collapsed);
        collapsedView.setTextViewText(R.id.notification_collapsed_title, title);
        collapsedView.setImageViewResource(R.id.notification_icon, icon);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCustomContentView(collapsedView)
                .setOngoing(false) // Makes it swipeable
                .setAutoCancel(true) // Removes when tapped
                .build();

        manager.notify(id, notification);
    }

    private void handleAlert(boolean isAlarm) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();

        // 1. Play Sound
        if (isAlarm || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            playSound(isAlarm);
        }

        // 2. Vibrate
        if (isAlarm || ringerMode != AudioManager.RINGER_MODE_SILENT) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
                } else {
                    vibrator.vibrate(new long[]{0, 500, 500}, 0);
                }
            }
        }
    }

    private void playSound(boolean isAlarm) {
        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(isAlarm ? 
                    RingtoneManager.TYPE_ALARM : RingtoneManager.TYPE_NOTIFICATION);
            
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmSound);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(isAlarm ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("AlarmService", "Error playing sound", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Active Alarm",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setDescription("Alarm is ringing");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createRingingNotification(String title, String text, int icon, PendingIntent snoozePI) {
        Intent stopIntent = new Intent(this, AlarmReceiver.StopSoundReceiver.class);
        PendingIntent stopPI = PendingIntent.getBroadcast(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_layout_expanded);
        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_layout_collapsed);

        expandedView.setTextViewText(R.id.notification_title, title);
        expandedView.setTextViewText(R.id.notification_text, text);
        expandedView.setImageViewResource(R.id.notification_icon, icon);
        expandedView.setOnClickPendingIntent(R.id.notification_stop, stopPI);
        if (snoozePI != null) {
            expandedView.setViewVisibility(R.id.notification_snooze, View.VISIBLE);
            expandedView.setOnClickPendingIntent(R.id.notification_snooze, snoozePI);
        } else {
            expandedView.setViewVisibility(R.id.notification_snooze, View.GONE);
        }

        collapsedView.setTextViewText(R.id.notification_collapsed_title, title);
        collapsedView.setImageViewResource(R.id.notification_icon, icon);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setDeleteIntent(stopPI) // Stops sound if the user manages to swipe it away
                .setOngoing(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        Log.d("AlarmService", "AlarmService destroyed - stopping sound/vibration");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
