package com.example.final_projectsss.molds;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class ReminderStorage {

    private static final String PREF_NAME = "reminder_pref";
    private static final String KEY_DATES = "saved_dates";
    private static final String TAG = "REMINDER_STORAGE";

    /**
     * Save a reminder date locally.
     */
    public static void saveReminder(Context context, String date) {
        if (context == null || date == null || date.trim().isEmpty()) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> current = new HashSet<>(prefs.getStringSet(KEY_DATES, new HashSet<>()));
            current.add(date);
            prefs.edit().putStringSet(KEY_DATES, current).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save reminder: " + date, e);
        }
    }

    /**
     * Get all saved reminder dates.
     */
    public static Set<String> getAllReminders(Context context) {
        if (context == null) return new HashSet<>();

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return new HashSet<>(prefs.getStringSet(KEY_DATES, new HashSet<>()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to get reminders", e);
            return new HashSet<>();
        }
    }

    /**
     * Remove a reminder date locally and cancel its alarm.
     */
    public static void removeReminder(Context context, String date) {
        if (context == null || date == null || date.trim().isEmpty()) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> current = new HashSet<>(prefs.getStringSet(KEY_DATES, new HashSet<>()));
            current.remove(date);
            prefs.edit().putStringSet(KEY_DATES, current).apply();

            ReminderScheduler.cancelReminder(context, date);

        } catch (Exception e) {
            Log.e(TAG, "Failed to remove reminder: " + date, e);
        }
    }

    /**
     * Clears ALL saved reminders from local storage and cancels them in the system.
     */
    public static void clearAllReminders(Context context) {
        if (context == null) return;

        try {
            Set<String> all = getAllReminders(context);
            for (String date : all) {
                ReminderScheduler.cancelReminder(context, date);
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_DATES).apply();

        } catch (Exception e) {
            Log.e(TAG, "Failed to clear all reminders", e);
        }
    }

    /**
     * After cancel/delete, check whether THIS USER still has any booked slots on this date.
     * If not, remove the reminder for that date.
     */
    public static void removeReminderIfNoBookingsLeft(Context context, String dateId, FirebaseFirestore db) {
        if (context == null || db == null || dateId == null || dateId.trim().isEmpty()) {
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            removeReminder(context, dateId);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("dor")
                .document(dateId)
                .collection("slots")
                .whereEqualTo("isBooked", true)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        removeReminder(context, dateId);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed checking bookings left for date: " + dateId, e));
    }
}