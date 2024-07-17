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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;

import com.banjos.dosalarm.R;
import com.banjos.dosalarm.tools.DateTimesFormats;
import com.banjos.dosalarm.tools.IntentCreator;
import com.banjos.dosalarm.tools.LocationService;
import com.banjos.dosalarm.tools.NotificationJobScheduler;
import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.Alarm;
import com.banjos.dosalarm.types.AlarmLocation;
import com.banjos.dosalarm.types.AlarmType;
import com.banjos.dosalarm.types.IntentKeys;
import com.banjos.dosalarm.types.NotificationType;
import com.banjos.dosalarm.worker.NotificationWorker;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String NOTIFICATIONS_WORK_SCHEDULED_KEY = "notificationsWorkScheduled";

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;

    private AlarmManager alarmManager;
    private LinearLayout linearLayout;
    private PreferencesService preferencesService;
    public static final int MAX_ALARMS = 4;
    private AlarmLocation alarmLocation;
    private String cityNameForPresentation;
    private LocationService locationService;

    private Switch shacharitReminderSwitch;
    private Switch minchaReminderSwitch;
    private Switch maarivReminderSwitch;
    private Switch candleLightReminderSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("MainActivity", "onCreate");
        Context context = getApplicationContext();
        locationService = new LocationService();

        preferencesService = new PreferencesService(context);

        SharedPreferences myPrefs = preferencesService.getMyPreferences();

        //this is supposed to be called once in the applications life... when client upgrades his app
        //we call the UpgradeReceiver and the job will be updated
        if (! isNotificationsWorkScheduled(myPrefs)) {
            NotificationJobScheduler.scheduleDailyNotificationsJob(context);
            markNotificationsWorkAsScheduled(myPrefs);
        }

        alarmLocation = locationService.getClientLocationDetails(context);
        cityNameForPresentation = getCityNameByCityCode(alarmLocation.getCityCode());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_details);

        Button addAlarmBtn = findViewById(R.id.addAdditionAlarmBtn);
        linearLayout = findViewById(R.id.alarmsLayout);
        List<Alarm> allAlarms = preferencesService.getAlarmsList();

        TextView todaysZmanimText = findViewById(R.id.labelTextView);
        todaysZmanimText.setText(getString(R.string.todays_zmanim, cityNameForPresentation));

        if (allAlarms.size() >= MAX_ALARMS) {
            addAlarmBtn.setEnabled(false);
            int backgroundColor = ContextCompat.getColor(this, R.color.grey); // Replace with your color resource
            addAlarmBtn.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        }
        // Create a LinearLayout.LayoutParams object with desired margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Set margins (left, top, right, bottom) as needed
        layoutParams.setMargins(10, 50, 10, 10);

        for (Alarm alarm : allAlarms) {
            if (alarm != null && alarm.getDateAndTime().after(Calendar.getInstance())) {
                createTextViewForAlarm(alarm, layoutParams);
            } else  if (alarm != null) { //if for some reason we have an alarm in the past - let's remove it
                preferencesService.removeAlarm(alarm);
            }
        }

        addAlarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetAlarmActivity.class);

            startActivity(intent);
        });

        ImageView todaysZmanimIcon = findViewById(R.id.timeIconImageView);
        todaysZmanimIcon.setOnClickListener(v -> {
            Log.d("MainActivity", "today's zmanim clicked");
            AlertDialog dialog = createTodayZmanimAlertDialog();
            dialog.show();
        });

        TextView todaysZmanimTxt = findViewById(R.id.labelTextView);

        todaysZmanimTxt.setOnClickListener(new View.OnClickListener() {
            int clicksOnEmptyTextView = 0;
            @Override
            public void onClick(View v) {
                clicksOnEmptyTextView++;

                if (clicksOnEmptyTextView == 7) {
                    AlertDialog dialog = createSevenClicksDialog();
                    dialog.show();
                    clicksOnEmptyTextView = 0;

                    //TODO remove
                    PendingIntent pendingIntent =
                            getNotificationPendingIntent(context, 11, NotificationType.SHACHARIT_REMINDER);
                    Calendar todayCal = Calendar.getInstance();
                    todayCal.add(Calendar.SECOND, 10);
                    NotificationWorker.scheduleNotification(context, pendingIntent, todayCal.getTime());
                    //
                } else {
                    Log.d("AnimationClick", "number of clicks = " + clicksOnEmptyTextView);
                }
            }
        });

        candleLightReminderSwitch = findViewById(R.id.candle_lighting_reminder_switch);
        maarivReminderSwitch = findViewById(R.id.maariv_reminder_switch);
        minchaReminderSwitch = findViewById(R.id.mincha_reminder_switch);
        shacharitReminderSwitch = findViewById(R.id.shacharit_reminder_switch);

        if (!userAgreedToReceiveNotifications()) {
            preferencesService.candleLightReminderSwitched(false);
            preferencesService.shacharitReminderSwitched(false);
            preferencesService.minchaReminderSwitched(false);
            preferencesService.maarivReminderSwitched(false);
        }

        candleLightReminderSwitch.setChecked(preferencesService.isCandleLightReminderSelected());
        maarivReminderSwitch.setChecked(preferencesService.isMaarivReminderSelected());
        minchaReminderSwitch.setChecked(preferencesService.isMinchaReminderSelected());
        shacharitReminderSwitch.setChecked(preferencesService.isShacharisReminderSelected());

        // Set listener for switch state changes
        shacharitReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (areNotificationsEnabled(context)) {
                            preferencesService.shacharitReminderSwitched(true);
                            //TODO run from here and from every other 'switch' NotificationWorker.scheduleNotifications - not critical
                        } else {
                            preferencesService.shacharitReminderSwitched(false);
                            shacharitReminderSwitch.setChecked(false);
                        }
                    } else {
                        preferencesService.shacharitReminderSwitched(false);
                    }
                });

        minchaReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (areNotificationsEnabled(context)) {
                    preferencesService.minchaReminderSwitched(true);
                } else {
                    preferencesService.minchaReminderSwitched(false);
                    minchaReminderSwitch.setChecked(false);
                }
            } else {
                preferencesService.minchaReminderSwitched(false);
            }
        });

        maarivReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (areNotificationsEnabled(context)) {
                    preferencesService.maarivReminderSwitched(true);
                } else {
                    preferencesService.maarivReminderSwitched(false);
                    maarivReminderSwitch.setChecked(false);
                }
            } else {
                preferencesService.maarivReminderSwitched(false);
            }
        });

        candleLightReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (areNotificationsEnabled(context)) {
                    preferencesService.candleLightReminderSwitched(true);
                } else {
                    preferencesService.candleLightReminderSwitched(false);
                    candleLightReminderSwitch.setChecked(false);
                }
            } else {
                preferencesService.candleLightReminderSwitched(false);
            }
        });
    }

    private boolean areNotificationsEnabled(Context context) {
        if (userAgreedToReceiveNotifications()) {
            Log.d("NotificationsPermissions", "User agreed to receive notifications");
            return true;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,  "android.permission.POST_NOTIFICATIONS")) {
            Log.d("NotificationsPermissions", "Requesting user to Approve sending Notifications");
            requestNotificationPermission();
        } else {
            Log.d("NotificationsPermissions", "User Denied Permission With 'Don't Ask Again', calling 'openNotificationSettings'");
            openNotificationSettings(context);
            return false;
        }
        return userAgreedToReceiveNotifications();

    }

    private boolean userAgreedToReceiveNotifications() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{"android.permission.POST_NOTIFICATIONS"},
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Add this line
        context.startActivity(intent);
    }

    private AlertDialog createSevenClicksDialog() {

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.sha_shalom);
        imageView.setVisibility(View.VISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(imageView);
        builder.setTitle(getString(R.string.shabbat_shalom))
                .setIcon(R.drawable.candles)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String getCityNameByCityCode(String cityCode) {
        int resourceId = getResources().
                getIdentifier(cityCode, "string", getPackageName());
        return getResources().getString(resourceId);
    }

    @Override
    public void onBackPressed() {
        Log.d("MainActivity", "back clicked- exiting ");
        finishAffinity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upper_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createTextViewForAlarm(Alarm alarm, LinearLayout.LayoutParams layoutParams) {

        TextView textView = new TextView(getApplicationContext());
        Calendar alarmDateAndTime = alarm.getDateAndTime();

        if (alarmDateAndTime != null) {

            String html = prepareAlarmDetailsText(alarm);
            String warning = buildWarningMsg(alarm);

            if (warning != null) {
                String warningStr = getString(R.string.warning);
                String fullMsg = html + warningStr + "&nbsp;&nbsp;";
                Spanned spanned = Html.fromHtml(fullMsg);
                Drawable icon = getResources().getDrawable(R.drawable.warning_icon);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BOTTOM);

                SpannableString spannableString = new SpannableString(spanned);
                int textLength = spannableString.length();

                spannableString.setSpan(imageSpan, textLength -1, textLength, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        AlertDialog dialog = createAlertDialog(warning);
                        dialog.show();
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        // Optionally, customize the appearance of the clickable span
                        ds.setUnderlineText(false); // Remove underline
                        ds.setColor(ContextCompat.getColor(getApplicationContext(), R.color.light_blue));
                    }
                };

                spannableString.setSpan(clickableSpan, textLength -1, textLength, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setHighlightColor(Color.TRANSPARENT);
                textView.setText(spannableString);

            } else {
                textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
            }

            textView.setVisibility(View.VISIBLE);

            textView.setOnLongClickListener(v -> {

                Button deleteAlarmBtn = findViewById(R.id.deleteAlarmBtn);
                Button editAlarmBtn = findViewById(R.id.editAlarmBtn);
                Button shareAlarmBtn = findViewById(R.id.shareAlarmButton);

                //this alarm is not selected
                if (!textView.isSelected()) {

                    //first unselect all other alarms
                    deselectAllTextViews();

                    textView.setSelected(true);
                    editAlarmBtn.setEnabled(true);
                    editAlarmBtn.setVisibility(View.VISIBLE);
                    deleteAlarmBtn.setEnabled(true);
                    deleteAlarmBtn.setVisibility(View.VISIBLE);
                    shareAlarmBtn.setEnabled(true);
                    shareAlarmBtn.setVisibility(View.VISIBLE);

                    textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_grey));

                    editAlarmBtn.setOnClickListener(z -> {
                        Intent i = new Intent(this, SetAlarmActivity.class);
                        i.putExtra(IntentKeys.ALARM, alarm);
                        startActivity(i);
                    });

                    deleteAlarmBtn.setOnClickListener(y -> {
                        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(
                                getApplicationContext(), alarm);

                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent);
                            Log.d("ALARM", "Alarm was canceled");
                        }
                        preferencesService.removeAlarm(alarm);
                        startActivity(new Intent(this, MainActivity.class));
                    });
                    shareAlarmBtn.setOnClickListener(view -> shareContent(alarm));

                } else {
                    deselectTextView(textView);
                    editAlarmBtn.setEnabled(false);
                    editAlarmBtn.setVisibility(View.INVISIBLE);
                    deleteAlarmBtn.setEnabled(false);
                    deleteAlarmBtn.setVisibility(View.INVISIBLE);
                    shareAlarmBtn.setEnabled(false);
                    shareAlarmBtn.setVisibility(View.INVISIBLE);
                }
                return true;
            });
        } else {
            throw new RuntimeException("this screen always needs to have 'alarmDateAndTime'");
        }

        textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_blue));
        textView.setTextSize(16);

        textView.setLayoutParams(layoutParams);
        textView.setGravity(Gravity.TOP);
        linearLayout.addView(textView);

    }

    private void shareContent(Alarm alarm) {
        Date alarmTime = alarm.getDateAndTime().getTime();
        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(alarmTime);
        String date = DateTimesFormats.dateFormat.format(alarmTime);
        String completeDate = date + ", " + hebrewDate;
        String time = DateTimesFormats.timeFormat.format(alarmTime);
        String shareText = getString(R.string.share_msg, completeDate, time);

        String mimeType = "text/plain";

        ShareCompat.IntentBuilder
                .from(this)
                .setType(mimeType)
                .setText(shareText)
                .startChooser();
    }

    private void deselectAllTextViews() {
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View childView = linearLayout.getChildAt(i);
            if (childView instanceof TextView && !(childView instanceof Button)) {
                TextView textView = (TextView) childView;
                deselectTextView(textView);
            }
        }
    }

    private void deselectTextView(TextView textView) {
        textView.setSelected(false);
        textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_blue));
    }

    private String buildWarningMsg(Alarm alarm) {
        Calendar alarmDateAndTimeCal = alarm.getDateAndTime();
        Date alarmDateAndTime = alarmDateAndTimeCal.getTime();

        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(alarmDateAndTimeCal);

        zcal.setGeoLocation(locationService.getGeoLocationFromAlarmLocation(alarmLocation));

        long alarmTime = alarmDateAndTime.getTime();

        long midDay  = zcal.getChatzos().getTime();

        long szMGA = zcal.getSofZmanShmaMGA().getTime();
        long szGRA = zcal.getSofZmanShmaGRA().getTime();
        long szTfilaGRA  = zcal.getSofZmanTfilaGRA().getTime();
        long szTfilaMGA  = zcal.getSofZmanTfilaMGA().getTime();
        zcal.getCandleLighting();

        DateFormat timeFormat = DateTimesFormats.timeFormat;
        String szMGAStr = timeFormat.format(szMGA);
        String szGRAStr = timeFormat.format(szGRA);
        String szTfilaMgaStr = timeFormat.format(szTfilaMGA);
        String szTfilaGRAStr = timeFormat.format(szTfilaGRA);

        Log.d("ALARM",
                "type: " + alarm.getType() + " | " +
                " now: " + timeFormat.format(Calendar.getInstance().getTime()) + " | " +
                " alarm time: " + timeFormat.format(alarmDateAndTime) );

        if (alarmTime > midDay) {
            return null;
        }

        boolean lateForShma = alarmTime > szGRA && alarmTime > szMGA;
        boolean lateForTfila = alarmTime > szTfilaGRA && alarmTime > szTfilaMGA;

        if (!lateForShma && !lateForTfila) {
            return null;
        } else {
            return buildWarningMsgHtml(lateForShma, lateForTfila, szMGAStr, szGRAStr, szTfilaMgaStr, szTfilaGRAStr);
        }
    }

    private String buildWarningMsgHtml(boolean lateForShma, boolean lateForTfila, String szMGAStr, String szGRAStr,
                                       String szTfilaMGAStr, String szTfilaGRAStr) {

        StringBuilder sb = new StringBuilder();

        if (lateForShma) {
            sb.append("<br>");
            sb.append("<font size=25 color=" + Color.RED + ">");
            sb.append(getString(R.string.alarm_after_shma)).append("<br>");
            sb.append("</font>");
            sb.append("<font size=25 color=" + Color.WHITE + ">");
            sb.append(getString(R.string.latest_shma_mga, szMGAStr)).append("<br>");
            sb.append(getString(R.string.latest_shma_gra, szGRAStr)).append("<br>");
            sb.append("</font>");
        }

        if (lateForTfila) {
            sb.append("<br>");
            sb.append("<font size=25 color=" + Color.RED + ">");
            sb.append(getString(R.string.alarm_after_tfilah)).append("<br>");
            sb.append("</font>");
            sb.append("<font size=25 color=" + Color.WHITE + ">");
            sb.append(getString(R.string.latest_shacharis_mga, szTfilaMGAStr)).append("<br>");
            sb.append(getString(R.string.latest_shacharis_gra, szTfilaGRAStr)).append("<br>");
            sb.append("</font>");
        }
        return sb.toString();
    }

    private AlertDialog createAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.warning))
                .setMessage(Html.fromHtml(msg))
                .setIcon(R.drawable.warning_icon)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String prepareAlarmDetailsText(Alarm alarm) {
        Date alarmDate = alarm.getDateAndTime().getTime();

        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(alarmDate);

        String date = DateTimesFormats.dateFormat.format(alarmDate);
        String time = DateTimesFormats.timeFormat.format(alarmDate);
        String label;
        if (alarm.getLabel() != null && !alarm.getLabel().isEmpty()) {
            label = getString(R.string.alarm_will_start_with_label, "<b>" + alarm.getLabel() + "</b>");
        } else {
            label = getString(R.string.alarm_will_start);
        }
        String text = "<br><font color=" + Color.GRAY + "  size=3 >&nbsp;&nbsp;&nbsp;" + label + " </font><br>" +
                "<font size=30 color=" + Color.BLACK + ">&nbsp;<b> " + date +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + hebrewDate +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + time + "</b></font><br>";

        if (alarm.getType() == AlarmType.MINCHA) {
            text += "<br><font color=" + Color.GRAY + "  size=3 >15 min. before sunset</font><br>";
        }
        return text;
    }

    private AlertDialog createTodayZmanimAlertDialog() {
        String msg = getTodaysZmanim();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.todays_zmanim, cityNameForPresentation))
                .setMessage(Html.fromHtml(msg))
                .setIcon(R.drawable.time_icon)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String getTodaysZmanim() {
        GeoLocation gl = locationService.getGeoLocationFromAlarmLocation(alarmLocation);
        DateFormat timeFormat = DateTimesFormats.timeFormat;
        timeFormat.setTimeZone(gl.getTimeZone());

        ZmanimCalendar zcal = new ZmanimCalendar(gl);
        ComplexZmanimCalendar czc = new ComplexZmanimCalendar(gl);
        ;

        StringBuilder sb = new StringBuilder("<br>");
        sb.append(getString(R.string.sunrise, timeFormat.format(zcal.getSunrise()))).append(" <br><br>")
                .append(getString(R.string.latest_shma_mga, timeFormat.format(zcal.getSofZmanShmaMGA()))).append(" <br><br>")
                .append(getString(R.string.latest_shma_gra, timeFormat.format(zcal.getSofZmanShmaGRA()))).append(" <br><br>")
                .append(getString(R.string.latest_shacharis_mga, timeFormat.format(zcal.getSofZmanTfilaMGA()))).append(" <br><br>")
                .append(getString(R.string.latest_shacharis_gra, timeFormat.format(zcal.getSofZmanTfilaGRA()))).append(" <br><br>")
                .append(getString(R.string.midday, timeFormat.format(zcal.getChatzos()))).append(" <br><br>")
                .append(getString(R.string.sunset, timeFormat.format(zcal.getSunset()))).append(" <br><br>")
                .append(getString(R.string.nightfall, timeFormat.format(zcal.getTzais()))).append(" <br><br>")
                .append(getString(R.string.nightfall, timeFormat.format(czc.getTzaisGeonim6Point45Degrees()))).append(" <br><br>")
                .append(" <br><br>");
        return sb.toString();

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
                // Permission was granted, show the notification
                Toast.makeText(this, "Notification permission approvaed", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
