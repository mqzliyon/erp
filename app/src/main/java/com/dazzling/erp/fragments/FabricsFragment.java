package com.dazzling.erp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.Fabric;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import com.dazzling.erp.fragments.FabricsAdapter;

/**
 * Fragment for managing fabrics
 * Displays list of fabrics and allows CRUD operations
 */
public class FabricsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAdd;
    private List<Fabric> fabricsList;
    private FirestoreService firestoreService;
    private FabricsAdapter fabricsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fabrics, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadFabrics();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_fabrics);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        fabAdd = view.findViewById(R.id.fab_add_fabric);
        
        fabricsList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fabricsAdapter = new FabricsAdapter(fabricsList);
        recyclerView.setAdapter(fabricsAdapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            showAddFabricDialog();
        });
    }

    private void showAddFabricDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_fabric, null);
        final EditText editFabricType = dialogView.findViewById(R.id.edit_fabric_type);
        final EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        final EditText editDate = dialogView.findViewById(R.id.edit_date);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        editDate.setOnClickListener(view -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (datePicker, y, m, d) -> {
                calendar.set(y, m, d);
                editDate.setText(dateFormat.format(calendar.getTime()));
            }, year, month, day);
            datePickerDialog.show();
        });

        final com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle("Add Fabric")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null);

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean valid = true;
            String fabricType = editFabricType.getText().toString().trim();
            String quantityStr = editQuantity.getText().toString().trim();
            String dateStr = editDate.getText().toString().trim();

            if (fabricType.isEmpty()) {
                editFabricType.setError("Fabric Type is required");
                valid = false;
            } else {
                editFabricType.setError(null);
            }
            if (quantityStr.isEmpty()) {
                editQuantity.setError("Quantity is required");
                valid = false;
            } else {
                try {
                    Float.parseFloat(quantityStr);
                    editQuantity.setError(null);
                } catch (NumberFormatException e) {
                    editQuantity.setError("Quantity must be a number");
                    valid = false;
                }
            }
            if (dateStr.isEmpty()) {
                editDate.setError("Date is required");
                valid = false;
            } else {
                editDate.setError(null);
            }
            if (!valid) return;

            float quantity = Float.parseFloat(quantityStr);
            java.util.Date date;
            try {
                date = dateFormat.parse(dateStr);
            } catch (Exception e) {
                editDate.setError("Invalid date format");
                return;
            }
            // Create Fabric object (using only required fields for now)
            Fabric fabric = new Fabric();
            fabric.setFabricType(fabricType);
            fabric.setQuantityKg(quantity);
            fabric.setCreatedAt(date);
            // Add to Firestore
            firestoreService.addFabric(fabric, new FirestoreService.FabricCallback() {
                @Override
                public void onFabricsLoaded(List<Fabric> fabrics) {}
                @Override
                public void onFabricAdded(Fabric fabric) {
                    Snackbar.make(requireView(), "Fabric added!", Snackbar.LENGTH_SHORT).show();
                    loadFabrics();
                }
                @Override
                public void onFabricUpdated(Fabric fabric) {}
                @Override
                public void onFabricDeleted(String fabricId) {}
                @Override
                public void onError(String error) {
                    Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
                }
            });
            // Dismiss dialog if successful
            dialog.dismiss();
        });
    }

    private void loadFabrics() {
        firestoreService.getFabrics(new FirestoreService.FabricCallback() {
            @Override
            public void onFabricsLoaded(List<Fabric> fabrics) {
                if (fabrics != null && !fabrics.isEmpty()) {
                    fabricsList.clear();
                    fabricsList.addAll(fabrics);
                    fabricsAdapter.setFabrics(fabricsList);
                    showFabricsList();
                } else {
                    showEmptyState();
                }
            }
            @Override public void onFabricAdded(Fabric fabric) {}
            @Override public void onFabricUpdated(Fabric fabric) {}
            @Override public void onFabricDeleted(String fabricId) {}
            @Override public void onError(String error) {
                showEmptyState();
                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("No fabrics found.\nTap + to add a new fabric.");
    }

    private void showFabricsList() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }
} 