package com.dazzling.erp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import android.util.Log;

/**
 * Fragment for managing cutting operations
 * Displays list of cutting operations and allows CRUD operations
 */
public class CuttingFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private List<Cutting> cuttingList;
    private List<com.dazzling.erp.models.Fabric> fabricList;
    private FirestoreService firestoreService;
    private CuttingAdapter cuttingAdapter;
    private LottieAnimationView lottieLoading;
    private boolean isFragmentDestroyed = false; // Flag to prevent operations after destruction

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cutting, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadCuttingOperations();

        // FAB: Send to Lot
        FloatingActionButton fabAddCutting = view.findViewById(R.id.fab_add_cutting);
        fabAddCutting.setOnClickListener(v -> showSendToLotDialog());
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_cutting);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        lottieLoading = view.findViewById(R.id.lottie_loading);
        cuttingList = new ArrayList<>();
        fabricList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cuttingAdapter = new CuttingAdapter(this, cuttingList);
        recyclerView.setAdapter(cuttingAdapter);
    }

    private void setupListeners() {
        // Removed fabAdd.setOnClickListener
    }

    private void loadCuttingOperations() {
        if (!isAdded() || getActivity() == null || isFragmentDestroyed) {
            return;
        }
        
        firestoreService.getCuttings(new FirestoreService.CuttingCallback() {
            @Override
            public void onCuttingsLoaded(List<Cutting> cuttings) {
                if (!isAdded() || getActivity() == null || cuttingList == null || isFragmentDestroyed) {
                    return;
                }
                
                try {
                    cuttingList.clear();
                    if (cuttings != null) {
                        cuttingList.addAll(cuttings);
                    }
                    
                    if (cuttingAdapter != null) {
                        cuttingAdapter.notifyDataSetChanged();
                    }
                    
                    if (cuttingList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showCuttingList();
                    }
                } catch (Exception e) {
                    Log.e("CuttingFragment", "Error updating cutting list", e);
                }
            }
            @Override public void onCuttingAdded(Cutting cutting) {}
            @Override public void onCuttingUpdated(Cutting cutting) {}
            @Override public void onCuttingDeleted(String cuttingId) {}
            @Override public void onError(String error) {
                if (isAdded() && !isFragmentDestroyed) {
                    Log.e("CuttingFragment", "Error loading cuttings: " + error);
                }
            }
        });
    }

    private void showEmptyState() {
        if (!isAdded() || recyclerView == null || emptyStateText == null || isFragmentDestroyed) {
            return;
        }
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("No cutting operations found.\nTap + to add a new operation.");
    }

    private void showCuttingList() {
        if (!isAdded() || recyclerView == null || emptyStateText == null || isFragmentDestroyed) {
            return;
        }
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        if (!isAdded() || lottieLoading == null || isFragmentDestroyed) {
            return;
        }
        
        lottieLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            lottieLoading.playAnimation();
        } else {
            lottieLoading.pauseAnimation();
        }
    }

    private static class CuttingAdapter extends RecyclerView.Adapter<CuttingAdapter.ViewHolder> {
        private final List<Cutting> cuttingList;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
        private final CuttingFragment fragment;
        public CuttingAdapter(CuttingFragment fragment, List<Cutting> cuttingList) {
            this.fragment = fragment;
            this.cuttingList = cuttingList;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cutting, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (cuttingList == null || position >= cuttingList.size()) {
                return;
            }
            
            Cutting cutting = cuttingList.get(position);
            if (cutting == null) {
                return;
            }
            
            // Compact display for small lists
            if (cutting.getQuantityPcs() > 0) {
                holder.quantity.setText(String.format("%.0f pcs", cutting.getQuantityPcs()));
            } else {
                holder.quantity.setText(String.format("%.1f kg", cutting.getQuantityKg()));
            }
            
            // Compact fabric type display
            String fabricType = cutting.getFabricType();
            if (fabricType != null && fabricType.length() > 20) {
                fabricType = fabricType.substring(0, 17) + "...";
            }
            holder.fabricType.setText(fabricType);
            
            // Compact date display
            String dateText = cutting.getCreatedAt() != null ? dateFormat.format(cutting.getCreatedAt()) : "";
            holder.date.setText(dateText);

            // View mode on item click
            holder.itemView.setOnClickListener(v -> {
                if (fragment.isAdded()) {
                    fragment.showCuttingViewDialog(v, cutting);
                }
            });

            // 3-dot menu for small cutting lists
            holder.menu.setOnClickListener(v -> {
                if (!fragment.isAdded()) return;
                
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.menu_cutting_item, popup.getMenu());
                
                // Dynamically show/hide menu items based on cutting state
                android.view.MenuItem convertItem = popup.getMenu().findItem(R.id.action_convert);
                if (convertItem != null) {
                    if (cutting.getQuantityPcs() > 0) {
                        convertItem.setTitle("Update Pcs");
                        convertItem.setIcon(android.R.drawable.ic_menu_edit);
                    } else {
                        convertItem.setTitle("Convert to Pcs");
                        convertItem.setIcon(android.R.drawable.ic_menu_rotate);
                    }
                }
                
                popup.setOnMenuItemClickListener(item -> {
                    if (!fragment.isAdded()) return false;
                    
                    if (item.getItemId() == R.id.action_view) {
                        fragment.showCuttingViewDialog(v, cutting);
                        return true;
                    } else if (item.getItemId() == R.id.action_edit) {
                        fragment.showEditCuttingDialog(cutting);
                        return true;
                    } else if (item.getItemId() == R.id.action_convert) {
                        if (cutting.getQuantityPcs() > 0) {
                            fragment.showUpdateCuttingDialog(cutting);
                        } else {
                            fragment.showConvertCuttingDialog(cutting);
                        }
                        return true;
                    } else if (item.getItemId() == R.id.action_delete) {
                        fragment.showDeleteCuttingDialog(v, cutting);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });

            // Sync icon click: show convert dialog
            holder.sync.setOnClickListener(v -> {
                if (fragment.isAdded()) {
                    fragment.showConvertCuttingDialog(cutting);
                }
            });
        }
        @Override
        public int getItemCount() {
            return cuttingList != null ? cuttingList.size() : 0;
        }
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView fabricType, quantity, date;
            ImageView menu;
            ImageView sync;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.image_cutting_icon);
                fabricType = itemView.findViewById(R.id.text_cutting_fabric_type);
                quantity = itemView.findViewById(R.id.text_cutting_quantity);
                date = itemView.findViewById(R.id.text_cutting_date);
                menu = itemView.findViewById(R.id.image_cutting_menu);
                sync = itemView.findViewById(R.id.image_cutting_sync);
            }
        }
    }

    private void showCuttingViewDialog(View anchor, Cutting cutting) {
        if (isAdded()) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Cutting Details");
            StringBuilder message = new StringBuilder();
            message.append("Fabric Type: ").append(cutting.getFabricType() != null ? cutting.getFabricType() : "").append("\n");
            message.append("Reference: ").append(cutting.getFabricType() != null ? cutting.getFabricType() : "").append("\n");
            message.append("Date: ").append(cutting.getCreatedAt() != null ? new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a").format(cutting.getCreatedAt()) : "").append("\n");
            if (cutting.getQuantityPcs() > 0) {
                message.append("Current Quantity: ").append(String.format("%.0f pcs", cutting.getQuantityPcs())).append("\n");
                Double originalKg = cutting.getOriginalQuantityKg();
                if (originalKg != null) {
                    message.append("Original Quantity: ").append(String.format("%.2f kg", originalKg)).append("\n");
                    if (originalKg > 0) {
                        double pcsPerKg = cutting.getQuantityPcs() / originalKg;
                        message.append("Pcs per kg: ").append(String.format("%.2f", pcsPerKg)).append("\n");
                    }
                }
            } else {
                message.append("Quantity: ").append(String.format("%.2f kg", cutting.getQuantityKg()));
            }
            builder.setMessage(message.toString());
            builder.setPositiveButton("OK", null);
            builder.show();
        }
    }

    private void showDeleteCuttingDialog(View anchor, Cutting cutting) {
        if (!isAdded()) return;
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Cutting")
            .setMessage("Are you sure you want to delete this cutting entry? This will return the quantity to the fabric stock.")
            .setPositiveButton("Delete", (dialog, which) -> {
                showLoading(true);
                firestoreService.deleteCutting(cutting.getId(), new FirestoreService.CuttingCallback() {
                    @Override
                    public void onCuttingDeleted(String cuttingId) {
                        // Remove from local list and update UI
                        for (int i = 0; i < cuttingList.size(); i++) {
                            if (cuttingList.get(i).getId().equals(cuttingId)) {
                                cuttingList.remove(i);
                                cuttingAdapter.notifyItemRemoved(i);
                                break;
                            }
                        }
                        double qty = cutting.getOriginalQuantityKg() != null ? cutting.getOriginalQuantityKg() : cutting.getQuantityKg();
                        String fabricType = cutting.getFabricType();
                        String color = cutting.getColor();
                        String lotNumber = cutting.getLotNumber();
                        firestoreService.returnQuantityToFabric(fabricType, color, lotNumber, qty, new FirestoreService.FabricCallback() {
                            @Override public void onFabricsLoaded(java.util.List<com.dazzling.erp.models.Fabric> fabrics) {}
                            @Override public void onFabricAdded(com.dazzling.erp.models.Fabric fabric) {}
                            @Override public void onFabricUpdated(com.dazzling.erp.models.Fabric fabric) {
                                showLoading(false);
                                if (isAdded()) {
                                    android.widget.Toast.makeText(requireContext(), "Quantity returned to fabric stock.", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override public void onFabricDeleted(String fabricId) {}
                            @Override public void onError(String error) {
                                showLoading(false);
                                if (isAdded()) {
                                    android.widget.Toast.makeText(requireContext(), "Failed to return quantity: " + error, android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        if (cuttingList.isEmpty()) showEmptyState();
                    }
                    @Override public void onCuttingsLoaded(java.util.List<com.dazzling.erp.models.Cutting> cuttings) {}
                    @Override public void onCuttingAdded(com.dazzling.erp.models.Cutting cutting) {}
                    @Override public void onCuttingUpdated(com.dazzling.erp.models.Cutting cutting) {}
                    @Override public void onError(String error) {
                        showLoading(false);
                        if (isAdded()) {
                            android.widget.Toast.makeText(requireContext(), "Failed to delete cutting entry.", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public void showConvertCuttingDialog(Cutting cutting) {
        if (!isAdded()) return;

        // Prevent double conversion
        if (cutting.getQuantityKg() == 0) {
            android.widget.Toast.makeText(requireContext(), "Already converted full quantity", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Convert Cutting");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        // Quantity (disabled, in kg)
        final android.widget.EditText etQuantityKg = new android.widget.EditText(requireContext());
        etQuantityKg.setHint("Quantity (kg)");
        etQuantityKg.setText(String.format("%.2f", cutting.getQuantityKg()));
        etQuantityKg.setEnabled(false);
        layout.addView(etQuantityKg);

        // Quantity in pcs (editable)
        final android.widget.EditText etQuantityPcs = new android.widget.EditText(requireContext());
        etQuantityPcs.setHint("Quantity (pcs)");
        etQuantityPcs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etQuantityPcs);

        builder.setView(layout);
        builder.setPositiveButton("Convert", (dialog, which) -> {
            String pcsValue = etQuantityPcs.getText().toString();
            if (!pcsValue.isEmpty()) {
                try {
                    showLoading(true); // Show loading animation
                    double pcs = Double.parseDouble(pcsValue);
                    // Set originalQuantityKg only if not set
                    if (cutting.getOriginalQuantityKg() == null) {
                        cutting.setOriginalQuantityKg(cutting.getQuantityKg());
                    }
                    cutting.setQuantityPcs(pcs);
                    cutting.setQuantityKg(0); // Remove kg from list view
                    firestoreService.updateCutting(cutting, new FirestoreService.CuttingCallback() {
                        @Override public void onCuttingUpdated(com.dazzling.erp.models.Cutting updated) {
                            cuttingAdapter.notifyDataSetChanged();
                            android.widget.Toast.makeText(requireContext(), "Conversion successful!", android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onError(String error) {
                            android.widget.Toast.makeText(requireContext(), "Error: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onCuttingDeleted(String cuttingId) {}
                        @Override public void onCuttingsLoaded(java.util.List<com.dazzling.erp.models.Cutting> cuttings) {}
                        @Override public void onCuttingAdded(com.dazzling.erp.models.Cutting cutting) {}
                    });
                } catch (NumberFormatException ignored) {
                    showLoading(false); // Hide loading animation on error
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void showUpdateCuttingDialog(Cutting cutting) {
        if (!isAdded()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Update Cutting");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        // Current Quantity (pcs, disabled)
        final android.widget.EditText etCurrentPcs = new android.widget.EditText(requireContext());
        etCurrentPcs.setHint("Current Quantity (pcs)");
        etCurrentPcs.setText(String.format("%.0f", cutting.getQuantityPcs()));
        etCurrentPcs.setEnabled(false);
        layout.addView(etCurrentPcs);

        // Update Quantity (pcs, editable)
        final android.widget.EditText etUpdatePcs = new android.widget.EditText(requireContext());
        etUpdatePcs.setHint("Update Quantity (pcs)");
        etUpdatePcs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etUpdatePcs);

        // Date field (default to today)
        final android.widget.EditText etDate = new android.widget.EditText(requireContext());
        etDate.setHint("Date");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
        etDate.setText(sdf.format(new java.util.Date()));
        layout.addView(etDate);

        builder.setView(layout);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String updatePcsValue = etUpdatePcs.getText().toString();
            String dateValue = etDate.getText().toString();
            if (!updatePcsValue.isEmpty()) {
                try {
                    showLoading(true);
                    double newPcs = Double.parseDouble(updatePcsValue);
                    cutting.setQuantityPcs(newPcs);
                    // Optionally, you can store the date somewhere if needed
                    firestoreService.updateCutting(cutting, new FirestoreService.CuttingCallback() {
                        @Override public void onCuttingUpdated(com.dazzling.erp.models.Cutting updated) {
                            cuttingAdapter.notifyDataSetChanged();
                            android.widget.Toast.makeText(requireContext(), "Update successful!", android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onError(String error) {
                            android.widget.Toast.makeText(requireContext(), "Error: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onCuttingDeleted(String cuttingId) {}
                        @Override public void onCuttingsLoaded(java.util.List<com.dazzling.erp.models.Cutting> cuttings) {}
                        @Override public void onCuttingAdded(com.dazzling.erp.models.Cutting cutting) {}
                    });
                } catch (NumberFormatException ignored) {
                    showLoading(false);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void showEditCuttingDialog(Cutting cutting) {
        if (!isAdded()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Cutting");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        // Fabric Type (editable)
        final android.widget.EditText etFabricType = new android.widget.EditText(requireContext());
        etFabricType.setHint("Fabric Type");
        etFabricType.setText(cutting.getFabricType());
        layout.addView(etFabricType);

        // Color (editable)
        final android.widget.EditText etColor = new android.widget.EditText(requireContext());
        etColor.setHint("Color");
        etColor.setText(cutting.getColor());
        layout.addView(etColor);

        // Lot Number (editable)
        final android.widget.EditText etLotNumber = new android.widget.EditText(requireContext());
        etLotNumber.setHint("Lot Number");
        etLotNumber.setText(cutting.getLotNumber());
        layout.addView(etLotNumber);

        // Quantity (editable)
        final android.widget.EditText etQuantity = new android.widget.EditText(requireContext());
        etQuantity.setHint("Quantity");
        if (cutting.getQuantityPcs() > 0) {
            etQuantity.setText(String.format("%.0f", cutting.getQuantityPcs()));
            etQuantity.setHint("Quantity (pcs)");
        } else {
            etQuantity.setText(String.format("%.2f", cutting.getQuantityKg()));
            etQuantity.setHint("Quantity (kg)");
        }
        etQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etQuantity);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String fabricType = etFabricType.getText().toString();
            String color = etColor.getText().toString();
            String lotNumber = etLotNumber.getText().toString();
            String quantityValue = etQuantity.getText().toString();

            if (!fabricType.isEmpty() && !quantityValue.isEmpty()) {
                try {
                    showLoading(true);
                    cutting.setFabricType(fabricType);
                    cutting.setColor(color);
                    cutting.setLotNumber(lotNumber);
                    
                    double quantity = Double.parseDouble(quantityValue);
                    if (cutting.getQuantityPcs() > 0) {
                        cutting.setQuantityPcs(quantity);
                        cutting.setQuantityKg(0);
                    } else {
                        cutting.setQuantityKg(quantity);
                        cutting.setQuantityPcs(0);
                    }

                    firestoreService.updateCutting(cutting, new FirestoreService.CuttingCallback() {
                        @Override public void onCuttingUpdated(com.dazzling.erp.models.Cutting updated) {
                            cuttingAdapter.notifyDataSetChanged();
                            android.widget.Toast.makeText(requireContext(), "Cutting updated successfully!", android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onError(String error) {
                            android.widget.Toast.makeText(requireContext(), "Error: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                        @Override public void onCuttingDeleted(String cuttingId) {}
                        @Override public void onCuttingsLoaded(java.util.List<com.dazzling.erp.models.Cutting> cuttings) {}
                        @Override public void onCuttingAdded(com.dazzling.erp.models.Cutting cutting) {}
                    });
                } catch (NumberFormatException e) {
                    android.widget.Toast.makeText(requireContext(), "Please enter a valid quantity", android.widget.Toast.LENGTH_SHORT).show();
                    showLoading(false);
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Please fill all required fields", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSendToLotDialog() {
        // Prevent multiple dialogs from opening
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_send_to_lot, null);
        
        // Create dialog first so it's accessible in callbacks
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Prevent dialog from being dismissed during operations
        dialog.setCanceledOnTouchOutside(false);
        
        final com.google.android.material.textfield.TextInputLayout layoutLotNumber = dialogView.findViewById(R.id.layout_lot_number);
        final android.widget.AutoCompleteTextView inputLotNumber = dialogView.findViewById(R.id.input_lot_number);
        final com.google.android.material.textfield.TextInputLayout layoutFabricType = dialogView.findViewById(R.id.layout_fabric_type);
        final android.widget.AutoCompleteTextView inputFabricType = dialogView.findViewById(R.id.input_fabric_type);
        final TextView textFabricQuantity = dialogView.findViewById(R.id.text_fabric_quantity);
        final com.google.android.material.textfield.TextInputEditText inputSendQuantity = dialogView.findViewById(R.id.input_send_quantity);
        final com.google.android.material.textfield.TextInputEditText inputDate = dialogView.findViewById(R.id.input_date);
        final com.google.android.material.button.MaterialButton buttonSend = dialogView.findViewById(R.id.button_send);

        // Validate UI elements exist
        if (layoutLotNumber == null || inputLotNumber == null || layoutFabricType == null || 
            inputFabricType == null || textFabricQuantity == null || inputSendQuantity == null || 
            inputDate == null || buttonSend == null) {
            android.widget.Toast.makeText(requireContext(), "Error initializing dialog", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up date picker and default to today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        inputDate.setText(dateFormat.format(calendar.getTime()));
        
        // Date picker
        inputDate.setOnClickListener(v -> {
            if (!isAdded()) return;
            
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
            
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(requireContext(), (view, y, m, d) -> {
                java.util.Calendar selected = java.util.Calendar.getInstance();
                selected.set(y, m, d);
                inputDate.setText(dateFormat.format(selected.getTime()));
            }, year, month, day);
            datePickerDialog.show();
        });

        // Load lot numbers for dropdown
        firestoreService.getLots(new FirestoreService.LotCallback() {
            @Override
            public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {
                if (!isAdded()) return;
                
                java.util.Set<String> lotNumbers = new java.util.HashSet<>();
                for (com.dazzling.erp.models.Lot lot : lots) {
                    if (lot.getLotNumber() != null && !lot.getLotNumber().isEmpty()) {
                        lotNumbers.add(lot.getLotNumber());
                    }
                }
                java.util.List<String> lotNumberOptions = new java.util.ArrayList<>(lotNumbers);
                android.widget.ArrayAdapter<String> lotNumberAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, lotNumberOptions);
                inputLotNumber.setAdapter(lotNumberAdapter);
            }
            @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
            @Override public void onLotUpdated(com.dazzling.erp.models.Lot lot) {}
            @Override public void onLotDeleted(String lotId) {}
            @Override public void onError(String error) {
                if (isAdded()) {
                    android.widget.Toast.makeText(requireContext(), "Failed to load lots: " + error, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load fabric types from cutting list
        java.util.Set<String> cuttingFabricTypes = new java.util.HashSet<>();
        if (cuttingList != null) {
            for (Cutting c : cuttingList) {
                if (c.getFabricType() != null && !c.getFabricType().isEmpty()) {
                    cuttingFabricTypes.add(c.getFabricType());
                }
            }
        }
        java.util.List<String> fabricTypeOptions = new java.util.ArrayList<>(cuttingFabricTypes);
        android.widget.ArrayAdapter<String> fabricTypeAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, fabricTypeOptions);
        inputFabricType.setAdapter(fabricTypeAdapter);

        // Show available pcs for selected fabric type from cuttingList
        inputFabricType.setOnItemClickListener((parent, view, position, id) -> {
            if (!isAdded()) return;
            
            String selectedFabricType = (String) parent.getItemAtPosition(position);
            if (selectedFabricType == null) return;
            
            int totalPcs = 0;
            if (cuttingList != null) {
                for (Cutting c : cuttingList) {
                    if (selectedFabricType.equals(c.getFabricType())) {
                        totalPcs += (int) c.getQuantityPcs();
                    }
                }
            }
            
            // Show red alert if quantity is 0 pcs
            if (totalPcs == 0) {
                textFabricQuantity.setTextColor(android.graphics.Color.RED);
                textFabricQuantity.setText("⚠️ Available: 0 pcs - Cannot send!");
            } else {
                textFabricQuantity.setTextColor(android.graphics.Color.BLACK);
                textFabricQuantity.setText("Available: " + totalPcs + " pcs");
            }
        });

        // Flag to prevent multiple simultaneous operations
        final java.util.concurrent.atomic.AtomicBoolean isProcessing = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Validate and send to lot
        buttonSend.setOnClickListener(v -> {
            // Prevent multiple clicks
            if (isProcessing.get() || !isAdded()) {
                return;
            }
            
            isProcessing.set(true);
            
            // Disable button to prevent multiple clicks
            buttonSend.setEnabled(false);
            
            // Show loading animation immediately
            showLoading(true);
            
            final String lotNumber = inputLotNumber.getText().toString().trim();
            final String fabricType = inputFabricType.getText().toString().trim();
            String sendQtyStr = inputSendQuantity.getText().toString().trim();
            String dateStr = inputDate.getText().toString().trim();
            boolean valid = true;
            layoutLotNumber.setError(null);
            layoutFabricType.setError(null);
            inputSendQuantity.setError(null);
            inputDate.setError(null);
            
            if (lotNumber.isEmpty()) {
                layoutLotNumber.setError("⚠️ Lot number is required!");
                valid = false;
            }
            if (fabricType.isEmpty()) {
                layoutFabricType.setError("⚠️ Fabric type is required!");
                valid = false;
            }
            
            // Parse sendQty with proper error handling
            int parsedSendQty = 0;
            if (sendQtyStr.isEmpty()) {
                inputSendQuantity.setError("⚠️ Quantity is required!");
                valid = false;
            } else {
                try {
                    parsedSendQty = Integer.parseInt(sendQtyStr);
                    if (parsedSendQty <= 0) {
                        inputSendQuantity.setError("⚠️ Quantity must be greater than 0!");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    inputSendQuantity.setError("Invalid number");
                    valid = false;
                }
            }
            final int sendQty = parsedSendQty; // Make it final for use in callbacks
            
            if (!valid) {
                showLoading(false);
                buttonSend.setEnabled(true);
                isProcessing.set(false);
                return;
            }
            
            // Validate send quantity against selected fabric
            int availablePcs = 0;
            if (cuttingList != null) {
                for (Cutting c : cuttingList) {
                    if (fabricType.equals(c.getFabricType())) {
                        availablePcs += (int) c.getQuantityPcs();
                    }
                }
            }
            
            if (sendQty > availablePcs) {
                showLoading(false);
                inputSendQuantity.setError("⚠️ Cannot send more than available (" + availablePcs + " pcs)!");
                buttonSend.setEnabled(true);
                isProcessing.set(false);
                return;
            }
            
            // Find available cuttings for this fabric type
            java.util.List<Cutting> availableCuttings = new java.util.ArrayList<>();
            if (cuttingList != null) {
                for (Cutting c : cuttingList) {
                    if (fabricType.equals(c.getFabricType()) && c.getQuantityPcs() > 0) {
                        availableCuttings.add(c);
                    }
                }
            }
            
            if (availableCuttings.isEmpty()) {
                showLoading(false);
                android.widget.Toast.makeText(requireContext(), "No cutting entries found for this fabric type!", android.widget.Toast.LENGTH_SHORT).show();
                buttonSend.setEnabled(true);
                isProcessing.set(false);
                return;
            }
            
            // Show success message after a short delay to indicate processing
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (isAdded()) {
                    android.widget.Toast.makeText(requireContext(), "Processing send operation...", android.widget.Toast.LENGTH_SHORT).show();
                }
            }, 500);
            
            // Calculate how much to take from each cutting entry
            int remainingQty = sendQty;
            java.util.List<Cutting> cuttingsToUpdate = new java.util.ArrayList<>();
            
            for (Cutting cutting : availableCuttings) {
                if (remainingQty <= 0) break;
                
                int availableInThisCutting = (int) cutting.getQuantityPcs();
                int toTakeFromThisCutting = Math.min(remainingQty, availableInThisCutting);
                
                // Create a copy of the cutting to update with all fields properly initialized
                Cutting updatedCutting = new Cutting();
                updatedCutting.setId(cutting.getId());
                updatedCutting.setLotNumber(cutting.getLotNumber());
                updatedCutting.setFabricType(cutting.getFabricType());
                updatedCutting.setColor(cutting.getColor());
                updatedCutting.setQuantityPcs(cutting.getQuantityPcs() - toTakeFromThisCutting);
                updatedCutting.setQuantityKg(cutting.getQuantityKg());
                updatedCutting.setOriginalQuantityKg(cutting.getOriginalQuantityKg());
                updatedCutting.setCuttingType(cutting.getCuttingType());
                updatedCutting.setOperator(cutting.getOperator());
                updatedCutting.setMachineId(cutting.getMachineId());
                updatedCutting.setQuality(cutting.getQuality());
                updatedCutting.setNotes(cutting.getNotes());
                updatedCutting.setStatus(cutting.getStatus());
                updatedCutting.setCreatedBy(cutting.getCreatedBy());
                updatedCutting.setUpdatedBy(cutting.getUpdatedBy());
                updatedCutting.setCreatedAt(cutting.getCreatedAt());
                updatedCutting.setUpdatedAt(new java.util.Date());
                updatedCutting.setCompletedAt(cutting.getCompletedAt());
                
                cuttingsToUpdate.add(updatedCutting);
                remainingQty -= toTakeFromThisCutting;
            }
            
            // Update cuttings one by one with proper error handling
            final java.util.concurrent.atomic.AtomicInteger completedUpdates = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.atomic.AtomicBoolean hasError = new java.util.concurrent.atomic.AtomicBoolean(false);
            
            for (Cutting cuttingToUpdate : cuttingsToUpdate) {
                firestoreService.updateCutting(cuttingToUpdate, new FirestoreService.CuttingCallback() {
                    @Override
                    public void onCuttingUpdated(Cutting updatedCutting) {
                        if (!isAdded()) return;
                        
                        int completed = completedUpdates.incrementAndGet();
                        if (completed == cuttingsToUpdate.size() && !hasError.get()) {
                            android.util.Log.d("CuttingFragment", "All cutting updates complete, now updating lot: " + lotNumber);
                            
                            // Show progress message
                            android.widget.Toast.makeText(requireContext(), "Updating lot information...", android.widget.Toast.LENGTH_SHORT).show();
                            
                            // Now update the Lot - use one-time query to avoid infinite callbacks
                            firestoreService.searchLotsByNumberOnce(lotNumber, new FirestoreService.LotCallback() {
                                @Override public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {
                                    if (!isAdded()) return;
                                    
                                    // Find the exact lot by lot number
                                    com.dazzling.erp.models.Lot targetLot = null;
                                    if (lots != null) {
                                        for (com.dazzling.erp.models.Lot lot : lots) {
                                            if (lotNumber.equals(lot.getLotNumber())) {
                                                targetLot = lot;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (targetLot == null) {
                                        showLoading(false);
                                        android.widget.Toast.makeText(requireContext(), "Lot not found! Please check the lot number.", android.widget.Toast.LENGTH_LONG).show();
                                        buttonSend.setEnabled(true);
                                        isProcessing.set(false);
                                        android.util.Log.e("CuttingFragment", "Lot not found: " + lotNumber);
                                        return;
                                    }
                                    
                                    // Update the lot's cutting quantity
                                    int newLotCuttingPcs = targetLot.getCuttingPcs() + sendQty;
                                    targetLot.setCuttingPcs(newLotCuttingPcs);
                                    android.util.Log.d("CuttingFragment", "Updating lot cutting pcs from " + (newLotCuttingPcs - sendQty) + " to " + newLotCuttingPcs);
                                    
                                    firestoreService.updateLot(targetLot, new FirestoreService.LotCallback() {
                                        @Override public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                                            if (!isAdded()) return;
                                            
                                            // Hide loading and show success message
                                            showLoading(false);
                                            
                                            // Show success message with more details
                                            String successMessage = String.format("✅ Successfully sent %d pcs to lot %s!", sendQty, lotNumber);
                                            android.widget.Toast.makeText(requireContext(), successMessage, android.widget.Toast.LENGTH_LONG).show();
                                            
                                            // Dismiss dialog and refresh
                                            dialog.dismiss();
                                            loadCuttingOperations();
                                            
                                            android.util.Log.d("CuttingFragment", "Successfully sent " + sendQty + " pcs to lot: " + lotNumber);
                                            isProcessing.set(false);
                                        }
                                        @Override public void onError(String error) {
                                            if (!isAdded()) return;
                                            
                                            showLoading(false);
                                            android.widget.Toast.makeText(requireContext(), "Failed to update lot: " + error, android.widget.Toast.LENGTH_LONG).show();
                                            buttonSend.setEnabled(true);
                                            isProcessing.set(false);
                                            android.util.Log.e("CuttingFragment", "Failed to update lot: " + error);
                                        }
                                        @Override public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                                        @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                                        @Override public void onLotDeleted(String lotId) {}
                                    });
                                }
                                @Override public void onError(String error) {
                                    if (!isAdded()) return;
                                    
                                    hasError.set(true);
                                    showLoading(false);
                                    android.widget.Toast.makeText(requireContext(), "Failed to find lot: " + error, android.widget.Toast.LENGTH_LONG).show();
                                    buttonSend.setEnabled(true);
                                    isProcessing.set(false);
                                    android.util.Log.e("CuttingFragment", "Failed to find lot: " + error);
                                }
                                @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                                @Override public void onLotUpdated(com.dazzling.erp.models.Lot lot) {}
                                @Override public void onLotDeleted(String lotId) {}
                            });
                        }
                    }
                    @Override public void onError(String error) {
                        if (!isAdded()) return;
                        
                        hasError.set(true);
                        showLoading(false);
                        android.widget.Toast.makeText(requireContext(), "Failed to update cutting: " + error, android.widget.Toast.LENGTH_LONG).show();
                        buttonSend.setEnabled(true);
                        isProcessing.set(false);
                        android.util.Log.e("CuttingFragment", "Error updating cutting: " + error);
                    }
                    @Override public void onCuttingDeleted(String cuttingId) {}
                    @Override public void onCuttingsLoaded(java.util.List<com.dazzling.erp.models.Cutting> cuttings) {}
                    @Override public void onCuttingAdded(com.dazzling.erp.models.Cutting cutting) {}
                });
            }
        });

        // Handle dialog dismissal
        dialog.setOnDismissListener(dialogInterface -> {
            isProcessing.set(false);
        });

        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        try {
            // Hide keyboard when fragment is paused
            if (getActivity() != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null && getView() != null) {
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                }
            }
            
            // Pause any ongoing animations or operations
            if (lottieLoading != null) {
                lottieLoading.pauseAnimation();
            }
        } catch (Exception e) {
            Log.e("CuttingFragment", "Error in onPause", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Mark fragment as destroyed to prevent operations
        isFragmentDestroyed = true;
        
        // Hide keyboard before destroying view
        if (getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        
        // Clean up references to prevent memory leaks
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        if (cuttingAdapter != null) {
            cuttingAdapter = null;
        }
        if (cuttingList != null) {
            cuttingList.clear();
            cuttingList = null;
        }
        if (fabricList != null) {
            fabricList.clear();
            fabricList = null;
        }
        if (firestoreService != null) {
            firestoreService = null;
        }
        if (lottieLoading != null) {
            lottieLoading = null;
        }
        if (emptyStateText != null) {
            emptyStateText = null;
        }
        
        Log.d("CuttingFragment", "Fragment destroyed and cleaned up");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("CuttingFragment", "Fragment onDestroy called");
    }
} 