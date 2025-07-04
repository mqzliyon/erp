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
    private Dialog loadingDialog;

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

        lotsAdapter.setOnLotMenuClickListener(new LotsAdapter.OnLotMenuClickListener() {
            @Override
            public void onView(Lot lot) {}
            @Override
            public void onEdit(Lot lot) {}
            @Override
            public void onDelete(Lot lot) {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Lot")
                    .setMessage("Are you sure you want to delete this lot?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        showLoadingDialog();
                        firestoreService.deleteLot(lot.getId(), new FirestoreService.LotCallback() {
                            @Override
                            public void onLotsLoaded(List<Lot> lots) {}
                            @Override
                            public void onLotAdded(Lot lot) {}
                            @Override
                            public void onLotUpdated(Lot lot) {}
                            @Override
                            public void onLotDeleted(String lotId) {
                                hideLoadingDialog();
                                showSuccessAlert("Lot deleted successfully!");
                                loadLots();
                            }
                            @Override
                            public void onError(String error) {
                                hideLoadingDialog();
                                new android.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Error")
                                    .setMessage("Failed to delete lot: " + error)
                                    .setPositiveButton("OK", null)
                                    .show();
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> showAddLotDialog());
    }

    private void showAddLotDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_lot);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Get dialog root view and input fields
        View dialogRoot = dialog.findViewById(R.id.dialog_root);
        EditText editLotNumber = dialog.findViewById(R.id.edit_lot_number);
        EditText editLotDate = dialog.findViewById(R.id.edit_lot_date);
        Button buttonAdd = dialog.findViewById(R.id.button_add);
        Button buttonCancel = dialog.findViewById(R.id.button_cancel);

        // Colors for background change
        int highlightColor = Color.parseColor("#dad5d4");
        int defaultColor = Color.WHITE; // or whatever your normal background is

        // Focus change listener (only dialog background)
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                dialogRoot.setBackgroundColor(highlightColor);
            } else if (!editLotNumber.hasFocus() && !editLotDate.hasFocus()) {
                dialogRoot.setBackgroundColor(defaultColor);
            }
        };

        // Set focus listeners
        editLotNumber.setOnFocusChangeListener(focusListener);
        editLotDate.setOnFocusChangeListener(focusListener);

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

        buttonAdd.setOnClickListener(v -> {
            String lotNumber = editLotNumber.getText().toString().trim();
            String lotDate = editLotDate.getText().toString().trim();
            
            if (lotNumber.isEmpty()) {
                editLotNumber.setError("Please enter lot number");
                return;
            }
            
            // Prepend 'Lot-' to the lot number
            final String finalLotNumber = "Lot-" + lotNumber;
            
            // Parse date string to Date object
            final java.util.Date parsedDate;
            try {
                parsedDate = sdf.parse(lotDate);
            } catch (Exception e) {
                editLotDate.setError("Invalid date");
                return;
            }
            
            showLoadingDialog();
            // Directly add the lot without duplicate validation
            Lot newLot = new Lot();
            newLot.setLotNumber(finalLotNumber);
            newLot.setOrderDate(parsedDate);
            firestoreService.addLot(newLot, new FirestoreService.LotCallback() {
                @Override
                public void onLotsLoaded(List<Lot> lots) {}
                @Override
                public void onLotAdded(Lot lot) {
                    hideLoadingDialog();
                    dialog.dismiss();
                    dialogRoot.setBackgroundColor(defaultColor); // Reset background
                    showSuccessAlert("Lot added successfully!");
                    loadLots(); // Refresh the list
                }
                @Override
                public void onLotUpdated(Lot lot) {}
                @Override
                public void onLotDeleted(String lotId) {}
                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    showSuccessAlert("Error adding lot: " + error);
                }
            });
        });

        buttonCancel.setOnClickListener(v -> {
            dialog.dismiss();
            dialogRoot.setBackgroundColor(defaultColor); // Reset background
        });

        dialog.show();
    }

    private void showSuccessAlert(String message) {
        Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(Color.TRANSPARENT); // transparent background
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.parseColor("#137333")); // dark green text (or use your preferred color)
        textView.setTextSize(16);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // center align text
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

    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;
        loadingDialog = new Dialog(requireContext());
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading_lottie);
        loadingDialog.setCancelable(false);
        LottieAnimationView lottie = loadingDialog.findViewById(R.id.lottie_loader);
        lottie.setAnimation("loader.json");
        lottie.playAnimation();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
} 