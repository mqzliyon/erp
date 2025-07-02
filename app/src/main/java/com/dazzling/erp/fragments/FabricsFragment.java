package com.dazzling.erp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.Fabric;
import com.dazzling.erp.models.Transfer;
import com.dazzling.erp.models.Cutting;
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
import com.airbnb.lottie.LottieAnimationView;

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
    private LottieAnimationView lottieLoading;

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
        lottieLoading = view.findViewById(R.id.lottie_loading);
        fabricsList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fabricsAdapter = new FabricsAdapter(fabricsList);
        recyclerView.setAdapter(fabricsAdapter);
        fabricsAdapter.setOnFabricMenuClickListener(new FabricsAdapter.OnFabricMenuClickListener() {
            @Override
            public void onView(Fabric fabric) {
                showFabricDetailsDialog(fabric);
            }
            @Override
            public void onEdit(Fabric fabric) {
                showUpdateFabricDialog(fabric);
            }
            @Override
            public void onTransfer(Fabric fabric) {
                showTransferDialog(fabric);
            }
            @Override
            public void onDelete(Fabric fabric) {
                showDeleteConfirmationDialog(fabric);
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            showAddFabricDialog();
        });
    }

    private void showLoading(boolean show) {
        if (lottieLoading != null) {
            lottieLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                lottieLoading.playAnimation();
            } else {
                lottieLoading.pauseAnimation();
            }
        }
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
            showLoading(true);
            firestoreService.addFabric(fabric, new FirestoreService.FabricCallback() {
                @Override
                public void onFabricsLoaded(List<Fabric> fabrics) {}
                @Override
                public void onFabricAdded(Fabric fabric) {
                    showLoading(false);
                    Snackbar.make(requireView(), "Fabric added!", Snackbar.LENGTH_SHORT).show();
                    loadFabrics();
                }
                @Override
                public void onFabricUpdated(Fabric updatedFabric) {
                    showLoading(false);
                    Snackbar.make(requireView(), "Fabric updated!", Snackbar.LENGTH_SHORT).show();
                }
                @Override
                public void onFabricDeleted(String fabricId) {}
                @Override
                public void onError(String error) {
                    showLoading(false);
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
            @Override public void onFabricUpdated(Fabric updatedFabric) {}
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

    private void showFabricDetailsDialog(Fabric fabric) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
        StringBuilder message = new StringBuilder();
        message.append("Type: ").append(fabric.getFabricType()).append("\n");
        message.append("Quantity: ").append(fabric.getQuantityKg()).append(" kg\n");
        if (fabric.getCreatedAt() != null) {
            message.append("Created: ").append(sdf.format(fabric.getCreatedAt())).append("\n");
        }
        if (fabric.getUpdatedAt() != null && fabric.getCreatedAt() != null && !fabric.getUpdatedAt().equals(fabric.getCreatedAt())) {
            message.append("Updated: ").append(sdf.format(fabric.getUpdatedAt())).append("\n");
        }
        // Show transfer info if available
        if (fabric.getTransferHistory() != null && !fabric.getTransferHistory().isEmpty()) {
            message.append("Transfer/Return History:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            for (Transfer t : fabric.getTransferHistory()) {
                String type = (t.getNote() != null && t.getNote().toLowerCase().contains("return")) ? "Return" : "Transfer";
                message.append("- ")
                    .append(type).append(": ")
                    .append(dateFormat.format(t.getDate())).append(" ")
                    .append(timeFormat.format(t.getDate())).append(", ")
                    .append("Qty: ").append(t.getQuantity()).append(" kg\n");
            }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Fabric Details")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showUpdateFabricDialog(Fabric fabric) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_fabric, null);
        final EditText editFabricType = dialogView.findViewById(R.id.edit_fabric_type);
        final EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        final EditText editDate = dialogView.findViewById(R.id.edit_date);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Pre-fill fields
        editFabricType.setText(fabric.getFabricType());
        editQuantity.setText(String.valueOf(fabric.getQuantityKg()));
        if (fabric.getCreatedAt() != null) {
            editDate.setText(dateFormat.format(fabric.getCreatedAt()));
            calendar.setTime(fabric.getCreatedAt());
        }

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
                .setTitle("Update Fabric")
                .setView(dialogView)
                .setPositiveButton("Update", null)
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
            // Update Fabric object
            fabric.setFabricType(fabricType);
            fabric.setQuantityKg(quantity);
            fabric.setCreatedAt(date);
            // Update in Firestore
            showLoading(true);
            firestoreService.updateFabric(fabric, new FirestoreService.FabricCallback() {
                @Override
                public void onFabricsLoaded(List<Fabric> fabrics) {}
                @Override
                public void onFabricAdded(Fabric fabric) {}
                @Override
                public void onFabricUpdated(Fabric updatedFabric) {
                    showLoading(false);
                    Snackbar.make(requireView(), "Fabric updated!", Snackbar.LENGTH_SHORT).show();
                }
                @Override
                public void onFabricDeleted(String fabricId) {}
                @Override
                public void onError(String error) {
                    showLoading(false);
                    Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
                }
            });
            // Dismiss dialog if successful
            dialog.dismiss();
        });
    }

    private void showDeleteConfirmationDialog(Fabric fabric) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Fabric")
                .setMessage("Are you sure you want to delete this fabric?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    showLoading(true);
                    firestoreService.deleteFabric(fabric.getId(), new FirestoreService.FabricCallback() {
                        @Override
                        public void onFabricsLoaded(List<Fabric> fabrics) {}
                        @Override
                        public void onFabricAdded(Fabric fabric) {}
                        @Override
                        public void onFabricUpdated(Fabric fabric) {}
                        @Override
                        public void onFabricDeleted(String fabricId) {
                            showLoading(false);
                            if (isAdded()) {
                                Snackbar.make(requireView(), "Fabric deleted!", Snackbar.LENGTH_SHORT).show();
                                loadFabrics();
                            }
                        }
                        @Override
                        public void onError(String error) {
                            showLoading(false);
                            if (isAdded()) {
                                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTransferDialog(Fabric fabric) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transfer_fabric, null);
        EditText editQuantity = dialogView.findViewById(R.id.edit_transfer_quantity);
        TextView textCurrent = dialogView.findViewById(R.id.text_current_quantity);
        EditText editDate = dialogView.findViewById(R.id.edit_transfer_date);

        double currentQuantity = fabric.getQuantityKg();
        textCurrent.setText("Current quantity: " + currentQuantity + " kg");

        final Calendar[] selectedDate = {Calendar.getInstance()};
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, year, month, day) -> {
                selectedDate[0].set(year, month, day);
                String dateStr = String.format(Locale.getDefault(), "%02d-%02d-%04d", day, month+1, year);
                editDate.setText(dateStr);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(getContext())
            .setTitle("Transfer For Cutting")
            .setView(dialogView)
            .setPositiveButton("OK", (dialog, which) -> {
                String qtyStr = editQuantity.getText().toString().trim();
                double transferQty;
                try {
                    transferQty = Double.parseDouble(qtyStr);
                } catch (NumberFormatException e) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Enter a valid quantity", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (transferQty > currentQuantity) {
                    new AlertDialog.Builder(getContext())
                        .setTitle("Invalid Quantity")
                        .setMessage("Cannot transfer more than current quantity (" + currentQuantity + " kg)")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }
                // Save transfer info
                fabric.setLastTransferQuantity(transferQty);
                fabric.setLastTransferDate(selectedDate[0].getTime());
                // Add to transfer history
                List<Transfer> history = fabric.getTransferHistory();
                if (history == null) history = new ArrayList<>();
                history.add(new Transfer(transferQty, selectedDate[0].getTime(), null));
                fabric.setTransferHistory(history);
                // Subtract transfer quantity from fabric quantity
                double newQuantity = fabric.getQuantityKg() - transferQty;
                if (newQuantity < 0) newQuantity = 0;
                fabric.setQuantityKg(newQuantity);
                // Update fabric in Firestore
                showLoading(true);
                firestoreService.updateFabric(fabric, new FirestoreService.FabricCallback() {
                    @Override
                    public void onFabricsLoaded(List<Fabric> fabrics) {}
                    @Override
                    public void onFabricAdded(Fabric fabric) {}
                    @Override
                    public void onFabricUpdated(Fabric fabric) {
                        showLoading(false);
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Transfer info saved", Toast.LENGTH_SHORT).show();
                        }
                        // Automatically add to cutting section
                        Cutting cutting = new Cutting();
                        cutting.setFabricType(fabric.getFabricType());
                        cutting.setQuantityKg(transferQty);
                        cutting.setCreatedAt(selectedDate[0].getTime());
                        // Optionally set more fields (color, lotNumber, etc.)
                        showLoading(true);
                        firestoreService.addCutting(cutting, new FirestoreService.CuttingCallback() {
                            @Override
                            public void onCuttingsLoaded(List<Cutting> cuttings) {}
                            @Override
                            public void onCuttingAdded(Cutting cutting) {
                                showLoading(false);
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Transferred to Cutting section", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCuttingUpdated(Cutting cutting) {}
                            @Override
                            public void onCuttingDeleted(String cuttingId) {}
                            @Override
                            public void onError(String error) {
                                showLoading(false);
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to add to Cutting: " + error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFabricDeleted(String fabricId) {}
                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to save transfer info: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 