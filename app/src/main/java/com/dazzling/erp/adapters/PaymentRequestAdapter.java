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
import com.dazzling.erp.models.PaymentRequest;

import java.text.DecimalFormat;
import java.util.List;

public class PaymentRequestAdapter extends RecyclerView.Adapter<PaymentRequestAdapter.ViewHolder> {
    
    private Context context;
    private List<PaymentRequest> paymentRequests;
    private OnItemClickListener onItemClickListener;
    private OnMenuClickListener onMenuClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(PaymentRequest paymentRequest);
    }
    
    public interface OnMenuClickListener {
        void onMenuClick(View view, PaymentRequest paymentRequest);
    }
    
    public PaymentRequestAdapter(Context context, List<PaymentRequest> paymentRequests) {
        this.context = context;
        this.paymentRequests = paymentRequests;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnMenuClickListener(OnMenuClickListener listener) {
        this.onMenuClickListener = listener;
    }
    
    public void updateData(List<PaymentRequest> newPaymentRequests) {
        this.paymentRequests = newPaymentRequests;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_request, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentRequest paymentRequest = paymentRequests.get(position);
        
        // Set payment method
        holder.textPaymentMethod.setText(paymentRequest.getPaymentMethod());
        
        // Set amount with BDT symbol and formatting
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        String formattedAmount = "৳ " + formatter.format(paymentRequest.getAmount());
        holder.textAmount.setText(formattedAmount);
        
        // Set invoice number
        holder.textInvoiceNumber.setText("Invoice: " + paymentRequest.getInvoiceNumber());
        
        // Set date and time
        holder.textDate.setText(paymentRequest.getDate());
        holder.textTime.setText(paymentRequest.getTime());
        
        // Set status
        holder.textStatus.setText(paymentRequest.getStatus());
        
        // Set status background color and text color based on status
        if ("Pending".equals(paymentRequest.getStatus())) {
            holder.textStatus.setBackgroundColor(0xFFFACC15); // #FACC15 yellow
            holder.textStatus.setTextColor(0xFF000000); // Black text
        } else if ("Approved".equals(paymentRequest.getStatus())) {
            holder.textStatus.setBackgroundResource(R.drawable.bg_chip_quantity);
            holder.textStatus.setTextColor(0xFFFFFFFF); // White text
        } else if ("Rejected".equals(paymentRequest.getStatus())) {
            holder.textStatus.setBackgroundResource(R.drawable.bg_notification_dot);
            holder.textStatus.setTextColor(0xFFFFFFFF); // White text
        } else {
            // Default styling for other statuses
            holder.textStatus.setBackgroundResource(R.drawable.bg_chip_date);
            holder.textStatus.setTextColor(0xFFFFFFFF); // White text
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(paymentRequest);
            }
        });
        
        holder.btnMore.setOnClickListener(v -> {
            if (onMenuClickListener != null) {
                onMenuClickListener.onMenuClick(v, paymentRequest);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return paymentRequests.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textPaymentMethod, textAmount, textInvoiceNumber, textDate, textTime, textStatus;
        ImageView btnMore;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPaymentMethod = itemView.findViewById(R.id.text_payment_method);
            textAmount = itemView.findViewById(R.id.text_amount);
            textInvoiceNumber = itemView.findViewById(R.id.text_invoice_number);
            textDate = itemView.findViewById(R.id.text_date);
            textTime = itemView.findViewById(R.id.text_time);
            textStatus = itemView.findViewById(R.id.text_status);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
} 