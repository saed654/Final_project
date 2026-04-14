package com.example.final_projectsss.molds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.final_projectsss.MainActivity;

import java.util.HashSet;
import java.util.Set;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BOOT_DEBUG";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (context == null) {
                Log.e(TAG, "Context is null in BootReceiver");
                return;
            }

            String action = intent != null ? intent.getAction() : null;
            Log.d(TAG, "Receiver fired. action = " + action);

            if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                return;
            }

            NotificationHelper.createNotificationChannel((MainActivity) context);

            Set<String> dates = ReminderStorage.getAllReminders(context);
            Log.d(TAG, "Saved reminder dates count = " + dates.size());
            for (String item : dates) {
                if (item == null || !item.contains("|")) continue;

                String[] parts = item.split("\\|", 2);
                if (parts.length != 2) continue;

                String email = parts[0];
                String date = parts[1];

                if (ReminderScheduler.isReminderValid(date)) {
                    Log.d(TAG, "Rescheduling valid reminder: " + email + " | " + date);
                    ReminderScheduler.scheduleReminder(context, date, true,"0");
                } else {
                    Log.d(TAG, "Expired reminder found after boot: " + email + " | " + date);

                    SharedPreferences prefs = context.getSharedPreferences("reminder_pref", Context.MODE_PRIVATE);
                    Set<String> current = new HashSet<>(prefs.getStringSet("saved_dates", new HashSet<>()));
                    current.remove(item);
                    prefs.edit().putStringSet("saved_dates", current).apply();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "BootReceiver crashed while restoring reminders", e);
        }
    }
}