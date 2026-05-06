package com.example.final_projectsss.molds;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.VH> {
    private boolean isProcessing = false;
    List<SlotModel> list;
    String dateId;
    boolean isManager;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public TimeSlotAdapter(List<SlotModel> list, String dateId, boolean isManager) {
        this.list = list;
        this.dateId = dateId;
        this.isManager = isManager;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.time_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SlotModel s = list.get(pos);

        // User view
        if (!isManager) {
            h.txt.setText(s.time);

            if (s.isBooked) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null
                        && s.userId != null
                        && s.userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    h.txt.setBackgroundColor(Color.parseColor("#FF9800")); // my booking
                } else {
                    h.txt.setBackgroundColor(Color.RED); // booked by someone else
                }
            } else {
                h.txt.setBackgroundColor(Color.GRAY); // free
            }
        } else {
            // Manager view
            if (s.isBooked) {
                h.txt.setText(s.time + "\nBooked by:\n" + s.userEmail);
                h.txt.setBackgroundColor(Color.RED);
            } else {
                h.txt.setText(s.time + "\nAvailable");
                h.txt.setBackgroundColor(Color.GRAY);
            }
        }

        // Click = booking for normal user only on free slot
        h.txt.setOnClickListener(v -> {
            if (!isManager && !s.isBooked) {
                if(isProcessing) return;
                bookSlot(s, v.getContext());
            }
        });

        // Long click
        h.txt.setOnLongClickListener(v -> {
            if (isManager) {
                showManagerDialog(s, v.getContext());
            } else {
                cancelIfOwner(s, v.getContext());
            }
            return true;
        });
    }

    /**
     * Normal user can cancel only his own reservation.
     */
    private void cancelIfOwner(SlotModel s, Context c) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(s.userId==""){
            Toast.makeText(c, "No appointment to cancel", Toast.LENGTH_SHORT).show();
            return;
        }

        if (s.userId == null || !s.userId.equals(uid)) {
            Toast.makeText(c, "You can only cancel your own appointment", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(c)
                .setTitle("Cancel Appointment")
                .setMessage("Do you want to cancel your appointment?")
                .setPositiveButton("Yes", (d, w) -> {

                    DocumentReference ref = db.collection("dor")
                            .document(dateId)
                            .collection("slots")
                            .document(s.id);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot snap = transaction.get(ref);

                        String bookedUserId = snap.getString("userId");
                        if (bookedUserId == null || !bookedUserId.equals(uid)) {
                            throw new FirebaseFirestoreException(
                                    "This slot is not yours anymore",
                                    FirebaseFirestoreException.Code.ABORTED
                            );
                        }

                        transaction.update(ref,
                                "isBooked", false,
                                "userId", "",
                                "userEmail", "");

                        return null;
                    }).addOnSuccessListener(unused -> {
                        s.isBooked = false;
                        s.userId = "";
                        s.userEmail = "";
                        notifyDataSetChanged();

                        // Remove reminder date if user no longer has any bookings on that same day
                        ReminderStorage.removeReminderIfNoBookingsLeft(c, dateId, db);

                        Toast.makeText(c, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e ->
                            Toast.makeText(c, "Failed to cancel", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Manager can cancel booking or delete slot.
     */
    private void showManagerDialog(SlotModel s, Context c) {
        String[] options = {"Cancel Booking", "Delete Slot"};

        new AlertDialog.Builder(c)
                .setTitle("Manager Actions")
                .setItems(options, (d, which) -> {

                    if (which == 0) {
                        ownercanceling(s, c,false);
                    }

                    if (which == 1) {
                        ownercanceling(s,c,true);
                        // Delete slot
                        db.collection("dor")
                                .document(dateId)
                                .collection("slots")
                                .document(s.id)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    int position = list.indexOf(s);
                                    if (position != -1) {
                                        list.remove(position);
                                        notifyItemRemoved(position);
                                    }

                                    Toast.makeText(c, "Slot deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(c, "Failed to delete slot", Toast.LENGTH_SHORT).show());
                    }

                })
                .show();
    }

    public void ownercanceling(SlotModel s, Context c,boolean delete){
        {
            // Cancel booking

            String oldUserId = s.userId;
            String oldUserEmail = s.userEmail;
            String oldTime = s.time;

            if(oldUserId==null|| oldUserId.isEmpty()){
                if(!delete)
                Toast.makeText(c, "No booking to cancel", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("dor")
                    .document(dateId)
                    .collection("slots")
                    .document(s.id)
                    .update("isBooked", false,
                            "userId", "",
                            "userEmail", "")
                    .addOnSuccessListener(unused -> {

                        // send alert to user if this slot belonged to someone
                        if (oldUserId != null && !oldUserId.isEmpty()) {
                            Map<String, Object> alert = new HashMap<>();
                            alert.put("type", "appointment_cancelled");
                            alert.put("message", "Your appointment on " + dateId + " at " + oldTime + " was cancelled.");
                            alert.put("createdAt", FieldValue.serverTimestamp());

                            db.collection("users")
                                    .document(oldUserId)
                                    .collection("alerts")
                                    .add(alert);
                            Toast.makeText(c, "Booking cancelled", Toast.LENGTH_SHORT).show();
                        }

                        s.isBooked = false;
                        s.userId = "";
                        s.userEmail = "";
                        notifyDataSetChanged();

                        ReminderStorage.removeReminderIfNoBookingsLeft(c, dateId, db);


                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(c, "Failed to cancel booking", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Book slot safely with transaction.
     * Also schedules only one reminder per date.
     */
    private void bookSlot(SlotModel s, Context c) {
          isProcessing=true;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            isProcessing=false;
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // 🔴 STEP 1: CHECK USER BOOKINGS FOR THIS DAY
        db.collection("dor")
                .document(dateId)
                .collection("slots")
                .get()
                .addOnSuccessListener(query -> {

                    int userCount = 0;
                    int existingIndex = -1;

                    for (int i = 0; i < list.size(); i++) {
                        SlotModel slot = list.get(i);

                        if (uid.equals(slot.userId)) {
                            userCount++;
                            existingIndex = i;
                        }
                    }

                    int currentIndex = list.indexOf(s);

                    // ❌ RULE 1: MAX 2 BOOKINGS
                    if (userCount >= 2) {
                        isProcessing=false;
                        Toast.makeText(c,
                                "You can only book 2 consecutive slots",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ❌ RULE 2: MUST BE CONSECUTIVE
                    if (userCount == 1) {
                        if (Math.abs(currentIndex - existingIndex) != 1) {
                            isProcessing=false;
                            Toast.makeText(c,
                                    "Slots must be consecutive (e.g. 09:00 + 09:30)",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // 🟢 STEP 2: SAFE BOOKING (TRANSACTION)
                    DocumentReference ref = db.collection("dor")
                            .document(dateId)
                            .collection("slots")
                            .document(s.id);

                    db.runTransaction(transaction -> {

                        DocumentSnapshot snap = transaction.get(ref);

                        Boolean isBookedNow = snap.getBoolean("isBooked");
                        if (isBookedNow != null && isBookedNow) {
                            throw new FirebaseFirestoreException(
                                    "Already booked",
                                    FirebaseFirestoreException.Code.ABORTED
                            );
                        }

                        transaction.update(ref,
                                "isBooked", true,
                                "userId", uid,
                                "userEmail", email == null ? "" : email);
                        isProcessing=false;
                        return null;

                    }).addOnSuccessListener(v -> {

                        s.isBooked = true;
                        s.userId = uid;
                        s.userEmail = email == null ? "" : email;

                        notifyDataSetChanged();

                        // 🔔 Reminder logic (only once per day)
                        if (ReminderScheduler.isReminderValid(dateId)) {
                            ReminderScheduler.scheduleReminder(c, dateId, false);
                        }
                        isProcessing=false;
                        Toast.makeText(c, "Booked!", Toast.LENGTH_SHORT).show();

                    }).addOnFailureListener(e ->
                    {
                            Toast.makeText(c,
                                    "Slot already taken",
                                    Toast.LENGTH_SHORT).show();
                        isProcessing=false;});
                });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txt;

        public VH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.timeText);
        }
    }
}