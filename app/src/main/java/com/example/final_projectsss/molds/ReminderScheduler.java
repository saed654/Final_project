package com.example.final_projectsss.molds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

public class ReminderScheduler {
    private static final String TAG = "REMINDER_DEBUG";

    // Change to true only for testing
    private static final boolean TEST_MODE = true;

    /**
     * Schedules only one reminder per date.
     * Production mode: day before at 9:00 AM.
     * Test mode: 30 seconds from now.
     *
     * isReschedule = true when called from BootReceiver
     * isReschedule = false when called after a new booking
     */
    public static void scheduleReminder(Context context, String documentId, boolean isReschedule) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        if (!isValidDocumentId(documentId)) {
            Log.e(TAG, "Invalid documentId format: " + documentId);
            return;
        }

        try {
            Log.d(TAG, "Trying to schedule for: " + documentId + " | reschedule=" + isReschedule);

            Set<String> saved = ReminderStorage.getAllReminders(context);

            if (!isReschedule && saved.contains(documentId)) {
                Log.d(TAG, "Reminder already exists for date: " + documentId);
                return;
            }

            Calendar calendar = buildReminderCalendar(documentId);

            if (calendar == null) {
                Log.e(TAG, "Failed to build reminder calendar for: " + documentId);
                return;
            }

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                Log.d(TAG, "Reminder time already passed for: " + documentId);
                ReminderStorage.removeReminder(context, documentId);
                return;
            }

            // Save locally so we can restore after reboot
            if (!saved.contains(documentId)) {
                ReminderStorage.saveReminder(context, documentId);
                Log.d(TAG, "Saved reminder date: " + documentId);
            }

            // Create the intent for the Receiver
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("title", "Appointment Reminder");
            intent.putExtra("message", "You have an appointment on " + documentId);
            intent.putExtra("notification_id", documentId.hashCode());


            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    documentId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            long triggerAtMillis = calendar.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }

            Log.d(TAG, "Reminder scheduled successfully for: " + documentId );

        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder for: " + documentId, e);
        }
    }

    /**
     * Cancels a scheduled reminder for a specific date.
     */
    public static void cancelReminder(Context context, String documentId) {
        if (context == null || !isValidDocumentId(documentId)) {
            return;
        }

        try {
            Intent intent = new Intent(context, ReminderReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    documentId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Reminder cancelled for: " + documentId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel reminder for: " + documentId, e);
        }
    }

    public static boolean isReminderValid(String documentId) {
        if (!isValidDocumentId(documentId)) {
            return false;
        }

        try {
            Calendar reminderTime = buildReminderCalendar(documentId);
            return reminderTime != null && reminderTime.getTimeInMillis() > System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "isReminderValid failed for: " + documentId, e);
            return false;
        }
    }

    private static Calendar buildReminderCalendar(String documentId) {
        try {
            if (TEST_MODE) {
                Calendar testCalendar = Calendar.getInstance();
                testCalendar.add(Calendar.SECOND, 30);
                return testCalendar;
            }

            String[] parts = documentId.split("-");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // reminder one day before
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            return calendar;

        } catch (Exception e) {
            Log.e(TAG, "buildReminderCalendar failed for: " + documentId, e);
            return null;
        }
    }

    private static boolean isValidDocumentId(String documentId) {
        if (documentId == null) return false;
        return documentId.matches("\\d{1,2}-\\d{1,2}-\\d{4}");
    }
}
