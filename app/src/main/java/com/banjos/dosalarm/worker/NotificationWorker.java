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

import com.banjos.dosalarm.tools.DateTimesFormats;
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

        boolean isTestMode = preferencesService.isTestMode();

        boolean scheduleCandleLightingNotification = scheduleNotificationForCandleLightingToday(zcalToday, clientsLocation, context);

        boolean schedulePrayerNotification = schedulePrayerNotification();

        if (scheduleCandleLightingNotification) {

            Date notificationTime = getCandleLightingNotificationTime(zcalToday);

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
        }

        if (schedulePrayerNotification) {
            schedulePrayerReminders(context);
        }

        if (!scheduleCandleLightingNotification && !schedulePrayerNotification) {
            Log.d("NotificationWorker", "no notification to schedule | test mode: " + isTestMode);
        }

    }

    public static void schedulePrayerReminders(Context context) {
        Log.d("NotificationWorker", "about to schedule PN ");

        Calendar nowCal = Calendar.getInstance();
        nowCal.add(Calendar.SECOND, 10);
        Date notificationTime = nowCal.getTime();

        PendingIntent pendingIntent =
                IntentCreator.getPrayerReminderPendingIntent(context, SHACHARIS_REQUEST_CODE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime.getTime(), pendingIntent);
        String date =  DateTimesFormats.dateFormat.format(notificationTime);
        Log.d("NotificationWorker", "scheduled PN | time:" + date);
    }

    private boolean schedulePrayerNotification() {
        boolean notifyMaariv = preferencesService.isMaarivReminderSelected();
        boolean notifyMincha =  preferencesService.isMinchaReminderSelected();
        boolean notifyShacharis = preferencesService.isShacharisReminderSelected();

        return notifyMaariv || notifyMincha || notifyShacharis;
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
