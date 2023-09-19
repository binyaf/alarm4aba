package com.alarms.myalarm.activity;

import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;

import com.alarms.myalarm.R;

public class TopBarActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upper_bar, menu);
        return true;
    }
}

