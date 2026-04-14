package com.example.final_projectsss.molds;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.final_projectsss.R;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "REMINDER_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (context == null) {
                Log.e(TAG, "Context is null");
                return;
            }

            NotificationHelper.createNotificationChannel(context);

            String title = intent != null ? intent.getStringExtra("title") : null;
            String message = intent != null ? intent.getStringExtra("message") : null;
            int notificationId = intent != null
                    ? intent.getIntExtra("notification_id", 1000)
                    : 1000;
            String email = intent != null ? intent.getStringExtra("email") : null;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    context,
                    NotificationHelper.CHANNEL_ID
            )
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title == null ? "Appointment Reminder" : title)
                    .setContentText(message == null ? email+"You have an appointment tomorrow." : message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                return;
            }

            NotificationManagerCompat.from(context).notify(notificationId, builder.build());

        } catch (Exception e) {
            Log.e(TAG, "ReminderReceiver crashed", e);
        }
    }
}