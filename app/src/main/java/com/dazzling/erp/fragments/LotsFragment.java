package com.dazzling.erp.fragments;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.Window;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.os.Handler;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.Toast;

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
    private LotsAdapter lotsAdapter;

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
        lotsAdapter = new LotsAdapter(lotsList);
        recyclerView.setAdapter(lotsAdapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> showAddLotDialog());
    }

    private void showAddLotDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_lot);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText editLotNumber = dialog.findViewById(R.id.edit_lot_number);
        EditText editLotDate = dialog.findViewById(R.id.edit_lot_date);
        TextView textLotNumberError = dialog.findViewById(R.id.text_lot_number_error);
        Button buttonCreateLot = dialog.findViewById(R.id.button_create_lot);

        // Set current date if empty
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());
        editLotDate.setText(currentDate);

        editLotDate.setOnClickListener(v -> {
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                java.util.Calendar selected = java.util.Calendar.getInstance();
                selected.set(y, m, d);
                editLotDate.setText(sdf.format(selected.getTime()));
            }, year, month, day);
            datePickerDialog.show();
        });

        buttonCreateLot.setOnClickListener(v -> {
            String lotNumber = editLotNumber.getText().toString().trim();
            String lotDate = editLotDate.getText().toString().trim();
            textLotNumberError.setVisibility(View.GONE);
            if (lotNumber.isEmpty()) {
                textLotNumberError.setText("Lot number is required");
                textLotNumberError.setVisibility(View.VISIBLE);
                return;
            }
            // Check uniqueness
            boolean exists = false;
            for (Lot lot : lotsList) {
                if (lot.getLotNumber().equalsIgnoreCase("Lot-" + lotNumber)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                textLotNumberError.setText("This lot number is already used.");
                textLotNumberError.setTextColor(Color.RED);
                textLotNumberError.setVisibility(View.VISIBLE);
                return;
            }
            if (lotDate.isEmpty()) lotDate = currentDate;
            // Show loading animation
            Dialog loadingDialog = new Dialog(requireContext());
            loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadingDialog.setContentView(R.layout.dialog_loading_lottie);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            LottieAnimationView lottie = loadingDialog.findViewById(R.id.lottie_loader);
            lottie.setAnimation("loader.json");
            lottie.playAnimation();
            loadingDialog.show();
            // Create Lot object
            Lot newLot = new Lot();
            newLot.setLotNumber("Lot-" + lotNumber);
            try {
                newLot.setCreatedAt(sdf.parse(lotDate));
            } catch (Exception e) {
                newLot.setCreatedAt(calendar.getTime());
            }
            firestoreService.addLot(newLot, new FirestoreService.LotCallback() {
                @Override
                public void onLotsLoaded(List<Lot> lots) {}
                @Override
                public void onLotAdded(Lot lot) {
                    loadingDialog.dismiss();
                    dialog.dismiss();
                    showSuccessAlert("Lot created successfully!");
                    showLotsList();
                }
                @Override
                public void onLotUpdated(Lot lot) {}
                @Override
                public void onLotDeleted(String lotId) {}
                @Override
                public void onError(String error) {
                    loadingDialog.dismiss();
                    textLotNumberError.setText(error);
                    textLotNumberError.setTextColor(Color.RED);
                    textLotNumberError.setVisibility(View.VISIBLE);
                }
            });
        });
        dialog.show();
    }

    private void showSuccessAlert(String message) {
        Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.GREEN);
        snackbar.show();
    }

    private void loadLots() {
        firestoreService.getLots(new FirestoreService.LotCallback() {
            @Override
            public void onLotsLoaded(List<Lot> lots) {
                lotsList.clear();
                if (lots != null && !lots.isEmpty()) {
                    lotsList.addAll(lots);
                    lotsAdapter.setLots(lotsList);
                    showLotsList();
                } else {
                    lotsAdapter.setLots(lotsList);
                    showEmptyState();
                }
            }
            @Override
            public void onLotAdded(Lot lot) {}
            @Override
            public void onLotUpdated(Lot lot) {}
            @Override
            public void onLotDeleted(String lotId) {}
            @Override
            public void onError(String error) {
                showEmptyState();
            }
        });
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