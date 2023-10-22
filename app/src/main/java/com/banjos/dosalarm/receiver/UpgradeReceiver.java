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
                String packageName = intent.getData().getSchemeSpecificPart();
                //we want it to be called only when this app is been upgraded
                if (packageName.equals(context.getPackageName())) {
                        Log.d("UpgradeReceiver", "app was upgraded");
                        //we don't want the job to run twice
                        WorkManager.getInstance(context).cancelAllWorkByTag(NotificationJobScheduler.WORKER_TAG);
                        NotificationJobScheduler.scheduleDailyNotificationsJob(context);
                }
        }
}
