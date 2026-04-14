package com.example.final_projectsss.molds;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.final_projectsss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OrderAlertHelper {

    private static final String TAG = "ORDER_ALERT_HELPER";

    public static void checkAndShowReadyNotifications(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "No logged-in user, skipping alerts");
            return;
        }
        Log.d(TAG, "Checking for ready notifications");
        NotificationHelper.createNotificationChannel(context);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("alerts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String type = doc.getString("type");
                            String message = doc.getString("message");

                            if (message == null || message.trim().isEmpty()) {
                                message = "You have a new update.";
                            }

                            String title;
                            if ("appointment_cancelled".equals(type)) {
                                title = "Appointment Cancelled";
                            } else if ("order_ready".equals(type)) {
                                title = "Order Ready";
                            } else {
                                title = "Store Notification";
                            }

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                                    context,
                                    NotificationHelper.CHANNEL_ID
                            )
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle(title)
                                    .setContentText(message)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true);

                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED) {

                                int notificationId = doc.getId().hashCode();

                                NotificationManagerCompat.from(context)
                                        .notify(notificationId, builder.build());

                                // Delete only after successful display attempt
                                doc.getReference().delete()
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Failed to delete shown alert: " + doc.getId(), e));
                            } else {
                                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Failed to process alert doc: " + doc.getId(), e);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to load alerts from Firestore", e));
    }
}