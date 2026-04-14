package com.example.final_projectsss.log_in;

import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.final_projectsss.R;
import com.example.final_projectsss.molds.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class Manager_fragment extends Fragment {

    private static final String TAG = "MANAGER_FRAGMENT";

    EditText email;
    Spinner roleSpinner;
    Button setRole, disableBtn, enableBtn;

    FirebaseAuth auth;
    FirebaseFirestore db;

    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_manager_dash, container, false);

        email = rootView.findViewById(R.id.inputEmail);
        roleSpinner = rootView.findViewById(R.id.roleSpinner);
        setRole = rootView.findViewById(R.id.setRole);
        disableBtn = rootView.findViewById(R.id.btnDisable);
        enableBtn = rootView.findViewById(R.id.btnEnable);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        roleSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("user", "manager")
        ));

        setRole.setOnClickListener(v -> setRole());
        disableBtn.setOnClickListener(v -> setActive(false));
        enableBtn.setOnClickListener(v -> setActive(true));

        return rootView;
    }
    private void setRole() {
        setRole.setEnabled(false);
        String e = email.getText().toString().trim().toLowerCase();
        String role = roleSpinner.getSelectedItem().toString();

        if (e.isEmpty()) {
            showSnack("Enter email");
            return;
        }

        db.collection("users")
                .whereEqualTo("email", e)
                .get()
                .addOnSuccessListener(q -> {

                    if (q.isEmpty()) {
                        showSnack("User not found");
                        Log.w(TAG, "User not found: " + e);
                        return;
                    }

                    if (q.size() > 1) {
                        showSnack("More than one user has this email");
                        Log.w(TAG, "Duplicate email in Firestore: " + e);
                        return;
                    }

                    q.getDocuments().forEach(doc -> {

                        String currentRole = doc.getString("role");

                        if (role.equals(currentRole)) {
                            showSnack("User already has this role");
                            Log.d(TAG, "Role unchanged for " + e + ": " + role);
                            return;
                        }

                        if ("manager".equals(currentRole) && "user".equals(role)) {
                            showSnack("Cannot downgrade manager");
                            Log.w(TAG, "Attempt to downgrade manager: " + e);
                            return;
                        }

                        doc.getReference()
                                .update("role", role)
                                .addOnSuccessListener(unused -> {
                                    showSnack("Role updated to " + role);
                                    Log.d(TAG, "Role updated for " + e + " to " + role);
                                })
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Failed to update role for " + e, err);
                                    showSnack("Failed to update role");
                                });
                    });
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Role search failed", err);
                    showSnack("Something went wrong");
                });
        setRole.setEnabled(true);
    }

    private void setActive(boolean active) {
        disableBtn.setEnabled(!active);
        enableBtn.setEnabled(active);
        String e = email.getText().toString().trim();

        if (e.isEmpty()) {
            showSnack("Enter email");
            return;
        }

        db.collection("users")
                .whereEqualTo("email", e)
                .get()
                .addOnSuccessListener(q -> {

                    if (q.isEmpty()) {
                        showSnack("User not found");
                        Log.w(TAG, "User not found: " + e);
                        return;
                    }

                    q.getDocuments().forEach(doc -> {

                        if ("manager".equals(doc.getString("role"))) {
                            showSnack("Cannot change manager status");
                            Log.w(TAG, "Attempt to modify manager");
                        } else {
                            doc.getReference().update("active", active).addOnSuccessListener(unused -> {
                                showSnack(active ? "User enabled" : "User disabled");
                                Log.d(TAG, "User " + e + " active=" + active);
                            }).addOnFailureListener(err -> {
                                Log.e(TAG, "Status change failed", err);
                                showSnack(err.getMessage());
                            });
                        }
                    });
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Status change failed", err);
                    showSnack(err.getMessage());
                });
        disableBtn.setEnabled(true);
        enableBtn.setEnabled(true);

    }

    private void showSnack(String msg) {
        if (rootView != null) {
            Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG).show();
        }
    }
}
