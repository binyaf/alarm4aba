package com.banjos.dosalarm.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
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
    private int CANDLE_LIGHTING_OFFSET = 20;
    private int CANDLE_LIGHTING_OFFSET_JERUSALEM = 40;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationWorker", "NotificationWorker was called");
        scheduleNotification();
        Log.d("NotificationWorker", "NotificationWorker finished");
        return Result.success();
    }

    private void scheduleNotification() {

        ZmanimCalendar zcalToday = new ZmanimCalendar();
        Calendar today = Calendar.getInstance();
        zcalToday.setCalendar(today);

        ZmanimCalendar zcalTomorrow = new ZmanimCalendar();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        zcalTomorrow.setCalendar(tomorrow);

        boolean inIsrael = true;
        //I pass the sunrise (and not the sunset) since I'm interested in the day in general, not in a specific time on the day.
        boolean tomorrowIsAssurBemlacha = zcalTomorrow.isAssurBemlacha(tomorrow.getTime(), zcalTomorrow.getSunrise(), inIsrael);
        boolean todayIsAssurBemlacha = zcalToday.isAssurBemlacha(today.getTime(), zcalToday.getSunrise(), inIsrael);

        if (!todayIsAssurBemlacha && tomorrowIsAssurBemlacha) {
            Context context = getApplicationContext();

            Date notificationTime = getNotificationTime(zcalToday, context);

            PendingIntent pendingIntent = IntentCreator.getNotificationPendingIntent(context);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
        }
    }

    private Date getNotificationTime(ZmanimCalendar today, Context context) {
        int timeBeforeShabbat = timeBeforeShabbatToSendNotificationInMinutes(context);
        Calendar  candleLightingTime = getCandleLightingTime(today, context);
        candleLightingTime.add(Calendar.MINUTE, -timeBeforeShabbat);
        Log.d("NotificationWorker", "timeBeforeShabbat (minutes) to send notification = " + timeBeforeShabbat);
        Log.d("NotificationWorker", "candleLightingTime = " + timeBeforeShabbat);
        return candleLightingTime.getTime();

    }

    private int timeBeforeShabbatToSendNotificationInMinutes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("pref_notification_time_before_shabbat", 60);
    }

    private Calendar getCandleLightingTime(ZmanimCalendar today, Context context) {

        AlarmLocation clientsLocation = locationService.getClaientLocationDetails(context);
        GeoLocation gl = locationService.getGeoLocationFromAlarmLocation(clientsLocation);
        today.setGeoLocation(gl);

        if (clientsLocation.getCityCode().equals("JLM_IL")) {
            today.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET_JERUSALEM);
        } else {
            today.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today.getCandleLighting());
        return calendar;
    }
}
