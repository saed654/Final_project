package com.example.final_projectsss.molds;

import static androidx.core.content.ContentProviderCompat.requireContext;

import static java.security.AccessController.getContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.R;
import com.example.final_projectsss.products.OrderModel;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface Listener {
        void onReady(OrderModel order);
    }

    private final List<OrderModel> list;
    private final Listener listener;

    public OrderAdapter(List<OrderModel> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manager_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        OrderModel order = list.get(position);

        h.tvName.setText(order.productName);
        h.tvUser.setText("User: " + order.userEmail);
        h.tvQty.setText("Qty: " + order.quantity);
        h.tvPrice.setText("Price: ₪ " + order.price);

        if (order.image != null && !order.image.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(order.image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                h.ivProduct.setImageBitmap(bitmap);
            } catch (Exception e) {
                h.ivProduct.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            h.ivProduct.setImageResource(R.drawable.ic_placeholder);
        }

        h.btnReady.setOnClickListener(v -> listener.onReady(order));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvUser, tvQty, tvPrice;
        Button btnReady;

        public VH(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivOrderImage);
            tvName = itemView.findViewById(R.id.tvOrderName);
            tvUser = itemView.findViewById(R.id.tvOrderUser);
            tvQty = itemView.findViewById(R.id.tvOrderQty);
            tvPrice = itemView.findViewById(R.id.tvOrderPrice);
            btnReady = itemView.findViewById(R.id.btnOrderReady);
        }
    }
}