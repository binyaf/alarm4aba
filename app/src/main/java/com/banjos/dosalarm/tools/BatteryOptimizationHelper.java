package com.banjos.dosalarm.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

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
                .setTitle("Battery Optimization")
                .setMessage("For reliable alarm scheduling, please disable battery optimization for this app. " +
                        "Without this, alarms may not trigger reliably on long delays.")
                .setPositiveButton("Disable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        // If the system cannot handle this intent, open battery settings
                        context.startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(true);
        
        try {
            builder.show();
        } catch (Exception e) {
            // Activity might not be available, silently fail
        }
    }
}
