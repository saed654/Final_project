package com.example.final_projectsss.molds;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.final_projectsss.MainActivity;

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
            for (String date : dates) {
                if (ReminderScheduler.isReminderValid(date)) {
                    Log.d(TAG, "Rescheduling valid reminder: " + date);
                    ReminderScheduler.scheduleReminder(context, date, true);
                } else {
                    Log.d(TAG, "Removing expired reminder after boot: " + date);
                    ReminderStorage.removeReminder(context, date);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "BootReceiver crashed while restoring reminders", e);
        }
    }
}