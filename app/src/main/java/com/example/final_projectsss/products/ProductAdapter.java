package com.example.final_projectsss.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.final_projectsss.R;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onplus(int pos,Product p);
        void onmin(int pos,Product p);
        void ondlete(Product p);
        void onorder(Product p);
        void onAdd();
        void onClick(Product p);
        void onLongPress(Product p);
    }

    private static final int TYPE_ADD = 0;
    private static final int TYPE_PRODUCT= 1;
    private static final int TYPE_SALA = 2;


    private List<Product> products;
    private boolean isManager;
    private Listener listener;

    public ProductAdapter(List<Product> products, boolean isManager, Listener listener) {
        this.products = products;
        this.isManager = isManager;
        this.listener = listener;
    }


    @Override
    public int getItemViewType(int position) {

        if (isManager && position == 0) return TYPE_ADD;

        int realPos = isManager ? position - 1 : position;
        Product p = products.get(realPos);

        return p.sala ? TYPE_SALA : TYPE_PRODUCT;
    }


    @Override
    public int getItemCount() {
        return isManager ? products.size() + 1 : products.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_ADD) {
            return new AddVH(inflater.inflate(
                    R.layout.item_add, parent, false));
        }

        if (viewType == TYPE_PRODUCT) {
            return new ProductVH(inflater.inflate(
                    R.layout.item_product, parent, false));
        }

        return new Salaproduct(inflater.inflate(
                R.layout.sala_product, parent, false));
    }



    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof AddVH) {
            holder.itemView.setOnClickListener(v -> listener.onAdd());
            return;
        }

        int realPos = isManager ? position - 1 : position;
        Product p = products.get(realPos);


        if (holder instanceof Salaproduct) {
            Salaproduct vh = (Salaproduct) holder;
            if (p.imgbase64 == null || p.imgbase64.isEmpty())
                vh.image.setImageResource(R.drawable.ic_placeholder);
            else
                vh.image.setImageBitmap(
                        ManagerProductsFragment.base64ToBitmap(p.imgbase64)
                );
            vh.plus.setOnClickListener(v -> listener.onplus(realPos,p));
            vh.min.setOnClickListener(v -> listener.onmin(realPos,p));
            vh.name.setText(p.name);
            vh.number.setText(String.valueOf(p.num));
            vh.deletesala.setOnClickListener(v -> listener.ondlete(p));
            vh.order.setOnClickListener(v -> listener.onorder(p));
            return; // 🔥 REQUIRED
        }

        ProductVH vh = (ProductVH) holder;
        vh.name.setText(p.name);
        vh.price.setText("$" + p.price);

        if (p.imgbase64 == null || p.imgbase64.isEmpty())
            vh.image.setImageResource(R.drawable.ic_placeholder);
        else
            vh.image.setImageBitmap(
                    ManagerProductsFragment.base64ToBitmap(p.imgbase64)
            );

        vh.itemView.setOnClickListener(v -> listener.onClick(p));

        if (isManager) {
            vh.itemView.setOnLongClickListener(v -> {
                listener.onLongPress(p);
                return true;
            });
        }
    }


    static class ProductVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;

        ProductVH(View v) {
            super(v);
            image = v.findViewById(R.id.img);
            name = v.findViewById(R.id.name);
            price = v.findViewById(R.id.price);
        }
    }

    static class AddVH extends RecyclerView.ViewHolder {
        AddVH(View v) {
            super(v);
        }
    }

    static class Salaproduct extends RecyclerView.ViewHolder {
        ImageView image;
         ImageButton min,plus;
        TextView name,number;
        Button deletesala,order;

        Salaproduct(View v) {
            super(v);
            min = v.findViewById(R.id.btn_minus);
            plus = v.findViewById(R.id.btn_plus);
            image = v.findViewById(R.id.salaimg);
            number=v.findViewById(R.id.number);
            name=v.findViewById(R.id.name);
            deletesala = v.findViewById(R.id.deletesala);
            order = v.findViewById(R.id.order);

        }
    }
}
