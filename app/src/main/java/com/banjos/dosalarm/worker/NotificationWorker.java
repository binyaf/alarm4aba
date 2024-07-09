package com.banjos.dosalarm.worker;

import static com.banjos.dosalarm.tools.IntentCreator.getNotificationPendingIntent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.NotificationType;
import com.kosherjava.zmanim.ZmanimCalendar;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

public class NotificationWorker extends Worker {

    private LocationService locationService;
    private int CANDLE_LIGHTING_REQUEST_CODE = 10;
    public static int SHACHARIS_REQUEST_CODE = 11;
    private int MINCHA_REQUEST_CODE = 12;
    private int MAARIV_REQUEST_CODE = 13;

    private SharedPreferences settingsPreference;
    private PreferencesService preferencesService;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
        settingsPreference = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesService = new PreferencesService(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationWorker", "NotificationWorker was called");
        Context context = getApplicationContext();
        scheduleNotifications(context);
        return Result.success();
    }

    private void scheduleNotifications(Context context) {

        AlarmLocation clientsLocation = locationService.getClientLocationDetails(context);

        ZmanimCalendar zcalToday = ZmanimService.getTodaysZmanimCalendar(clientsLocation);

        //boolean isTestMode = preferencesService.isTestMode();

        boolean scheduleCandleLightingNotification = scheduleNotificationForCandleLightingToday(zcalToday, clientsLocation, context);

        if (scheduleCandleLightingNotification) {

            Date notificationTime = getCandleLightingNotificationTime(zcalToday);

            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, CANDLE_LIGHTING_REQUEST_CODE, NotificationType.CANDLE_LIGHTING_REMINDER);

            scheduleNotification(context, pendingIntent, notificationTime);
        }

        if (preferencesService.isShacharisReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, SHACHARIS_REQUEST_CODE, NotificationType.SHACHARIT_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            todayCal.add(Calendar.SECOND, 10);
            scheduleNotification(context, pendingIntent, todayCal.getTime());
        }

        if (preferencesService.isMinchaReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, MINCHA_REQUEST_CODE, NotificationType.MINCHA_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            todayCal.add(Calendar.SECOND, 20);
            scheduleNotification(context, pendingIntent, todayCal.getTime());
        }

        if (preferencesService.isMaarivReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, MAARIV_REQUEST_CODE, NotificationType.MAARIV_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            todayCal.add(Calendar.SECOND, 30);
            scheduleNotification(context, pendingIntent, todayCal.getTime());
        }
    }

    public static void scheduleNotification(Context context, PendingIntent pendingIntent, Date notificationTime) {
        Date now = Calendar.getInstance().getTime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        if (notificationTime.after(now)) {
            Log.d("NotificationWorker", "Scheduling notification | time: " +
                    notificationTime + " | now: " + now);
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
        } else {
            Log.d("NotificationWorker", "NOT Scheduling notification:  " +
                    notificationTime + " | now: " + now + " | in the past not Scheduling");
        }
    }

    private boolean scheduleNotificationForCandleLightingToday(ZmanimCalendar zcalToday,
                                                               AlarmLocation clientsLocation, Context context) {
        boolean hasCandleLighting =
                  ZmanimService.hasCandleLightingToday(clientsLocation, context);

        if (hasCandleLighting) {
            Date now = Calendar.getInstance().getTime();
            boolean isNowBeforeCandleLightingTime = now.before(zcalToday.getCandleLighting());
            boolean isAfterMorning = LocalTime.now().isAfter(LocalTime.of(10, 0));
            return isNowBeforeCandleLightingTime && isAfterMorning;
        }
        return false;
    }

    private Date getCandleLightingNotificationTime(ZmanimCalendar today) {

        int timeBeforeShabbat = timeBeforeShabbatToSendNotificationInMinutes();

        Calendar notificationTime = Calendar.getInstance();

       /* if (PreferencesService.isTestMode()) {
            //  notificationTime.set(Calendar.HOUR, notificationTime.get(Calendar.HOUR));
            notificationTime.set(Calendar.MINUTE, notificationTime.get(Calendar.MINUTE) + 1);
        } else {*/
            notificationTime.setTime(today.getCandleLighting());
            notificationTime.add(Calendar.MINUTE, -timeBeforeShabbat);
      //  }
        return notificationTime.getTime();
    }

    private int timeBeforeShabbatToSendNotificationInMinutes() {
        String time = settingsPreference.getString("pref_notification_time_before_shabbat", "60");
        return Integer.valueOf(time);
    }

}
