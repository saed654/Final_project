package com.example.final_projectsss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.final_projectsss.log_in.Log_in_fragment;
import com.example.final_projectsss.molds.NotificationHelper;
import com.example.final_projectsss.molds.OrderAlertHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Home_fragment extends Fragment {
     private FirebaseFirestore db = FirebaseFirestore.getInstance();
     LinearLayout log_inContainer;

    private Button btnContactUs;
    private Button btnLoginNow;

    public Home_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnContactUs = view.findViewById(R.id.btnContactUs);
        btnLoginNow = view.findViewById(R.id.btnLoginNow);
        log_inContainer=view.findViewById(R.id.log_inh);

        NotificationHelper.createNotificationChannel(requireContext());
        OrderAlertHelper.checkAndShowReadyNotifications(requireContext());
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            log_inContainer.setVisibility(View.GONE);
        } else {
            log_inContainer.setVisibility(View.VISIBLE);
        }

        // Open dialer with store phone number
        btnContactUs.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:0559211349"));
            startActivity(intent);
        });

        // Go to login page
        btnLoginNow.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).changefrag(new Log_in_fragment(), R.id.log_inmenu);
            }
        });

        return view;
    }
}