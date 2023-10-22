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
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

public class NotificationWorker extends Worker {

    private LocationService locationService;
    private int CANDLE_LIGHTING_OFFSET = 20;
    private int CANDLE_LIGHTING_OFFSET_JERUSALEM = 40;

    private int CANDLE_LIGHTING_REQUEST_CODE = 10;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationWorker", "NotificationWorker was called");
        scheduleNotificationForCandleLighting(true);
        Log.d("NotificationWorker", "NotificationWorker finished");
        return Result.success();
    }

    private void scheduleNotificationForCandleLighting(boolean isTest) {

        ZmanimCalendar zcalToday = getTodaysZmanimCalendar();

        if (scheduleNotificationForCandleLightingToday(zcalToday) || isTest) {

            Context context = getApplicationContext();

            Date notificationTime = null;

            if (isTest) {
                notificationTime = getNotificationTimeTest(zcalToday, context);
            } else {
                notificationTime = getNotificationTime(zcalToday, context);
            }
            Date now = Calendar.getInstance().getTime();

            PendingIntent pendingIntent =
                    IntentCreator.getNotificationPendingIntent(context, CANDLE_LIGHTING_REQUEST_CODE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

            if (notificationTime.after(now)) {
                Log.d("NotificationWorker", "Scheduling notification | time: " +
                        notificationTime + " | now: " + now );
                alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
            } else {
                Log.d("NotificationWorker", "Notification time: | " +
                        notificationTime + " | now: " + now + " | in the past not Scheduling");
            }
        }
    }

    private ZmanimCalendar getTodaysZmanimCalendar() {

        ZmanimCalendar zcalToday = new ZmanimCalendar();

        AlarmLocation clientsLocation = locationService.getClaientLocationDetails(getApplicationContext());

        if (clientsLocation.getCityCode().equals("JLM_IL")) {
            zcalToday.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET_JERUSALEM);
        } else {
            zcalToday.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET);
        }
        GeoLocation gl = locationService.getGeoLocationFromAlarmLocation(clientsLocation);
        zcalToday.setGeoLocation(gl);

        return zcalToday;

    }

    private boolean scheduleNotificationForCandleLightingToday(ZmanimCalendar zcalToday) {
        boolean inIsrael = true;

        JewishCalendar jc = new JewishCalendar();
        jc.setInIsrael(inIsrael);
        boolean hasCandleLighting = jc.hasCandleLighting();

        if (hasCandleLighting) {
            boolean isBeforeCandleLightingTime =  zcalToday.getCandleLighting().before(Calendar.getInstance().getTime());
            boolean isAfterMorning = LocalTime.now().isAfter(LocalTime.of(11, 0));
            return isBeforeCandleLightingTime && isAfterMorning ;
        }
        return false;

    }

    private Date getNotificationTime(ZmanimCalendar today, Context context) {

        int timeBeforeShabbat = timeBeforeShabbatToSendNotificationInMinutes(context);

        Calendar candleLightingTime = Calendar.getInstance();
        candleLightingTime.setTime(today.getCandleLighting());
        candleLightingTime.add(Calendar.MINUTE, -timeBeforeShabbat);

        return candleLightingTime.getTime();
    }

    private Date getNotificationTimeTest(ZmanimCalendar today, Context context) {

        int timeBeforeShabbat = timeBeforeShabbatToSendNotificationInMinutes(context);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, cal.get(Calendar.HOUR) + 1);
        cal.set(Calendar.MINUTE, 41);
        return cal.getTime();
    }
    private int timeBeforeShabbatToSendNotificationInMinutes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String time = sharedPreferences.getString("pref_notification_time_before_shabbat", "60");
        return Integer.valueOf(time);
    }

}
