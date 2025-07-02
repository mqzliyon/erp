package com.dazzling.erp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.Cutting;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing cutting operations
 * Displays list of cutting operations and allows CRUD operations
 */
public class CuttingFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAdd;
    private List<Cutting> cuttingList;
    private FirestoreService firestoreService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cutting, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadCuttingOperations();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_cutting);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        fabAdd = view.findViewById(R.id.fab_add_cutting);
        
        cuttingList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter when RecyclerView adapter is created
        // recyclerView.setAdapter(new CuttingAdapter(cuttingList));
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            // TODO: Open add cutting operation dialog
            Snackbar.make(v, "Add Cutting Operation functionality coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void loadCuttingOperations() {
        // TODO: Load cutting operations from Firestore
        // For now, show empty state
        showEmptyState();
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("No cutting operations found.\nTap + to add a new operation.");
    }

    private void showCuttingList() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }
} 