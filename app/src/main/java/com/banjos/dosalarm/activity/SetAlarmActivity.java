package com.banjos.dosalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.databinding.AddAlarmScreenBinding;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmType;
import com.banjos.dosalarm.types.IntentKeys;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SetAlarmActivity extends AppCompatActivity {

    private AddAlarmScreenBinding binding;
    private AlarmManager alarmManager;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private int testModeClicks = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = AddAlarmScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.addAlarmMain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupToolbar();

        final Alarm alarm = initializeAlarm();
        final Calendar alarmDateAndTime = alarm.getDateAndTime();
        
        setupViews(alarm, alarmDateAndTime);
    }

    private void setupToolbar() {
        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private Alarm initializeAlarm() {
        Alarm alarm;
        if (getIntent() != null && getIntent().getSerializableExtra(IntentKeys.ALARM) != null) {
            alarm = (Alarm) getIntent().getSerializableExtra(IntentKeys.ALARM);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_alarm);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.add_alarm);
            }
            Calendar alarmDateAndTime = createCalendarWithDefaultValues();
            int defaultDuration = getDefaultDuration();
            alarm = new Alarm(AlarmType.REGULAR, defaultDuration, alarmDateAndTime);
        }

        handleTestMode();
        return alarm;
    }

    private void handleTestMode() {
        PreferencesService preferencesService = new PreferencesService(getApplicationContext());
        SharedPreferences myPrefs = preferencesService.getMyPreferences();
        boolean isTestMode = myPrefs.getBoolean("testMode", false);

        if (isTestMode && binding.toolbar != null) {
            binding.toolbar.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.red));
        }

        if (binding.toolbar != null) {
            binding.toolbar.toolbar.setOnClickListener(v -> {
                testModeClicks++;
                if (testModeClicks == 13) {
                    boolean newValue = !myPrefs.getBoolean("testMode", false);
                    myPrefs.edit().putBoolean("testMode", newValue).apply();
                    if (newValue) {
                        binding.toolbar.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.red));
                    } else {
                        binding.toolbar.toolbar.setTitleTextColor(Color.WHITE);
                    }
                    testModeClicks = 0;
                }
            });
        }
    }

    private void setupViews(Alarm alarm, Calendar alarmDateAndTime) {
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        binding.selectDateText.setText(dateFormat.format(alarmDateAndTime.getTime()));
        binding.selectTimeText.setText(timeFormat.format(alarmDateAndTime.getTime()));
        binding.alarmLabelTextView.setText(alarm.getLabel());

        binding.alarmLabelTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                binding.alarmLabelTextView.clearFocus();
            }
            return false;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.numberPicker.setTextColor(Color.BLACK);
        }

        setupNumberPicker(alarm);

        binding.selectTimeText.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                alarmDateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                alarmDateAndTime.set(Calendar.MINUTE, minute);
                binding.selectTimeText.setText(timeFormat.format(alarmDateAndTime.getTime()));
            }, alarmDateAndTime.get(Calendar.HOUR_OF_DAY), alarmDateAndTime.get(Calendar.MINUTE), true).show();
        });

        binding.selectDateText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
                alarmDateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                alarmDateAndTime.set(Calendar.YEAR, year);
                alarmDateAndTime.set(Calendar.MONTH, monthOfYear);
                binding.selectDateText.setText(dateFormat.format(alarmDateAndTime.getTime()));
            }, alarmDateAndTime.get(Calendar.YEAR), alarmDateAndTime.get(Calendar.MONTH), alarmDateAndTime.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
            dialog.show();
        });

        binding.addAlarmOk.setOnClickListener(v -> handleSave(alarm, alarmDateAndTime));
        binding.addAlarmCancel.setOnClickListener(v -> finish());
    }

    private void setupNumberPicker(Alarm alarm) {
        String[] alarmDisplayedValues = new String[]{"5", "10", "15", "20", "25", "30", "40", "50", "60", "70", "80", "90", "100", "110", "120"};
        binding.numberPicker.setMinValue(0);
        binding.numberPicker.setMaxValue(alarmDisplayedValues.length - 1);
        binding.numberPicker.setDisplayedValues(alarmDisplayedValues);
        
        Map<Integer, Integer> alarmDurationMap = greateAlarmDurationMap();
        binding.numberPicker.setValue(alarmDurationMap.getOrDefault(alarm.getDuration(), 3)); // Default to 20 if not found
    }

    private void handleSave(Alarm alarm, Calendar alarmDateAndTime) {
        if (alarmDateAndTime.after(Calendar.getInstance())) {
            String[] alarmDisplayedValues = binding.numberPicker.getDisplayedValues();
            alarm.setDuration(Integer.parseInt(alarmDisplayedValues[binding.numberPicker.getValue()]));

            String labelStr = binding.alarmLabelTextView.getText().toString();
            if (labelStr.length() > 25) {
                labelStr = labelStr.substring(0, 24);
            }
            alarm.setLabel(labelStr);

            saveAlarm(alarm);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(Html.fromHtml("alarm is in the past", Html.FROM_HTML_MODE_LEGACY))
                    .setIcon(R.drawable.warning_icon)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Map<Integer, Integer> greateAlarmDurationMap() {
        Map<Integer, Integer> map = new TreeMap<>();
        int[] values = {5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120};
        for (int i = 0; i < values.length; i++) {
            map.put(values[i], i);
        }
        return map;
    }

    private int getDefaultDuration() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            return Integer.parseInt(sharedPreferences.getString("alarm_duration", "20"));
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    private void saveAlarm(Alarm alarm) {
        if (alarm.getId() == 0) {
            alarm.setId(UUID.randomUUID().hashCode());
        }
        createActualAlarm(alarm);
        saveAlarmToSharePreferences(alarm);
    }

    private void createActualAlarm(Alarm alarm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            return;
        }

        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(getApplicationContext(), alarm);
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarm.getDateAndTime().getTimeInMillis(), pendingIntent);

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        } catch (SecurityException e) {
            Log.e("setAlarmClock", "Couldn't set alarm, security issue", e);
        }
    }

    private void saveAlarmToSharePreferences(Alarm alarm) {
        PreferencesService preferencesService = new PreferencesService(getApplicationContext());
        Map<Integer, Alarm> allAlarms = preferencesService.getAlarms();
        allAlarms.put(alarm.getId(), alarm);
        preferencesService.saveAlarms(allAlarms);
    }

    private Calendar createCalendarWithDefaultValues() {
        Calendar cal = Calendar.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultTime = sharedPreferences.getString("default_alarm_time", "08:00");

        try {
            String[] timeParts = defaultTime.split(":");
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            cal.set(Calendar.MINUTE, timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0);
        } catch (Exception e) {
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 0);
        }

        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        return cal;
    }
}