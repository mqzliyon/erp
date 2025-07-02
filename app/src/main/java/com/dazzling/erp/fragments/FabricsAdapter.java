package com.dazzling.erp.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    }

    @Override
    public int getItemCount() {
        return fabrics != null ? fabrics.size() : 0;
    }

    static class FabricViewHolder extends RecyclerView.ViewHolder {
        TextView typeText, quantityText, dateText;
        FabricViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.text_fabric_type);
            quantityText = itemView.findViewById(R.id.text_fabric_quantity);
            dateText = itemView.findViewById(R.id.text_fabric_date);
        }
    }
} 