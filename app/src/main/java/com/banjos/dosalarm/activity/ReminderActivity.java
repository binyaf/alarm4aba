package com.banjos.dosalarm.activity;

import android.app.Activity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.media.Ringtone;
import com.banjos.dosalarm.R;

public class ReminderActivity extends Activity {
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PrayerReminder", "ReminderActivity - showPopupAlert - start");

        setContentView(R.layout.reminder_activity);

        // Make the activity appear as a popup
        // Make the activity full screen and show when locked
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                  WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );


        Button snoozeButton = findViewById(R.id.snoozeButton);
        Button stopButton = findViewById(R.id.stopButton);

        // Initialize Ringtone to play the default alarm sound
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        ringtone.play();

        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle snooze logic
                Toast.makeText(ReminderActivity.this, "Snoozed", Toast.LENGTH_SHORT).show();
                stopAlarmSound();
                finish(); // Close the activity
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle stop logic
                Toast.makeText(ReminderActivity.this, "Stopped", Toast.LENGTH_SHORT).show();
                stopAlarmSound();
                finish(); // Close the activity
            }
        });
        Log.d("PrayerReminder", "ReminderActivity - showPopupAlert - end ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }
}
