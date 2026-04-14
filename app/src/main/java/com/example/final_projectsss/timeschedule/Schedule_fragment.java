package com.example.final_projectsss.timeschedule;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.final_projectsss.R;
import com.example.final_projectsss.molds.NotificationHelper;
import com.example.final_projectsss.molds.SlotModel;
import com.example.final_projectsss.molds.TimeSlotAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedule_fragment extends Fragment {

    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    RecyclerView recyclerView;
    Button selectDateBtn;
    FloatingActionButton addButton;
    TextView selectedDateText;

    String currentDateId = "";
    boolean isManager = false;
    public Schedule_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schedule_fragment, container, false);

        addButton = view.findViewById(R.id.add_date);

        recyclerView = view.findViewById(R.id.timeRecyclerView);
        selectDateBtn = view.findViewById(R.id.selectDateBtn);
        selectedDateText = view.findViewById(R.id.selectedDateText);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        NotificationHelper.createNotificationChannel(requireContext());
        requestNotificationPermission();


        // Load current user role
        loadCurrentUserRole();

        // Select date
        selectDateBtn.setOnClickListener(v -> openDatePicker());

        addButton.setOnClickListener(v -> {

            if (currentDateId == null || currentDateId.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Select a date first",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showAddSlotDialog(currentDateId);
        });

        return view;
    }

    /**
     * Loads current user role from Firestore.
     */
    private void loadCurrentUserRole() {
        if (auth.getCurrentUser() == null) {
            isManager = false;
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if ("manager".equals(role)) {
                            isManager = true;
                            addButton.setVisibility(View.VISIBLE);
                        } else {
                            isManager = false;
                            addButton.setVisibility(View.GONE);
                        }
                    } else {
                        isManager = false;
                    }
                })
                .addOnFailureListener(e -> isManager = false);

    }

    /**
     * Opens a date picker.
     * Prevents selecting past dates.
     */
    private void openDatePicker() {
        Calendar now = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {

                    // Build selected date calendar
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selected.set(Calendar.HOUR_OF_DAY, 0);
                    selected.set(Calendar.MINUTE, 0);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    // Build today's start (00:00:00)
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    // Prevent past date
                    if (selected.before(today)) {
                        Toast.makeText(requireContext(),
                                "This date has already passed",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Month from DatePicker is 0-based, for display/doc id use +1
                    int realMonth = month + 1;

                    currentDateId = dayOfMonth + "-" + realMonth + "-" + year;
                    selectedDateText.setText("Selected: " + currentDateId);

                    loadOrCreateDay(currentDateId);
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        // Also block past dates directly in the picker
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        dialog.show();
    }

    /**
     * Ensures the day document exists.
     */
    private void loadOrCreateDay(String documentId) {
        DocumentReference dayRef = db.collection("dor").document(documentId);

        dayRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                ensureSlotsExistAndLoad(documentId);
            } else {
                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("date", documentId);

                dayRef.set(dayMap)
                        .addOnSuccessListener(unused -> ensureSlotsExistAndLoad(documentId))
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(),
                                        "Failed to create day",
                                        Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(),
                        "Failed to load day",
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * If no slot documents exist for this day, create default slots.
     */
    private void ensureSlotsExistAndLoad(String documentId) {
        db.collection("dor")
                .document(documentId)
                .collection("slots")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        createDefaultSlots(documentId);
                    } else {
                        loadSlots(documentId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to check slots",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Creates default half-hour slots from 09:00 to 17:00.
     */
    private void createDefaultSlots(String documentId) {



        List<String> defaultTimes = new ArrayList<>();
        defaultTimes.add("09:00");
        defaultTimes.add("09:30");
        defaultTimes.add("10:00");
        defaultTimes.add("10:30");
        defaultTimes.add("11:00");
        defaultTimes.add("11:30");
        defaultTimes.add("12:00");
        defaultTimes.add("12:30");
        defaultTimes.add("13:00");
        defaultTimes.add("13:30");
        defaultTimes.add("14:00");
        defaultTimes.add("14:30");
        defaultTimes.add("15:00");
        defaultTimes.add("15:30");
        defaultTimes.add("16:00");
        defaultTimes.add("16:30");
        defaultTimes.add("17:00");

        for (String time : defaultTimes) {
            Map<String, Object> slotMap = new HashMap<>();
            int hour = Integer.parseInt(time.split(":")[0]);
            int minute = Integer.parseInt(time.split(":")[1]);
            int timeValue = hour * 60 + minute;
            slotMap.put("time", time);
            slotMap.put("timeValue", timeValue);
            slotMap.put("isBooked", false);
            slotMap.put("userId", "");
            slotMap.put("userEmail", "");

            db.collection("dor")
                    .document(documentId)
                    .collection("slots")
                    .add(slotMap);
        }

        loadSlots(documentId);
    }

    /**
     * Loads slot documents into SlotModel list.
     */
    private void loadSlots(String documentId) {
        db.collection("dor")
                .document(documentId)
                .collection("slots")
                .orderBy("timeValue")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SlotModel> slotList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        SlotModel slot = new SlotModel();

                        slot.id = doc.getId();
                        slot.time = doc.getString("time");

                        Boolean booked = doc.getBoolean("isBooked");
                        slot.isBooked = booked != null && booked;

                        String userId = doc.getString("userId");
                        String userEmail = doc.getString("userEmail");

                        slot.userId = userId == null ? "" : userId;
                        slot.userEmail = userEmail == null ? "" : userEmail;

                        slotList.add(slot);
                    }

                    showSlots(slotList, documentId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load slots",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows the slot list in RecyclerView.
     */
    private void showSlots(List<SlotModel> slots, String documentId) {
        TimeSlotAdapter adapter = new TimeSlotAdapter(slots, documentId, isManager);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Manager adds new slot by dialog.
     */
    private void showAddSlotDialog(String dateId) {

        EditText input = new EditText(requireContext());
        input.setHint("Enter time like 09:00");

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Slot")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {

                    String raw = input.getText().toString().trim();
                    String time = formatTime(raw);

                    if (time == null) {
                        Toast.makeText(requireContext(),
                                "Invalid time format",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!time.matches("\\d{2}:\\d{2}")) {
                        Toast.makeText(requireContext(),
                                "Invalid format (HH:MM)",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int hour = Integer.parseInt(time.split(":")[0]);
                    int minute = Integer.parseInt(time.split(":")[1]);

                    int timeValue = hour * 60 + minute;

                    // 🔴 CHECK DUPLICATE
                    db.collection("dor")
                            .document(dateId)
                            .collection("slots")
                            .whereEqualTo("timeValue", timeValue)
                            .get()
                            .addOnSuccessListener(query -> {

                                if (!query.isEmpty()) {
                                    Toast.makeText(requireContext(),
                                            "Slot already exists",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // 🟢 ADD SLOT
                                Map<String, Object> map = new HashMap<>();
                                map.put("time", time);
                                map.put("timeValue", timeValue);
                                map.put("isBooked", false);
                                map.put("userId", "");
                                map.put("userEmail", "");

                                db.collection("dor")
                                        .document(dateId)
                                        .collection("slots")
                                        .add(map)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(requireContext(),
                                                    "Slot added",
                                                    Toast.LENGTH_SHORT).show();
                                            loadSlots(dateId);
                                        });
                            });

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Requests notification permission on Android 13+.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        2001
                );
            }
        }
    }

    //valid time input
    private String formatTime(String input) {

        if (input == null || !input.contains(":")) return null;

        try {
            String[] parts = input.split(":");

            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());

            // ❌ invalid values
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }

            // 🔥 format to HH:MM
            return String.format("%02d:%02d", hour, minute);

        } catch (Exception e) {
            return null;
        }
    }
}