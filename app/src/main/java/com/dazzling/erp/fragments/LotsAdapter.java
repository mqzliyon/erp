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
import com.dazzling.erp.models.Lot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LotsAdapter extends RecyclerView.Adapter<LotsAdapter.LotViewHolder> {
    private List<Lot> lots;
    private List<Lot> allLots;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnLotMenuClickListener {
        void onView(Lot lot);
        void onEdit(Lot lot);
        void onDelete(Lot lot);
    }

    private OnLotMenuClickListener menuClickListener;

    public void setOnLotMenuClickListener(OnLotMenuClickListener listener) {
        this.menuClickListener = listener;
    }

    public LotsAdapter(List<Lot> lots) {
        this.lots = lots;
        this.allLots = new java.util.ArrayList<>(lots);
    }

    public void setLots(List<Lot> lots) {
        this.lots = lots;
        this.allLots = new java.util.ArrayList<>(lots);
        notifyDataSetChanged();
    }

    public void filterLots(String query) {
        if (query == null || query.trim().isEmpty()) {
            lots = new java.util.ArrayList<>(allLots);
        } else {
            String searchQuery = query.trim().toLowerCase();
            java.util.List<Lot> filtered = new java.util.ArrayList<>();
            
            for (Lot lot : allLots) {
                // Check if lot number matches (exact or partial)
                if (lot.getLotNumber() != null) {
                    String lotNumber = lot.getLotNumber().toLowerCase();
                    
                    // Exact match (highest priority) - shows first
                    if (lotNumber.equals(searchQuery)) {
                        filtered.add(0, lot);
                        continue;
                    }
                    
                    // Starts with match (high priority)
                    if (lotNumber.startsWith(searchQuery)) {
                        filtered.add(lot);
                        continue;
                    }
                    
                    // Contains match (medium priority)
                    if (lotNumber.contains(searchQuery)) {
                        filtered.add(lot);
                        continue;
                    }
                    
                    // Smart search: Remove "Lot-" prefix for more flexible searching
                    String cleanLotNumber = lotNumber.replace("lot-", "").replace("lot", "");
                    if (cleanLotNumber.contains(searchQuery) || searchQuery.contains(cleanLotNumber)) {
                        filtered.add(lot);
                        continue;
                    }
                }
                
                // Check if date matches (YYYY-MM-DD format)
                if (lot.getCreatedAt() != null) {
                    String dateStr = dateFormat.format(lot.getCreatedAt());
                    if (dateStr.contains(searchQuery)) {
                        filtered.add(lot);
                        continue;
                    }
                }
            }
            
            lots = filtered;
        }
        notifyDataSetChanged();
    }
    
    /**
     * Get search suggestions based on current lots
     */
    public java.util.List<String> getSearchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        String searchQuery = query.trim().toLowerCase();
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        
        for (Lot lot : allLots) {
            if (lot.getLotNumber() != null) {
                String lotNumber = lot.getLotNumber();
                if (lotNumber.toLowerCase().contains(searchQuery)) {
                    suggestions.add(lotNumber);
                }
            }
        }
        
        return suggestions;
    }

    @NonNull
    @Override
    public LotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lot, parent, false);
        return new LotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LotViewHolder holder, int position) {
        Lot lot = lots.get(position);
        holder.lotNumberText.setText(lot.getLotNumber());
        holder.dateText.setText(lot.getCreatedAt() != null ? dateFormat.format(lot.getCreatedAt()) : "");

        holder.itemView.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onView(lot);
            }
        });
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(holder.menuButton.getContext(), holder.menuButton);
            popup.getMenuInflater().inflate(R.menu.menu_lot_item, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_update) {
                    if (menuClickListener != null) menuClickListener.onEdit(lot);
                    return true;
                } else if (item.getItemId() == R.id.action_delete) {
                    if (menuClickListener != null) menuClickListener.onDelete(lot);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return lots != null ? lots.size() : 0;
    }

    static class LotViewHolder extends RecyclerView.ViewHolder {
        TextView lotNumberText, dateText;
        AppCompatImageButton menuButton;
        LotViewHolder(@NonNull View itemView) {
            super(itemView);
            lotNumberText = itemView.findViewById(R.id.text_lot_number);
            dateText = itemView.findViewById(R.id.text_lot_date);
            menuButton = itemView.findViewById(R.id.button_menu);
        }
    }
} 