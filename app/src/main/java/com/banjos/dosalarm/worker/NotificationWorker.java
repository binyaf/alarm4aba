package com.banjos.dosalarm.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.kosherjava.zmanim.ZmanimCalendar;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

public class NotificationWorker extends Worker {

    private LocationService locationService;
    private int CANDLE_LIGHTING_REQUEST_CODE = 10;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationWorker", "NotificationWorker was called");
        scheduleNotificationForCandleLighting();
        return Result.success();
    }

    private void scheduleNotificationForCandleLighting() {

        Context context = getApplicationContext();

        AlarmLocation clientsLocation = locationService.getClientLocationDetails(context);

        ZmanimCalendar zcalToday = ZmanimService.getTodaysZmanimCalendar(clientsLocation);

        SharedPreferences sharedPreferences = PreferencesService.getMyPreferences(context);
        boolean isTestMode = isTestMode(sharedPreferences);

        if (scheduleNotificationForCandleLightingToday(zcalToday, clientsLocation) ||
                isTestMode) {

            Date notificationTime = getNotificationTime(zcalToday, sharedPreferences);

            Date now = Calendar.getInstance().getTime();

            PendingIntent pendingIntent =
                    IntentCreator.getNotificationPendingIntent(context, CANDLE_LIGHTING_REQUEST_CODE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

            if (notificationTime.after(now)) {
                Log.d("NotificationWorker", "Scheduling notification | time: " +
                        notificationTime + " | now: " + now + " | test mode: " + isTestMode);
                alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
            } else {
                Log.d("NotificationWorker", "NOT Scheduling notification:  " +
                        notificationTime + " | now: " + now + " | in the past not Scheduling | test mode: " + isTestMode);
            }
        } else {
            Log.d("NotificationWorker", "no notification to schedule | test mode: false");
        }
    }

    private boolean scheduleNotificationForCandleLightingToday(ZmanimCalendar zcalToday, AlarmLocation clientsLocation) {
       boolean hasCandleLighting = ZmanimService.hasCandleLightingToday(clientsLocation);

        if (hasCandleLighting) {
            boolean isBeforeCandleLightingTime =  zcalToday.getCandleLighting().before(Calendar.getInstance().getTime());
            boolean isAfterMorning = LocalTime.now().isAfter(LocalTime.of(11, 0));
            return isBeforeCandleLightingTime && isAfterMorning ;
        }
        return false;

    }

    private Date getNotificationTime(ZmanimCalendar today, SharedPreferences sharedPreferences) {

        int timeBeforeShabbat = timeBeforeShabbatToSendNotificationInMinutes(sharedPreferences);

        Calendar notificationTime = Calendar.getInstance();

        if (isTestMode(sharedPreferences)) {
          //  notificationTime.set(Calendar.HOUR, notificationTime.get(Calendar.HOUR));
            notificationTime.set(Calendar.MINUTE, notificationTime.get(Calendar.MINUTE) + 1);
        } else {
            notificationTime.setTime(today.getCandleLighting());
            notificationTime.add(Calendar.MINUTE, -timeBeforeShabbat);
        }
        return notificationTime.getTime();
    }

    private int timeBeforeShabbatToSendNotificationInMinutes(SharedPreferences sharedPreferences) {
        String time = sharedPreferences.getString("pref_notification_time_before_shabbat", "60");
        return Integer.valueOf(time);
    }

    private boolean isTestMode(SharedPreferences myPrefs) {
        return myPrefs.getBoolean("testMode", false);
    }

}
