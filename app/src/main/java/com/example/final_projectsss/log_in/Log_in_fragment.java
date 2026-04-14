package com.example.final_projectsss.log_in;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.final_projectsss.MainActivity;
import com.example.final_projectsss.R;
import com.example.final_projectsss.molds.User;
import com.example.final_projectsss.products.UserProductsFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Log_in_fragment extends Fragment {

    private static final String TAG = "LOGIN_FRAGMENT";

    EditText email, password;
    Button login, signup;

    FirebaseAuth auth;
    FirebaseFirestore db;

    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_log_in_fragment, container, false);

        email = rootView.findViewById(R.id.mail);
        password = rootView.findViewById(R.id.pass);
        login = rootView.findViewById(R.id.b1);
        signup = rootView.findViewById(R.id.b2);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        /* ========= SIGN UP (USER ONLY) ========= */
        signup.setOnClickListener(v -> {
            login.setEnabled(false);
            signup.setEnabled(false);
            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty()) {
                showSnack("Please fill all fields");
                login.setEnabled(true);
                signup.setEnabled(true);
                return;
            }

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        User user = new User(e, "user", true);

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(unused -> {
                                    login.setEnabled(true);
                                    signup.setEnabled(true);
                                    showSnack("Account created successfully");
                                    Log.d(TAG, "User created: " + e);
                                    loging();
                                })
                                .addOnFailureListener(err -> {
                                    login.setEnabled(true);
                                    signup.setEnabled(true);
                                    Log.e(TAG, "Failed to save user in Firestore", err);
                                    showSnack("Account created, but failed to save user data.");
                                });
                    }).addOnFailureListener(err -> {
                        login.setEnabled(true);
                        signup.setEnabled(true);
                        Log.e(TAG, "Sign up failed", err);
                        showSnack(err.getMessage());
                    });
            login.setEnabled(true);
            signup.setEnabled(true);
        });

        /* ========= LOGIN ========= */
        login.setOnClickListener(v -> loging());


        return rootView;
    }
    private void loging()  {
        login.setEnabled(false);
        signup.setEnabled(false);
        String e = email.getText().toString().trim();
        String p = password.getText().toString().trim();

        if (e.isEmpty() || p.isEmpty()) {
            showSnack("Please fill all fields");
            login.setEnabled(true);
            signup.setEnabled(true);
            return;
        }

        auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    login.setEnabled(true);
                    signup.setEnabled(true);
                    String uid = result.getUser().getUid();

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                if (!doc.exists()) {
                                    auth.signOut();
                                    showSnack("Account data not found");
                                    Log.w(TAG, "No Firestore doc for UID: " + uid);
                                    return;
                                }

                                Boolean active = doc.getBoolean("active");
                                if (active == null || !active) {
                                    auth.signOut();
                                    showSnack("Account disabled. Contact manager.");
                                    Log.w(TAG, "Disabled account: " + e);
                                    return;
                                }

                                String role = doc.getString("role");
                                Log.d(TAG, "Login success, role=" + role);

                                if ("manager".equals(role)) {
                                    ((MainActivity) getActivity())
                                            .setmenu(role);
                                    ((MainActivity) getActivity())
                                            .changefrag(new Manager_fragment());
                                } else {
                                    ((MainActivity) getActivity())
                                            .setmenu(role);
                                    ((MainActivity) getActivity())
                                            .changefrag( new UserProductsFragment());
                                }
                            })
                            .addOnFailureListener(err -> {
                                login.setEnabled(true);
                                signup.setEnabled(true);
                                if (getActivity() != null) {
                                    auth.signOut(); // Sign out to prevent inconsistent state
                                    Log.e(TAG, "Failed to fetch user data from Firestore", err);
                                    showSnack("Could not retrieve account details.");
                                }
                            });
                })
                .addOnFailureListener(err -> {
                    login.setEnabled(true);
                    signup.setEnabled(true);
                    Log.e(TAG, "Login failed", err);
                    showSnack(err.getMessage());
                });
        login.setEnabled(true);
        signup.setEnabled(true);
    }
    private void showSnack(String msg) {
        // Check if view is available before showing snackbar
        if (rootView != null) {
            Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG).show();
        }
    }
}
