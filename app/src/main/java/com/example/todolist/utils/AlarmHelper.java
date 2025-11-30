package com.example.todolist.utils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import com.example.todolist.models.TodoItem;
import com.example.todolist.receiver.AlarmReceiver;

public class AlarmHelper {

    private static final String TAG = "AlarmHelper";

    public static void setAlarm(Context context, TodoItem todoItem) {
        Log.d(TAG, "Setting alarm for task: " + todoItem.getTask());

        if (todoItem.calculateAlarmTime() == 0) {
            Log.e(TAG, "Cannot set alarm: Invalid date/time");
            Toast.makeText(context, "Cannot set alarm: Invalid date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        long alarmTime = todoItem.calculateAlarmTime();
        Log.d(TAG, "Alarm time: " + new java.util.Date(alarmTime));

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null!");
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("task_id", todoItem.getId());
        intent.putExtra("task_title", todoItem.getTask());
        intent.putExtra("task_description", todoItem.getDescription());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                todoItem.getId(),
                intent,
                flags
        );

        try {
            // Handle different Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - Check permission first
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Alarm set with exact permission");
                } else {
                    // Fallback: Use inexact alarm or show permission dialog
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Alarm set with inexact fallback");

                    // Show permission dialog for next time
                    Toast.makeText(context,
                            "Alarm set (inexact). Enable exact alarms for better accuracy",
                            Toast.LENGTH_LONG).show();
                }
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                Log.d(TAG, "Alarm set exact for Android 6-11");
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4-5.1
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                Log.d(TAG, "Alarm set exact for Android 4.4-5.1");
            }
            else {
                // Android < 4.4
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                Log.d(TAG, "Alarm set for Android < 4.4");
            }

            todoItem.setAlarmTime(alarmTime);
            String message = "Reminder set for " + formatTimeForDisplay(todoItem.getDueTime());
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Alarm scheduled successfully");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
            handleAlarmPermissionError(context);
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm: " + e.getMessage());
            Toast.makeText(context, "Failed to set reminder", Toast.LENGTH_SHORT).show();
        }
    }

    private static void handleAlarmPermissionError(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Show dialog to request permission
            PermissionHelper.showExactAlarmPermissionDialog(context);
        } else {
            Toast.makeText(context,
                    "Alarm permission denied. Please check app permissions",
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void cancelAlarm(Context context, int taskId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, AlarmReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId,
                    intent,
                    flags
            );

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Alarm cancelled for task: " + taskId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm: " + e.getMessage());
        }
    }

    public static void updateAlarm(Context context, TodoItem todoItem) {
        cancelAlarm(context, todoItem.getId());
        if (todoItem.hasReminder() && todoItem.hasDueDate() && todoItem.hasDueTime()) {
            setAlarm(context, todoItem);
        }
    }

    private static String formatTimeForDisplay(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            String period = "AM";
            int displayHour = hour;

            if (hour >= 12) {
                period = "PM";
                if (hour > 12) displayHour = hour - 12;
            }
            if (hour == 0) displayHour = 12;

            return String.format("%d:%02d %s", displayHour, minute, period);
        } catch (Exception e) {
            return time;
        }
    }
}