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
    }

    public void setLots(List<Lot> lots) {
        this.lots = lots;
        notifyDataSetChanged();
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