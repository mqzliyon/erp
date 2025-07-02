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
import com.dazzling.erp.models.Lot;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing lots
 * Displays list of lots and allows CRUD operations
 */
public class LotsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAdd;
    private List<Lot> lotsList;
    private FirestoreService firestoreService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lots, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadLots();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_lots);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        fabAdd = view.findViewById(R.id.fab_add_lot);
        
        lotsList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter when RecyclerView adapter is created
        // recyclerView.setAdapter(new LotsAdapter(lotsList));
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            // TODO: Open add lot dialog
            Snackbar.make(v, "Add Lot functionality coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void loadLots() {
        // TODO: Load lots from Firestore
        // For now, show empty state
        showEmptyState();
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("No lots found.\nTap + to add a new lot.");
    }

    private void showLotsList() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }
} 