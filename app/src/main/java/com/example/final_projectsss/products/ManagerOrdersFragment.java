package com.example.final_projectsss.products;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.R;
import com.example.final_projectsss.molds.OrderAdapter;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ManagerOrdersFragment extends Fragment implements OrderAdapter.Listener {

    private RecyclerView rvOrders;
    private OrderAdapter adapter;
    private final ArrayList<OrderModel> orders = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ManagerOrdersFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_manager_orders, container, false);

        rvOrders = v.findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderAdapter(orders, this);
        rvOrders.setAdapter(adapter);

        loadOrders();

        return v;
    }

    private void loadOrders() {
        orders.clear();
        adapter.notifyDataSetChanged();

        db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orders.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        OrderModel model = new OrderModel();
                        model.id = doc.getId();
                        model.userId = doc.getString("userId");
                        model.userEmail = doc.getString("userEmail");
                        model.productId = doc.getString("productId");
                        model.productName = doc.getString("productName");
                        model.image = doc.getString("image");

                        Double priceDouble = doc.getDouble("price");
                        model.price = priceDouble != null ? priceDouble : 0.0;

                        Long q = doc.getLong("quantity");
                        model.quantity = q != null ? q.intValue() : 0;

                        orders.add(model);
                    }

                    adapter.notifyDataSetChanged();

                    if (orders.isEmpty()) {
                        showToast("No orders yet");
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to load orders"));
    }

    @Override
    public void onReady(OrderModel order) {
        if (!isAdded()) return;

        if (order == null) {
            showToast("Invalid order");
            return;
        }

        if (TextUtils.isEmpty(order.id)) {
            showToast("Order id is missing");
            return;
        }

        if (TextUtils.isEmpty(order.userId)) {
            showToast("User id is missing, cannot notify user");
            return;
        }

        String productName = order.productName != null ? order.productName : "your order";

        new AlertDialog.Builder(requireContext())
                .setTitle("Order Ready")
                .setMessage("Mark this order as ready and notify the user?")
                .setPositiveButton("Yes", (d, w) -> markOrderReady(order, productName))
                .setNegativeButton("No", null)
                .show();
    }

    private void markOrderReady(OrderModel order, String productName) {
        WriteBatch batch = db.batch();

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("type", "order_ready");
        alertData.put("message", "Your order for " + productName + " is ready.");
        alertData.put("createdAt", FieldValue.serverTimestamp());

        batch.set(
                db.collection("users")
                        .document(order.userId)
                        .collection("alerts")
                        .document(),
                alertData
        );

        batch.delete(db.collection("orders").document(order.id));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    int position = orders.indexOf(order);
                    if (position != -1) {
                        orders.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, orders.size());
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    showToast("User will be notified that the order is ready");

                    if (orders.isEmpty()) {
                        showToast("No orders left");
                    }
                })
                .addOnFailureListener(e ->
                        showToast("Failed to mark order ready"));
    }

    private void showToast(String msg) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}