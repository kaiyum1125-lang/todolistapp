package com.example.todolist.utils;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class PermissionHelper {

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Below Android 12, no special permission needed
    }

    public static void showExactAlarmPermissionDialog(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Exact Alarm Permission Required")
                    .setMessage("This app needs exact alarm permission to remind you of tasks at specific times. " +
                            "Please grant this permission in the next screen.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        openAlarmPermissionSettings(context);
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private static void openAlarmPermissionSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback if the specific action is not available
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);

            Toast.makeText(context, "Please enable 'Alarms & reminders' permission manually", Toast.LENGTH_LONG).show();
        }
    }
}