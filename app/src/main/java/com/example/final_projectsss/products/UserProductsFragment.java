package com.example.final_projectsss.products;



import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import com.example.final_projectsss.molds.NotificationHelper;
import com.example.final_projectsss.molds.OrderAlertHelper;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.MainActivity;
import com.example.final_projectsss.R;
import com.example.final_projectsss.ai.Ai_fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserProductsFragment extends Fragment implements ProductAdapter.Listener {

    private static final String TAG = "UserProductsFragment";
    private ListenerRegistration productsListener;
    protected RecyclerView rv;
    protected ArrayList<Product> products = new ArrayList<>();
    protected ProductAdapter adapter;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    protected FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_products, container, false);

        FloatingActionButton fab = v.findViewById(R.id.fab_add);
        rv = v.findViewById(R.id.rv);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new ProductAdapter(products, false, this);
        rv.setAdapter(adapter);

        // Guests cannot open cart
        if (auth.getCurrentUser() == null) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v1 -> openCartSafely());
        }

        NotificationHelper.createNotificationChannel(requireContext());
            OrderAlertHelper.checkAndShowReadyNotifications(requireContext());

        loadProducts();

        return v;
    }
    protected void loadProducts() {
        productsListener = db.collection("products")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (!isAdded()) return;

                    if (e != null) {
                        Log.e(TAG, "Error listening to products", e);
                        safeToast("Failed to load products");
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "Products snapshot is null");
                        return;
                    }

                    ArrayList<Product> temp = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();

                        String name = doc.getString("name");
                        if (name == null) name = "";

                        Double priceDouble = doc.getDouble("price");
                        double price = (priceDouble != null) ? priceDouble : 0.0;

                        String imageString = doc.getString("image");
                        if (imageString == null) imageString = "";

                        temp.add(new Product(id, name, price, imageString));
                    }

                    products.clear();
                    products.addAll(temp);
                    adapter.notifyDataSetChanged();

                    Log.d(TAG, "Products updated live. Count = " + products.size());
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
    }


    @Override
    public void onClick(Product p) {

        // Guests can ONLY see products visually, no AI, no cart, no actions
        if (auth.getCurrentUser() == null) {
            safeToast("You must log in to use AI or cart features");
            return;
        }

        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(p.name)
                .setItems(new String[]{"AI Info", "Add to cart(order)"}, (d, i) -> {
                    if (!isAdded()) return;

                    if (i == 0) {
                        openAiFragmentSafely(p);
                    } else {
                        addToCartSafely(p);
                    }
                })
                .show();
    }

    private void addToCartSafely(Product p) {
        if (auth.getCurrentUser() == null) {
            safeToast("You must log in first");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        DocumentReference cartItemRef = db.collection("users")
                .document(uid)
                .collection("cart")
                .document(p.id);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(cartItemRef);

                    if (snapshot.exists()) {
                        Long currentNumberObj = snapshot.getLong("number");
                        long currentNumber = (currentNumberObj != null) ? currentNumberObj : 0;
                        transaction.update(cartItemRef, "number", currentNumber + 1);
                    } else {
                        Map<String, Object> product = new HashMap<>();
                        product.put("number", 1);
                        product.put("productId", p.id);
                        product.put("productName", p.name);
                        product.put("image", p.imgbase64);
                        product.put("price", p.price);
                        product.put("createdAt", com.google.firebase.Timestamp.now());
                        transaction.set(cartItemRef, product);
                    }
                    return null;
                }).addOnSuccessListener(unused -> safeToast("Added to cart"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add to cart", e);
                    safeToast("Failed to add to cart");
                });
    }

    private void openAiFragmentSafely(Product p) {
        if (!isAdded()) return;
        if (!(getActivity() instanceof MainActivity)) return;

        Ai_fragment aiFragment = new Ai_fragment();

        Bundle bundle = new Bundle();
        bundle.putString("product_name", p.name);
        bundle.putString("product_id", p.id);
        bundle.putDouble("product_price", p.price);
        aiFragment.setArguments(bundle);

        ((MainActivity) getActivity()).changefrag(aiFragment, R.id.aimenu);
    }

    private void openCartSafely() {
        if (!isAdded()) return;
        if (!(getActivity() instanceof MainActivity)) return;

        ((MainActivity) getActivity()).changefrag(new sala());
    }

    protected void safeToast(String msg) {
        if (!isAdded()) return;

        Context context = getContext();
        if (context == null) return;

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onplus(int pos, Product p) {
    }

    @Override
    public void onmin(int pos, Product p) {
    }

    @Override
    public void ondlete(Product p) {
    }

    @Override
    public void onorder(Product p) {
    }

    @Override
    public void onAdd() {
    }

    @Override
    public void onLongPress(Product p) {
    }
}