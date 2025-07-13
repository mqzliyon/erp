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

public class ManagerRongdhonuFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_rongdhonu, container, false);
        
        // Initialize views and setup data
        setupViews(view);
        
        return view;
    }
    
    private void setupViews(View view) {
        TextView titleText = view.findViewById(R.id.title_text);
        titleText.setText("Rongdhonu Office - Manager View");
        
        // Add your Manager-specific Rongdhonu Office functionality here
        // This could include data loading, UI setup, etc.
    }
} 