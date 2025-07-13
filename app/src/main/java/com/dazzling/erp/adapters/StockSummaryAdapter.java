package com.dazzling.erp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.StockSummary;

import java.util.List;

public class StockSummaryAdapter extends RecyclerView.Adapter<StockSummaryAdapter.ViewHolder> {

    private List<StockSummary> stockSummaries;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private OnMenuClickListener onMenuClickListener;

    public interface OnItemClickListener {
        void onItemClick(StockSummary stockSummary);
    }

    public interface OnMenuClickListener {
        void onMenuClick(View view, StockSummary stockSummary);
    }

    public StockSummaryAdapter(Context context, List<StockSummary> stockSummaries) {
        this.context = context;
        this.stockSummaries = stockSummaries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockSummary stockSummary = stockSummaries.get(position);
        
        // Set title
        holder.textTitle.setText("Stock Summary");
        
        // Set date
        holder.textDate.setText(stockSummary.getDate());
        
        // Set product type
        holder.textType.setText(stockSummary.getProductType());
        
        // Set office
        String officeText = stockSummary.getOffice() != null ? stockSummary.getOffice() : "Unknown Office";
        holder.textOffice.setText(officeText);
        
        // Set icon based on product type
        switch (stockSummary.getProductType()) {
            case "Polo":
                holder.imageIcon.setImageResource(R.drawable.ic_polo);
                break;
            case "T-Shirt":
                holder.imageIcon.setImageResource(R.drawable.ic_tshirt);
                break;
            case "Stripe Polo":
                holder.imageIcon.setImageResource(R.drawable.ic_stripe_polo);
                break;
            default:
                holder.imageIcon.setImageResource(R.drawable.ic_fabric);
                break;
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(stockSummary);
            }
        });

        holder.buttonMenu.setOnClickListener(v -> {
            if (onMenuClickListener != null) {
                onMenuClickListener.onMenuClick(v, stockSummary);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stockSummaries.size();
    }

    public void updateData(List<StockSummary> newStockSummaries) {
        this.stockSummaries = newStockSummaries;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnMenuClickListener(OnMenuClickListener listener) {
        this.onMenuClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        TextView textTitle;
        TextView textDate;
        TextView textType;
        TextView textOffice;
        AppCompatImageButton buttonMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textTitle = itemView.findViewById(R.id.text_title);
            textDate = itemView.findViewById(R.id.text_date);
            textType = itemView.findViewById(R.id.text_type);
            textOffice = itemView.findViewById(R.id.text_office);
            buttonMenu = itemView.findViewById(R.id.button_menu);
        }
    }
} 