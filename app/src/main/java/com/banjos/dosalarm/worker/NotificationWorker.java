package com.banjos.dosalarm.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Calendar;
import java.util.Date;

public class NotificationWorker extends Worker {


    private LocationService locationService;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Code to schedule notifications (e.g., fetch sunset time and schedule notification)
        scheduleNotification();
        return Result.success();
    }

    private void scheduleNotification() {

        Calendar today = Calendar.getInstance();

        AlarmLocation alarmLocation = locationService.getAlarmLocationDetails(getApplicationContext());
        GeoLocation gl = locationService.getGeoLocationFromAlarmLocation(alarmLocation);
        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(today);

        zcal.setGeoLocation(gl);

        Context context = getApplicationContext();
        Log.d("NotificationWorker", "schedule notification");
        // Set the time you want the notification to be shown
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 20);
     //   calendar.set(Calendar.HOUR_OF_DAY, 9);  // Set the hour
     //   calendar.set(Calendar.MINUTE, 0);       // Set the minute

        PendingIntent pendingIntent = IntentCreator.getNotificationPendingIntent(context);

        // Set up AlarmManager to trigger the notification
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
}
