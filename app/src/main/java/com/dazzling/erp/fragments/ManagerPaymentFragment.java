package com.dazzling.erp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dazzling.erp.R;

public class ManagerPaymentFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_payment, container, false);
        
        // Initialize views and setup data
        setupViews(view);
        
        return view;
    }
    
    private void setupViews(View view) {
        TextView titleText = view.findViewById(R.id.title_text);
        titleText.setText("Payment Request - Manager View");
        
        // Add your Manager-specific Payment Request functionality here
        // This could include data loading, UI setup, etc.
    }
} 