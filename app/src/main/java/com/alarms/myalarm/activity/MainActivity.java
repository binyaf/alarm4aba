package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.alarms.myalarm.R;
import com.alarms.myalarm.tools.AlarmsPersistService;
import com.alarms.myalarm.tools.DateTimesFormats;
import com.alarms.myalarm.tools.IntentCreator;
import com.alarms.myalarm.tools.LocationService;
import com.alarms.myalarm.types.Alarm;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;
import com.bumptech.glide.Glide;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private Button editAlarmBtn;
    private Button deleteAlarmBtn;
    private AlarmManager alarmManager;
    private LocationService locationService;
    private LinearLayout linearLayout;
    private AlarmsPersistService alarmsPersistService;
    public static final int MAX_ALARMS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        locationService = new LocationService();
        alarmsPersistService = new AlarmsPersistService(getApplicationContext());
        String locationKey = getDefaultLocationKey();
        Log.d("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_details);

        deleteAlarmBtn = findViewById(R.id.deleteAlarmBtn);
        editAlarmBtn = findViewById(R.id.editAlarmBtn);
        Button addAlarmBtn = findViewById(R.id.addAdditionAlarmBtn);
        linearLayout = findViewById(R.id.alarmsLayout);
        List<Alarm> allAlarms = alarmsPersistService.getAlarmsList();

        TextView textView = findViewById(R.id.labelTextView);
        textView.setText(getString(R.string.todays_zmanim, locationKey));

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
            } else { //for some reason we have an alarm in the  past - let's remove it
                alarmsPersistService.removeAlarm(alarm);
            }
        }

        addAlarmBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetAlarmActivity.class);

            startActivity(intent);
        });

        ImageView todaysZemanimIcon = findViewById(R.id.timeIconImageView);
        todaysZemanimIcon.setOnClickListener(v -> {
            Log.d("MainActivity", "today's zmanim clicked");
            AlertDialog dialog = createTodayZmanimAlertDialog(getTodaysZmanim());
            dialog.show();
        });
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

            textView.setOnClickListener(new View.OnClickListener() {
                int clicksOnEmptyTextView = 0;
                @Override
                public void onClick(View v) {
                    clicksOnEmptyTextView++;

                    if (clicksOnEmptyTextView == 7) {
                        ImageView imageView = findViewById(R.id.imageView);
                        Glide.with(v.getContext()).asGif().load(R.raw.sha_shalom).into(imageView);
                        imageView.setVisibility(View.VISIBLE);
                        Log.d("AnimationClick", "Show animation");
                        clicksOnEmptyTextView = 0;
                        new CountDownTimer(15000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                // Do nothing
                            }
                            public void onFinish() {
                                imageView.setVisibility(View.GONE);
                                clicksOnEmptyTextView = 0;
                            }
                        }.start();
                    } else {
                        Log.d("AnimationClick", "number of clicks = " + clicksOnEmptyTextView);
                    }
                }
            });

            textView.setOnLongClickListener(v -> {
                //this alarm is not selected
                if (!textView.isSelected()) {

                    //first unselect all other alarms
                    deselectAllTextViews();

                    textView.setSelected(true);
                    editAlarmBtn.setEnabled(true);
                    editAlarmBtn.setVisibility(View.VISIBLE);
                    deleteAlarmBtn.setEnabled(true);
                    deleteAlarmBtn.setVisibility(View.VISIBLE);
                    textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_grey));

                    editAlarmBtn.setOnClickListener(z -> {
                        Intent i = new Intent(this, SetAlarmActivity.class);
                        i.putExtra(IntentKeys.ALARM, alarm);
                        startActivity(i);
                    });

                    deleteAlarmBtn.setOnClickListener(y -> {
                        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                        PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this,
                                getApplication(), alarm);

                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent);
                            Log.d("ALARM", "Alarm was canceled");
                        }
                        alarmsPersistService.removeAlarm(alarm);
                        startActivity(new Intent(this, MainActivity.class));
                    });

                } else {
                    deselectTextView(textView);
                    editAlarmBtn.setEnabled(false);
                    editAlarmBtn.setVisibility(View.INVISIBLE);
                    deleteAlarmBtn.setEnabled(false);
                    deleteAlarmBtn.setVisibility(View.INVISIBLE);
                }
                return true;
            });
        } else {
            throw new RuntimeException("this screen always needs to have 'alarmDateAndTime'");
        }

        textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.light_blue));
        textView.setTextSize(16);

        textView.setLayoutParams(layoutParams);
        linearLayout.addView(textView);

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

        zcal.setGeoLocation(locationService.getGeoLocation());

        long alarmTime = alarmDateAndTime.getTime();

        long midDay  = zcal.getChatzos().getTime();

        long szMGA = zcal.getSofZmanShmaMGA().getTime();
        long szGRA = zcal.getSofZmanShmaGRA().getTime();
        long szTfilaGRA  = zcal.getSofZmanTfilaGRA().getTime();
        long szTfilaMGA  = zcal.getSofZmanTfilaMGA().getTime();

        DateFormat timeFormat = DateTimesFormats.timeFormat;
        String szMGAStr = timeFormat.format(szMGA);
        String szGRAStr = timeFormat.format(szGRA);
        String szTfilaMgaStr = timeFormat.format(szTfilaMGA);
        String szTfilaGRAStr = timeFormat.format(szTfilaGRA);

        Log.d("ALARM",
                " type: " + alarm.getType() + " | " +
                " now: " + timeFormat.format(Calendar.getInstance().getTime()) + " | " +
                " alarm time: " + timeFormat.format(alarmDateAndTime) + " | " +
                " sz Shma MGa: " + szMGAStr + " | sz shma GRA: " + szGRAStr + " | " +
                " sz tfila GRA: " + szTfilaMgaStr + " | sz tfila MGA: " + szTfilaGRAStr + " | " +
                " sz Chazot: " + timeFormat.format(midDay));

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

        Calendar alarmDate = alarm.getDateAndTime();

        JewishDate jd = new JewishDate(alarmDate);
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        String hebrewDate = hdf.format(jd);

        String date = DateTimesFormats.dateFormat.format(alarmDate.getTime());
        String time = DateTimesFormats.timeFormat.format(alarmDate.getTime());
        String label = getString(R.string.alarm_will_start);
        String text = "<br><font color=" + Color.GRAY + "  size=3 >&nbsp;&nbsp;&nbsp;" + label + " </font><br>" +
                "<font size=30 color=" + Color.BLACK + ">&nbsp;<b> " + date +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + hebrewDate + "</b>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + time +
                "</font><br>";

        if (alarm.getType() == AlarmType.MINCHA) {
            text += "<br><font color=" + Color.GRAY + "  size=3 >15 min. before sunset</font><br>";
        }
        return text;
    }

    private AlertDialog createTodayZmanimAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Pair<Long, Long> location = alarmsPersistService.getLocation();

        builder.setTitle(getString(R.string.todays_zmanim, getDefaultLocationKey()))
                .setMessage(Html.fromHtml(msg))
                .setIcon(R.drawable.time_icon)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String getTodaysZmanim() {

        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(Calendar.getInstance());

        zcal.setGeoLocation(locationService.getGeoLocation());

        long midDay  = zcal.getChatzos().getTime();
        long szGRA = zcal.getSofZmanShmaGRA().getTime();
        long szMGA = zcal.getSofZmanShmaMGA().getTime();
        long szTfilaGRA  = zcal.getSofZmanTfilaGRA().getTime();
        long szTfilaMGA  = zcal.getSofZmanTfilaMGA().getTime();
        long sunset  = zcal.getSunset().getTime();

        DateFormat timeFormat = DateTimesFormats.timeFormat;
        return  "<br>" +  getString(R.string.latest_shma_gra, timeFormat.format(szMGA))  + " <br><br>" +
                          getString(R.string.latest_shma_mga, timeFormat.format(szGRA))  + " <br><br>" +
                          getString(R.string.latest_shacharis_mga, timeFormat.format(szTfilaMGA)) + " <br><br>" +
                          getString(R.string.latest_shacharis_gra, timeFormat.format(szTfilaGRA)) +  " <br><br>" +
                          getString(R.string.midday, timeFormat.format(midDay)) +  " <br><br>" +
                          getString(R.string.sunset, timeFormat.format(sunset));
    }

    private String getDefaultLocationKey() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> all = sharedPreferences.getAll();

        if (all.get("location") != null) {
           return (String)all.get("location");
        }
        return "";

    }
}
