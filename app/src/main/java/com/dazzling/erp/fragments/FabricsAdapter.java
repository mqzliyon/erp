package com.dazzling.erp.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.Fabric;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FabricsAdapter extends RecyclerView.Adapter<FabricsAdapter.FabricViewHolder> {
    private List<Fabric> fabrics;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnFabricMenuClickListener {
        void onView(Fabric fabric);
        void onEdit(Fabric fabric);
        void onTransfer(Fabric fabric);
        void onDelete(Fabric fabric);
    }

    private OnFabricMenuClickListener menuClickListener;

    public void setOnFabricMenuClickListener(OnFabricMenuClickListener listener) {
        this.menuClickListener = listener;
    }

    public FabricsAdapter(List<Fabric> fabrics) {
        this.fabrics = fabrics;
    }

    public void setFabrics(List<Fabric> fabrics) {
        this.fabrics = fabrics;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FabricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fabric, parent, false);
        return new FabricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FabricViewHolder holder, int position) {
        Fabric fabric = fabrics.get(position);
        holder.typeText.setText(fabric.getFabricType());
        holder.quantityText.setText(String.format(Locale.getDefault(), "%.2f kg", fabric.getQuantityKg()));
        holder.dateText.setText(fabric.getCreatedAt() != null ? dateFormat.format(fabric.getCreatedAt()) : "");
        
        holder.moreImage.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "View");
            popup.getMenu().add(0, 2, 1, "Update");
            popup.getMenu().add(0, 4, 2, "Delete");
            popup.setOnMenuItemClickListener(item -> {
                if (menuClickListener == null) return false;
                switch (item.getItemId()) {
                    case 1:
                        menuClickListener.onView(fabric);
                        return true;
                    case 2:
                        menuClickListener.onEdit(fabric);
                        return true;
                    case 4:
                        menuClickListener.onDelete(fabric);
                        return true;
                }
                return false;
            });
            popup.show();
        });
        
        // Automatically show view on item click
        holder.itemView.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onView(fabric);
            }
        });
        // Handle Transfer For Cutting button
        holder.transferButton.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onTransfer(fabric);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fabrics != null ? fabrics.size() : 0;
    }

    static class FabricViewHolder extends RecyclerView.ViewHolder {
        TextView typeText, quantityText, dateText;
        ImageView moreImage;
        ImageButton transferButton;
        FabricViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.text_fabric_type);
            quantityText = itemView.findViewById(R.id.text_fabric_quantity);
            dateText = itemView.findViewById(R.id.text_fabric_date);
            moreImage = itemView.findViewById(R.id.image_more);
            transferButton = itemView.findViewById(R.id.button_transfer);
        }
    }
} 