package com.banjos.dosalarm.activity;

import static com.banjos.dosalarm.tools.IntentCreator.getNotificationPendingIntent;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.adapter.AlarmAdapter;
import com.banjos.dosalarm.databinding.AlarmDetailsBinding;
import com.banjos.dosalarm.tools.DateTimesFormats;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.NotificationJobScheduler;
import com.banjos.dosalarm.tools.NotificationScheduler;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.IntentKeys;
import com.banjos.dosalarm.types.NotificationType;
import com.banjos.dosalarm.worker.NotificationWorker;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AlarmDetailsBinding binding;
    private AlarmAdapter alarmAdapter;
    private AlarmManager alarmManager;
    private PreferencesService preferencesService;
    public static final int MAX_ALARMS = 5;
    private AlarmLocation alarmLocation;
    private String cityNameForPresentation;
    private LocationService locationService;

    private static final String NOTIFICATIONS_WORK_SCHEDULED_KEY = "notificationsWorkScheduled";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Initialize default preferences from XML
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        binding = AlarmDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.alarmDetailsMain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d("MainActivity", "onCreate");
        Context context = getApplicationContext();
        locationService = new LocationService();
        preferencesService = new PreferencesService(context);

        SharedPreferences myPrefs = preferencesService.getMyPreferences();

        if (!isNotificationsWorkScheduled(myPrefs)) {
            NotificationJobScheduler.scheduleDailyNotificationsJob(context);
            markNotificationsWorkAsScheduled(myPrefs);
        }

        alarmLocation = locationService.getClientLocationDetails(context);
        cityNameForPresentation = getCityNameByCityCode(alarmLocation.getCityCode());

        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.app_name);
            }
        }

        setupRecyclerView();
        loadAlarms();
        hideActionButtons();

        binding.addAdditionAlarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetAlarmActivity.class);
            startActivity(intent);
        });

        binding.timeIconImageView.setOnClickListener(v -> {
            Log.d("MainActivity", "today's zmanim clicked");
            createTodayZmanimAlertDialog().show();
        });

        binding.labelTextView.setOnClickListener(new View.OnClickListener() {
            int clicksOnEmptyTextView = 0;
            @Override
            public void onClick(View v) {
                clicksOnEmptyTextView++;
                if (clicksOnEmptyTextView == 7) {
                    createSevenClicksDialog().show();
                    clicksOnEmptyTextView = 0;

                    NotificationType type = NotificationType.MINCHA_REMINDER;
                    PendingIntent pendingIntent = getNotificationPendingIntent(context, type);
                    Calendar todayCal = Calendar.getInstance();
                    todayCal.add(Calendar.SECOND, 10);
                    NotificationScheduler.scheduleNotification(context, pendingIntent, todayCal.getTime(), type);
                }
            }
        });

        setupReminders(context);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Refresh location and city name every time we return to this screen
        alarmLocation = locationService.getClientLocationDetails(this);
        if (alarmLocation != null) {
            cityNameForPresentation = getCityNameByCityCode(alarmLocation.getCityCode());
        } else {
            // Fallback to Jerusalem if loading fails
            cityNameForPresentation = getResources().getString(R.string.JLM_IL);
        }
        
        loadAlarms();
        hideActionButtons();
    }

    private void setupRecyclerView() {
        alarmAdapter = new AlarmAdapter((alarm, view) -> {
            // Create a new list copy
            List<Alarm> currentAlarms = new ArrayList<>(alarmAdapter.getCurrentList());
            
            boolean wasSelected = alarm.isSelected();
            
            // Clear all selections first
            for (Alarm a : currentAlarms) {
                a.setSelected(false);
            }
            
            // Toggle selection for the clicked alarm
            // We find the alarm in the new list to be safe
            for (Alarm a : currentAlarms) {
                if (a.getId() == alarm.getId()) {
                    a.setSelected(!wasSelected);
                    break;
                }
            }
            
            alarmAdapter.submitList(currentAlarms);
            alarmAdapter.notifyDataSetChanged();

            if (!wasSelected) {
                binding.addAdditionAlarmBtn.setVisibility(View.GONE);
                binding.editAlarmBtn.setEnabled(true);
                binding.editAlarmBtn.setVisibility(View.VISIBLE);
                binding.deleteAlarmBtn.setEnabled(true);
                binding.deleteAlarmBtn.setVisibility(View.VISIBLE);
                binding.shareAlarmButton.setEnabled(true);
                binding.shareAlarmButton.setVisibility(View.VISIBLE);

                binding.editAlarmBtn.setOnClickListener(z -> {
                    Intent i = new Intent(this, SetAlarmActivity.class);
                    i.putExtra(IntentKeys.ALARM, alarm);
                    startActivity(i);
                });

                binding.deleteAlarmBtn.setOnClickListener(y -> {
                    alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(getApplicationContext(), alarm);
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent);
                    }
                    preferencesService.removeAlarm(alarm);
                    loadAlarms();
                    hideActionButtons();
                });

                binding.shareAlarmButton.setOnClickListener(view1 -> shareContent(alarm));
            } else {
                hideActionButtons();
            }
        });

        binding.alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.alarmsRecyclerView.setAdapter(alarmAdapter);
    }

    private void hideActionButtons() {
        binding.addAdditionAlarmBtn.setVisibility(View.VISIBLE);
        binding.editAlarmBtn.setVisibility(View.GONE);
        binding.deleteAlarmBtn.setVisibility(View.GONE);
        binding.shareAlarmButton.setVisibility(View.GONE);
    }

    private void loadAlarms() {
        List<Alarm> allAlarms = preferencesService.getAlarmsList();
        Calendar now = Calendar.getInstance();
        List<Alarm> activeAlarms = new ArrayList<>();
        for (Alarm alarm : allAlarms) {
            if (alarm != null) {
                if (alarm.getDateAndTime().after(now)) {
                    alarm.setSelected(false); // Ensure no alarm is selected on load
                    activeAlarms.add(alarm);
                } else {
                    preferencesService.removeAlarm(alarm);
                }
            }
        }

        alarmAdapter.submitList(activeAlarms);
        binding.labelTextView.setText(getString(R.string.todays_zmanim, cityNameForPresentation));

        if (activeAlarms.size() >= MAX_ALARMS) {
            binding.addAdditionAlarmBtn.setEnabled(false);
            binding.addAdditionAlarmBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey)));
        } else {
            binding.addAdditionAlarmBtn.setEnabled(true);
            binding.addAdditionAlarmBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
        }
    }

    private void setupReminders(Context context) {
        if (!userAgreedToReceiveNotifications()) {
            preferencesService.candleLightReminderSwitched(false);
            preferencesService.shacharitReminderSwitched(false);
            preferencesService.minchaReminderSwitched(false);
            preferencesService.maarivReminderSwitched(false);
        }

        binding.candleLightingReminderSwitch.setChecked(preferencesService.isCandleLightReminderSelected());
        binding.maarivReminderSwitch.setChecked(preferencesService.isMaarivReminderSelected());
        binding.minchaReminderSwitch.setChecked(preferencesService.isMinchaReminderSelected());
        binding.shacharitReminderSwitch.setChecked(preferencesService.isShacharisReminderSelected());

        binding.shacharitReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleReminderSwitch(isChecked, "shacharit", context));
        binding.minchaReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleReminderSwitch(isChecked, "mincha", context));
        binding.maarivReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleReminderSwitch(isChecked, "maariv", context));
        binding.candleLightingReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleReminderSwitch(isChecked, "candle", context));
    }

    private void handleReminderSwitch(boolean isChecked, String type, Context context) {
        if (isChecked) {
            if (areNotificationsEnabled(context)) {
                updatePreference(type, true);
                runSchedulingJob(context);
            } else {
                updatePreference(type, false);
                resetSwitch(type);
            }
        } else {
            updatePreference(type, false);
        }
    }

    private void updatePreference(String type, boolean value) {
        switch (type) {
            case "shacharit": preferencesService.shacharitReminderSwitched(value); break;
            case "mincha": preferencesService.minchaReminderSwitched(value); break;
            case "maariv": preferencesService.maarivReminderSwitched(value); break;
            case "candle": preferencesService.candleLightReminderSwitched(value); break;
        }
    }

    private void resetSwitch(String type) {
        switch (type) {
            case "shacharit": binding.shacharitReminderSwitch.setChecked(false); break;
            case "mincha": binding.minchaReminderSwitch.setChecked(false); break;
            case "maariv": binding.maarivReminderSwitch.setChecked(false); break;
            case "candle": binding.candleLightingReminderSwitch.setChecked(false); break;
        }
    }

    private void runSchedulingJob(Context context) {
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest);
    }

    private boolean areNotificationsEnabled(Context context) {
        if (userAgreedToReceiveNotifications()) return true;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            requestNotificationPermission();
        } else {
            openNotificationSettings(context);
            return false;
        }
        return userAgreedToReceiveNotifications();
    }

    private boolean userAgreedToReceiveNotifications() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
    }

    private void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private AlertDialog createSevenClicksDialog() {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.sha_shalom);
        imageView.setVisibility(View.VISIBLE);

        return new AlertDialog.Builder(this)
                .setView(imageView)
                .setTitle(getString(R.string.shabbat_shalom))
                .setIcon(R.drawable.candles)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private String getCityNameByCityCode(String cityCode) {
        if (cityCode == null) return "";
        int resourceId = getResources().getIdentifier(cityCode, "string", getPackageName());
        if (resourceId != 0) {
            return getResources().getString(resourceId);
        }
        return cityCode;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upper_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareContent(Alarm alarm) {
        Date alarmTime = alarm.getDateAndTime().getTime();
        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(alarmTime);
        String date = DateTimesFormats.dateFormat.format(alarmTime);
        String completeDate = date + ", " + hebrewDate;
        String time = DateTimesFormats.timeFormat.format(alarmTime);
        String shareText = getString(R.string.share_msg, completeDate, time);

        ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(shareText)
                .startChooser();
    }

    private AlertDialog createTodayZmanimAlertDialog() {
        String msg = getTodaysZmanim();
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.todays_zmanim, cityNameForPresentation))
                .setMessage(Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY))
                .setIcon(R.drawable.time_icon)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private String getTodaysZmanim() {
        GeoLocation gl = locationService.getGeoLocationFromAlarmLocation(alarmLocation);
        DateFormat timeFormat = DateTimesFormats.timeFormat;
        timeFormat.setTimeZone(gl.getTimeZone());

        ZmanimCalendar zcal = new ZmanimCalendar(gl);
        ComplexZmanimCalendar czc = new ComplexZmanimCalendar(gl);

        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(new Date());
        return "<br><center>" + hebrewDate + "</center> <br><br><br>" +
                getString(R.string.dawn, timeFormat.format(zcal.getAlosHashachar())) + " <br><br>" +
                getString(R.string.sunrise, timeFormat.format(zcal.getSunrise())) + " <br><br>" +
                getString(R.string.latest_shma_mga, timeFormat.format(zcal.getSofZmanShmaMGA())) + " <br><br>" +
                getString(R.string.latest_shma_gra, timeFormat.format(zcal.getSofZmanShmaGRA())) + " <br><br>" +
                getString(R.string.latest_shacharis_mga, timeFormat.format(zcal.getSofZmanTfilaMGA())) + " <br><br>" +
                getString(R.string.latest_shacharis_gra, timeFormat.format(zcal.getSofZmanTfilaGRA())) + " <br><br>" +
                getString(R.string.midday, timeFormat.format(zcal.getChatzos())) + " <br><br>" +
                getString(R.string.sunset, timeFormat.format(zcal.getSunset())) + " <br><br>" +
                getString(R.string.nightfall, timeFormat.format(czc.getTzaisBaalHatanya())) + " <br><br>";
    }

    private boolean isNotificationsWorkScheduled(SharedPreferences myPrefs) {
        return myPrefs.getBoolean(NOTIFICATIONS_WORK_SCHEDULED_KEY, false);
    }

    private void markNotificationsWorkAsScheduled(SharedPreferences myPrefs) {
        myPrefs.edit().putBoolean(NOTIFICATIONS_WORK_SCHEDULED_KEY, true).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission approved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}