package com.example.todolist.receiver;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "todo_alarm_channel";
    private static final String CHANNEL_NAME = "To-Do Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered!");

        int taskId = intent.getIntExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");
        String taskDescription = intent.getStringExtra("task_description");

        Log.d(TAG, "Received alarm for task ID: " + taskId + ", Title: " + taskTitle);

        if (taskTitle == null) {
            taskTitle = "Task Reminder";
            Log.w(TAG, "Task title was null, using default");
        }

        showNotification(context, taskId, taskTitle, taskDescription);
    }

    private void showNotification(Context context, int taskId, String title, String description) {
        Log.d(TAG, "Showing notification for: " + title);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null!");
            return;
        }

        // Create notification channel for Android O+
        createNotificationChannel(notificationManager);

        // Create intent to open app
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                appIntent,
                flags
        );

        // Notification sound
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Log.d(TAG, "Using notification sound (alarm sound was null)");
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🔔 " + title)
                .setContentText(description != null ? description : "Task due now!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(description != null ? description : "Your task is due now. Don't forget to complete it!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setLights(Color.RED, 1000, 1000);

        // Show notification
        notificationManager.notify(taskId, builder.build());
        Log.d(TAG, "Notification displayed successfully");
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for to-do task reminders");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }
}