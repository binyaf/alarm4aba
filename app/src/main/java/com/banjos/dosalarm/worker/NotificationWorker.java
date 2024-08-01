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

    private Context context;

    private SharedPreferences settingsPreference;
    private PreferencesService preferencesService;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        locationService = new LocationService();
        settingsPreference = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesService = new PreferencesService(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("NotificationWorker", "NotificationWorker was called");
        Context context = getApplicationContext();
        scheduleNotifications();
        return Result.success();
    }

    public void scheduleNotifications() {

        AlarmLocation clientsLocation = locationService.getClientLocationDetails(context);

        ZmanimCalendar zcalToday = ZmanimService.getTodaysZmanimCalendar(clientsLocation);

        Log.d("NotificationWorker", "Todays Zmanim\n" + zcalToday.toJSON());
        boolean isTestMode = preferencesService.isTestMode();

        if (preferencesService.isCandleLightReminderSelected()) {

            if (scheduleNotificationForCandleLightingToday(zcalToday, clientsLocation, context)) {

                Date notificationTime = getCandleLightingNotificationTime(zcalToday);

                PendingIntent pendingIntent =
                        getNotificationPendingIntent(context, NotificationType.CANDLE_LIGHTING_REMINDER);

                scheduleNotification(context, pendingIntent, notificationTime, NotificationType.CANDLE_LIGHTING_REMINDER);
            } else {
                Log.d("NotificationWorker", "type: " + NotificationType.CANDLE_LIGHTING_REMINDER + " | NOT Scheduling notification | no candle lighting today");
            }
        }

        if (preferencesService.isShacharisReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, NotificationType.SHACHARIT_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            Date notificationTime;
            if (isTestMode) {
                todayCal.add(Calendar.SECOND, 10);
                notificationTime = todayCal.getTime();
            } else {
                notificationTime = getShacharitNotificationTime(zcalToday);
            }
            scheduleNotification(context, pendingIntent, notificationTime, NotificationType.SHACHARIT_REMINDER);
        }

        if (preferencesService.isMinchaReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, NotificationType.MINCHA_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            Date notificationTime;

            notificationTime = getMinchaNotificationTime(zcalToday);

            scheduleNotification(context, pendingIntent, notificationTime, NotificationType.MINCHA_REMINDER);
        }

        if (preferencesService.isMaarivReminderSelected()) {
            PendingIntent pendingIntent =
                    getNotificationPendingIntent(context, NotificationType.MAARIV_REMINDER);
            Calendar todayCal = Calendar.getInstance();
            Date notificationTime = getMaarivNotificationTime(zcalToday);

            //want to avoid sending notifications on Shabbat etc.
            if (! ZmanimService.isNowAssurBemlacha(clientsLocation)) {
                scheduleNotification(context, pendingIntent, notificationTime, NotificationType.MAARIV_REMINDER);
            }
        }
    }

    public static void scheduleNotification(Context context, PendingIntent pendingIntent,
                                            Date notificationTime, NotificationType notificationType) {
        Date now = Calendar.getInstance().getTime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        if (notificationTime.after(now)) {
            PreferencesService preferencesService = new PreferencesService(context);

            Log.d("NotificationWorker", "type: " + notificationType + " | Scheduling notification | notification time: " +
                    notificationTime + " | now: " + now);
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
        } else {
            Log.d("NotificationWorker", "type: " + notificationType + " | NOT Scheduling notification | notification time " +
                    notificationTime + " | now: " + now + " | notification is in the past - not Scheduling");
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

        int timeBeforeShabbat =  preferencesService.getCandleLightingMinutesBeforeShabbatForReminder();

        Calendar notificationTime = Calendar.getInstance();

        notificationTime.setTime(today.getCandleLighting());
        notificationTime.add(Calendar.MINUTE, -timeBeforeShabbat);

        return notificationTime.getTime();
    }

    private Date getMaarivNotificationTime(ZmanimCalendar zcalToday) {
        int min = preferencesService.getMaarivMinutesAfterSunsetForReminder();

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(zcalToday.getSunset());
        todayCal.add(Calendar.MINUTE, +min);
        return todayCal.getTime();
    }

    private Date getMinchaNotificationTime(ZmanimCalendar zcalToday) {

        int min = preferencesService.getMinchaMinutesBeforeSunsetForReminder();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(zcalToday.getSunset());
        todayCal.add(Calendar.MINUTE, -min);
        return todayCal.getTime();
    }

    private Date getShacharitNotificationTime(ZmanimCalendar zcalToday) {
        int min = preferencesService.getShacharitMinutesBeforeSunriseForReminder();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(zcalToday.getSunrise());
        todayCal.add(Calendar.MINUTE, -min);
        return todayCal.getTime();
    }


}
