package com.alarms.myalarm.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.alarms.myalarm.R;
import com.alarms.myalarm.tools.IntentCreator;
import com.alarms.myalarm.types.AlarmType;
import com.alarms.myalarm.types.IntentKeys;
import com.bumptech.glide.Glide;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class AlarmDetailsActivity extends AppCompatActivity {

    private TextView dateTimeText;

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYY");

    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private Calendar alarmDateAndTime;

    private Button editAlarmBtn;
    private Button deleteAlarmBtn;
    private AlarmManager alarmManager;
    private int alarmDurationSec;

    private AlarmType alarmType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_details);

        deleteAlarmBtn = findViewById(R.id.deleteAlarmBtn);
        editAlarmBtn = findViewById(R.id.editAlarmBtn);
        dateTimeText = findViewById(R.id.dateTimeText);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            alarmDateAndTime = (Calendar) getIntent().getSerializableExtra(IntentKeys.ALARM_CALENDAR);
            alarmDurationSec = getIntent().getIntExtra(IntentKeys.ALARM_DURATION, 20);
            alarmType = (AlarmType) getIntent().getSerializableExtra((IntentKeys.ALARM_TYPE));
        }

        if (alarmDateAndTime != null) {

            String html = prepareAlarmDetailsText();
            String warning = buildWarningMsg(alarmDateAndTime);

            if (warning != null) {
                String fullMsg = html + "<br>warning  ";
                Spanned spanned = Html.fromHtml(fullMsg);
                SpannableString spannableString = new SpannableString(spanned);
                Drawable icon = getResources().getDrawable(R.drawable.warning_icon);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BOTTOM);
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
                        ds.setColor(ContextCompat.getColor(getApplicationContext(), R.color.light_blue)); // Set text color
                    }
                };
                spannableString.setSpan(clickableSpan, textLength -1, textLength, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                dateTimeText.setMovementMethod(LinkMovementMethod.getInstance());
                dateTimeText.setHighlightColor(Color.TRANSPARENT);
                dateTimeText.setText(spannableString);

            } else {
                dateTimeText.setText(Html.fromHtml(html));
            }

            if (alarmType == AlarmType.MINCHA) {
                editAlarmBtn.setVisibility(View.INVISIBLE);
            } else {
                editAlarmBtn.setVisibility(View.VISIBLE);
            }
            deleteAlarmBtn.setVisibility(View.VISIBLE);
            dateTimeText.setVisibility(View.VISIBLE);

            dateTimeText.setOnClickListener(new View.OnClickListener() {
                int clicksOnEmptyTextView = 0;
                @Override
                public void onClick(View v) {
                    clicksOnEmptyTextView++;

                    if (clicksOnEmptyTextView == 7) {
                        ImageView imageView = findViewById(R.id.imageView);
                        Glide.with(v.getContext()).asGif().load(R.raw.sha_shalom).into(imageView);
                        imageView.setVisibility(View.VISIBLE);
                        Log.d("AnimationClick", "Show animation!!!");
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
        } else {
            throw new RuntimeException("this screen always needs to have 'alarmDateAndTime'");
        }

        editAlarmBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SetAlarmActivity.class);
            i.putExtra(IntentKeys.ALARM_CALENDAR, alarmDateAndTime);
            i.putExtra(IntentKeys.ALARM_DURATION, alarmDurationSec);
            i.putExtra(IntentKeys.ALARM_TYPE, alarmType);
            startActivity(i);
        });

        deleteAlarmBtn.setOnClickListener(v -> {
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            alarmDateAndTime = createCalendarWithDefaultValues();
            //editAlarmBtn.setVisibility(View.INVISIBLE);
            //deleteAlarmBtn.setVisibility(View.INVISIBLE);
            //dateTimeText.setVisibility(View.INVISIBLE);

            PendingIntent pendingIntent = IntentCreator.getAlarmPendingIntent(this,
                    getApplication(), alarmDurationSec, alarmType);

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("ALARM", "Alarm was canceled");
            }
            startActivity(new Intent(this, MainActivity.class));
        });
    }

    private String buildWarningMsg(Calendar alarmDateAndTimeCal) {
        Date alarmDateAndTime = alarmDateAndTimeCal.getTime();

        ZmanimCalendar zcal = new ZmanimCalendar();
        zcal.setCalendar(alarmDateAndTimeCal);
        GeoLocation gl = new GeoLocation("Jerusalem",   31.7683, 35.2137, 800, TimeZone.getTimeZone("Asia/Jerusalem"));
        zcal.setGeoLocation(gl);

        long alarmTime = alarmDateAndTime.getTime();

        long midDay  = zcal.getChatzos().getTime();

        long szGRA = zcal.getSofZmanShmaGRA().getTime();
        long szMGA = zcal.getSofZmanShmaMGA().getTime();
        long szTfilaGRA  = zcal.getSofZmanTfilaGRA().getTime();
        long szTfilaMGA  = zcal.getSofZmanTfilaMGA().getTime();

        Log.d("ALARM",
                " type: " + alarmType + " | " +
                " now: " + timeFormat.format(Calendar.getInstance().getTime()) + " | " +
                " alarm time: " + timeFormat.format(alarmDateAndTime) + " | " +
                " sz Shma GRA: " + timeFormat.format(szGRA) + " | " +
                " sz shma MGA: " + timeFormat.format(szMGA) + " | " +
                " sz tfila GRA: " + timeFormat.format(szTfilaGRA) + " | " +
                " sz tfila MGA: " + timeFormat.format(szTfilaMGA) + " | " +
                " sz Chazot: " + timeFormat.format(midDay));

        if (alarmTime > midDay) {
            return null;
        }
        String html;

        boolean lateForShma = alarmTime > szGRA && alarmTime > szMGA;
        boolean lateForTfila = alarmTime > szTfilaGRA && alarmTime > szTfilaMGA;

        if (!lateForShma && !lateForTfila) {
            return null;
        } else {
            html = "<font size=25 color=" + Color.RED + ">";
        }

        if (lateForShma) {
            html += "Your alarm is after Sof Zman Shma (MGA: "+ timeFormat.format(szMGA) + ", GRA " +
                    timeFormat.format(szGRA) +")<br>";
        }

        if (lateForTfila) {
            html += "Your alarm is after Sof Zman Tfila (MGA: "+ timeFormat.format(szTfilaMGA) + ", GRA " +
                    timeFormat.format(szTfilaGRA) +")";
        }
        html += "</font>";
        return html;
    }

    private AlertDialog createAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!")
                .setMessage(Html.fromHtml(msg))
                .setIcon(R.drawable.warning_icon)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String prepareAlarmDetailsText() {

        Date alarmDate = alarmDateAndTime.getTime();

        JewishDate jd = new JewishDate(alarmDate);
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        String hebrewDate = hdf.format(jd);

        String text = "<font color=" + Color.GRAY + "  size=3 >Alarm will start at: </font><br>" +
                "<font size=30 color=" + Color.BLACK + "><b> " + dateFormat.format(alarmDate) +
                "</b><br><b>" + hebrewDate + "</b>" +
                "</b><br><b>" + timeFormat.format(alarmDate) + "</b>" +
                "</font>";

        if (alarmType == AlarmType.MINCHA) {
            text += "<br><font color=" + Color.GRAY + "  size=3 >15 min. before sunset</font><br>";
        }
        return text;
    }

    private Calendar createCalendarWithDefaultValues() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 10);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        return cal;
    }
}
