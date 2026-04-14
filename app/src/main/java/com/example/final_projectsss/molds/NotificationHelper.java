package com.example.final_projectsss.molds;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class NotificationHelper {

    public static final String CHANNEL_ID = "appointment_reminders";
    public static final String CHANNEL_NAME = "Appointment Reminders";

    private static final String TAG = "NOTIFICATION_HELPER";

    public static void createNotificationChannel(Context context) {
        if (context == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

                channel.setDescription("Reminders for booked appointments and order alerts");

                NotificationManager manager =
                        context.getSystemService(NotificationManager.class);

                if (manager != null) {
                    manager.createNotificationChannel(channel);
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification channel", e);
        }
    }
}