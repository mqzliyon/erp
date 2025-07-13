package com.dazzling.erp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.StockSummary;

import java.util.List;

public class CeoStockSummaryAdapter extends RecyclerView.Adapter<CeoStockSummaryAdapter.ViewHolder> {

    private List<StockSummary> stockSummaries;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(StockSummary stockSummary);
    }

    public CeoStockSummaryAdapter(Context context, List<StockSummary> stockSummaries) {
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

        // Hide the menu button for CEO view (read-only)
        holder.buttonMenu.setVisibility(View.GONE);

        // Set click listener for item (read-only details view)
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(stockSummary);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        TextView textTitle;
        TextView textDate;
        TextView textType;
        TextView textOffice;
        View buttonMenu; // Changed to View to hide it

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