package com.banjos.dosalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.WorkManager;

import com.banjos.dosalarm.tools.NotificationJobScheduler;

public class UpgradeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                Log.d("UpgradeReceiver", "UpgradeReceiver was called");
                if (intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
                   Log.d("UpgradeReceiver", "app was upgraded | intent.getAction() = " + intent.getAction());
                   //we don't want the job to run twice
                   WorkManager.getInstance(context).cancelAllWorkByTag(NotificationJobScheduler.WORKER_TAG);
                   NotificationJobScheduler.scheduleDailyNotificationsJob(context);
                }
        }
}
