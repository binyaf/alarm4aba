package com.banjos.dosalarm.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.banjos.dosalarm.types.NotificationType;

import java.util.Calendar;
import java.util.Date;

public class NotificationScheduler {

    public static void scheduleNotification(Context context, PendingIntent pendingIntent,
                                            Date notificationTime, NotificationType notificationType) {
        Date now = Calendar.getInstance().getTime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        if (notificationTime.after(now)) {
            Log.d("NotificationWorker", "type: " + notificationType + " | Scheduling notification | notification time: " +
                    notificationTime + " | now: " + now);
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
        } else {
            Log.d("NotificationWorker", "type: " + notificationType + " | NOT Scheduling notification | notification time " +
                    notificationTime + " | now: " + now + " | notification is in the past - not Scheduling");
        }
    }
}
