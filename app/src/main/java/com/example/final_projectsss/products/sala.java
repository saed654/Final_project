package com.example.final_projectsss.products;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class sala extends Fragment implements ProductAdapter.Listener {

    protected RecyclerView rv;
    protected ArrayList<Product> cartproducts = new ArrayList<>();
    protected ProductAdapter adapter;
    protected FirebaseFirestore db;
    FirebaseAuth auth;

    private TextView tvTotalPrice;
    private Button btnOrderAll;

    private boolean isOrdering = false;

    public sala() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sala, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rv = v.findViewById(R.id.rvsala);
        tvTotalPrice = v.findViewById(R.id.tvTotalPrice);
        btnOrderAll = v.findViewById(R.id.btnOrderAll);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(cartproducts, false, this);
        rv.setAdapter(adapter);

        btnOrderAll.setOnClickListener(v1 -> orderAllProducts());

        loadProducts();

        return v;
    }

    private void loadProducts() {
        cartproducts.clear();
        adapter.notifyDataSetChanged();
        updateTotalPrice();

        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Cannot load cart, user is not logged in.");
            showToast("You must be logged in");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("cart")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartproducts.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "User cart is empty.");
                        adapter.notifyDataSetChanged();
                        updateTotalPrice();
                        return;
                    }

                    queryDocumentSnapshots.forEach(doc -> {
                        String productId = doc.getId();
                        String name = doc.getString("productName");
                        Double priceDouble = doc.getDouble("price");
                        String imageString = doc.getString("image");
                        Long numberLong = doc.getLong("number");

                        double price = priceDouble != null ? priceDouble : 0.0;
                        int number = numberLong != null ? numberLong.intValue() : 1;

                        cartproducts.add(new Product(
                                productId,
                                name != null ? name : "",
                                price,
                                imageString,
                                true,
                                number
                        ));
                    });

                    adapter.notifyDataSetChanged();
                    updateTotalPrice();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load cart", e);
                    showToast("Failed to load cart");
                });
    }

    private void updateTotalPrice() {
        double total = 0.0;

        for (Product p : cartproducts) {
            total += (p.price * p.num);
        }

        tvTotalPrice.setText(String.format(Locale.getDefault(), "Total: ₪ %.2f", total));
    }

    private int findProductIndexById(String productId) {
        for (int i = 0; i < cartproducts.size(); i++) {
            if (cartproducts.get(i).id.equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    private void showToast(String msg) {
        if (isAdded()) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onplus(int pos, Product p) {
        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in");
            return;
        }

        int newNum = cartproducts.get(pos).num + 1;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("cart")
                .document(p.id)
                .update("number", newNum)
                .addOnSuccessListener(unused -> {
                    cartproducts.get(pos).num = newNum;
                    adapter.notifyItemChanged(pos);
                    updateTotalPrice();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to increase quantity", e);
                    showToast("Failed to update quantity");
                });
    }

    @Override
    public void onmin(int pos, Product p) {
        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in");
            return;
        }

        if (cartproducts.get(pos).num > 1) {
            int newNum = cartproducts.get(pos).num - 1;

            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("cart")
                    .document(p.id)
                    .update("number", newNum)
                    .addOnSuccessListener(unused -> {
                        cartproducts.get(pos).num = newNum;
                        adapter.notifyItemChanged(pos);
                        updateTotalPrice();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to decrease quantity", e);
                        showToast("Failed to update quantity");
                    });

        } else {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("cart")
                    .document(p.id)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        if (pos >= 0 && pos < cartproducts.size()) {
                            cartproducts.remove(pos);
                            adapter.notifyItemRemoved(pos);
                            updateTotalPrice();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove product", e);
                        showToast("Failed to remove product");
                    });
        }
    }

    @Override
    public void ondlete(Product p) {
        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in");
            return;
        }

        int index = findProductIndexById(p.id);
        if (index == -1) return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("cart")
                .document(p.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    cartproducts.remove(index);
                    adapter.notifyItemRemoved(index);
                    updateTotalPrice();
                    showToast("Product removed");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete product", e);
                    showToast("Failed to delete product");
                });
    }

    @Override
    public void onorder(Product p) {
        if (p == null) return;

        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in");
            return;
        }

        if (isOrdering) return;
        isOrdering = true;
        btnOrderAll.setEnabled(false);

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail() == null ? "" : auth.getCurrentUser().getEmail();

        WriteBatch batch = db.batch();

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("userId", uid);
        orderMap.put("userEmail", email);
        orderMap.put("productId", p.id);
        orderMap.put("productName", p.name);
        orderMap.put("image", p.imgbase64);
        orderMap.put("price", p.price);
        orderMap.put("quantity", p.num);
        orderMap.put("createdAt", FieldValue.serverTimestamp());
        orderMap.put("ready", false);

        batch.set(db.collection("orders").document(), orderMap);
        batch.delete(db.collection("users").document(uid).collection("cart").document(p.id));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    int index = findProductIndexById(p.id);
                    if (index != -1) {
                        cartproducts.remove(index);
                        adapter.notifyItemRemoved(index);
                    }
                    updateTotalPrice();
                    showToast("Product ordered successfully");
                    isOrdering = false;
                    btnOrderAll.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to order product", e);
                    showToast("Failed to order product");
                    isOrdering = false;
                    btnOrderAll.setEnabled(true);
                });
    }

    private void orderAllProducts() {
        if (auth.getCurrentUser() == null) {
            showToast("You must be logged in");
            return;
        }

        if (cartproducts.isEmpty()) {
            showToast("Your cart is empty");
            return;
        }

        if (isOrdering) return;
        isOrdering = true;
        btnOrderAll.setEnabled(false);

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail() == null ? "" : auth.getCurrentUser().getEmail();

        WriteBatch batch = db.batch();

        for (Product item : cartproducts) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("userId", uid);
            orderMap.put("userEmail", email);
            orderMap.put("productId", item.id);
            orderMap.put("productName", item.name);
            orderMap.put("image", item.imgbase64);
            orderMap.put("price", item.price);
            orderMap.put("quantity", item.num);
            orderMap.put("createdAt", FieldValue.serverTimestamp());
            orderMap.put("ready", false);

            batch.set(db.collection("orders").document(), orderMap);
            batch.delete(db.collection("users").document(uid).collection("cart").document(item.id));
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    cartproducts.clear();
                    adapter.notifyDataSetChanged();
                    updateTotalPrice();
                    showToast("All products ordered successfully");
                    isOrdering = false;
                    btnOrderAll.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to order all products", e);
                    showToast("Failed to order all products");
                    isOrdering = false;
                    btnOrderAll.setEnabled(true);
                });
    }

    @Override
    public void onAdd() {
    }

    @Override
    public void onClick(Product p) {
    }

    @Override
    public void onLongPress(Product p) {
    }
}