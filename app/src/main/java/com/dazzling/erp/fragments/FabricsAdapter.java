package com.dazzling.erp.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import androidx.appcompat.widget.AppCompatImageButton;

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
        
        // Compact display for small lists
        String fabricType = fabric.getFabricType();
        if (fabricType != null && fabricType.length() > 20) {
            fabricType = fabricType.substring(0, 17) + "...";
        }
        holder.typeText.setText(fabricType);
        
        // Compact quantity display
        holder.quantityText.setText(String.format(Locale.getDefault(), "%.1f kg", fabric.getQuantityKg()));
        
        // Compact date display
        String dateText = fabric.getCreatedAt() != null ? dateFormat.format(fabric.getCreatedAt()) : "";
        holder.dateText.setText(dateText);
        
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
        
        // Handle 3-dot menu for small fabrics lists
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(holder.menuButton.getContext(), holder.menuButton);
            popup.getMenuInflater().inflate(R.menu.menu_fabric_item, popup.getMenu());
            
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_view) {
                    if (menuClickListener != null) menuClickListener.onView(fabric);
                    return true;
                } else if (item.getItemId() == R.id.action_update) {
                    if (menuClickListener != null) menuClickListener.onEdit(fabric);
                    return true;
                } else if (item.getItemId() == R.id.action_transfer) {
                    if (menuClickListener != null) menuClickListener.onTransfer(fabric);
                    return true;
                } else if (item.getItemId() == R.id.action_delete) {
                    if (menuClickListener != null) menuClickListener.onDelete(fabric);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return fabrics != null ? fabrics.size() : 0;
    }

    static class FabricViewHolder extends RecyclerView.ViewHolder {
        TextView typeText, quantityText, dateText;
        ImageView transferButton;
        ImageView menuButton;
        FabricViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.text_fabric_type);
            quantityText = itemView.findViewById(R.id.text_fabric_quantity);
            dateText = itemView.findViewById(R.id.text_fabric_date);
            transferButton = itemView.findViewById(R.id.button_transfer);
            menuButton = itemView.findViewById(R.id.button_menu);
        }
    }
} 