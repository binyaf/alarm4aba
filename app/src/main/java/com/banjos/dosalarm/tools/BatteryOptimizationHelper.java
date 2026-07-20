package com.banjos.dosalarm.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

public class BatteryOptimizationHelper {

    public static void promptDisableBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog(context, packageName);
            }
        }
    }

    private static void showBatteryOptimizationDialog(Context context, String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("CRITICAL: Battery Optimization")
                .setMessage("For alarms to work reliably 24+ hours in advance while idle, " +
                        "you MUST disable battery optimization for this app. " +
                        "Without this step, alarms WILL NOT FIRE.\n\n" +
                        "Tap \"Disable Now\" to proceed.")
                .setPositiveButton("Disable Now", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    try {
                        context.startActivity(intent);
                        Log.d("BatteryOptimization", "Opened battery optimization dialog");
                    } catch (Exception e) {
                        Log.e("BatteryOptimization", "Failed to open battery optimization", e);
                        try {
                            context.startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
                        } catch (Exception e2) {
                            Log.e("BatteryOptimization", "Failed to open battery settings", e2);
                        }
                    }
                })
                .setCancelable(false); // Force user to make a choice
        
        try {
            builder.show();
        } catch (Exception e) {
            Log.e("BatteryOptimization", "Could not show dialog", e);
        }
    }
}

