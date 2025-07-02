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

/**
 * Fragment for managing cutting operations
 * Displays list of cutting operations and allows CRUD operations
 */
public class CuttingFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private List<Cutting> cuttingList;
    private FirestoreService firestoreService;
    private CuttingAdapter cuttingAdapter;
    private LottieAnimationView lottieLoading;

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
        lottieLoading = view.findViewById(R.id.lottie_loading);
        cuttingList = new ArrayList<>();
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
        firestoreService.getCuttings(new FirestoreService.CuttingCallback() {
            @Override
            public void onCuttingsLoaded(List<Cutting> cuttings) {
                cuttingList.clear();
                cuttingList.addAll(cuttings);
                cuttingAdapter.notifyDataSetChanged();
                if (cuttingList.isEmpty()) {
                    showEmptyState();
                } else {
                    showCuttingList();
                }
            }
            @Override public void onCuttingAdded(Cutting cutting) {}
            @Override public void onCuttingUpdated(Cutting cutting) {}
            @Override public void onCuttingDeleted(String cuttingId) {}
            @Override public void onError(String error) {}
        });
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
            Cutting cutting = cuttingList.get(position);
            // Show pcs if available, otherwise kg
            if (cutting.getQuantityPcs() > 0) {
                holder.quantity.setText(String.format("%.0f pcs", cutting.getQuantityPcs()));
            } else {
                holder.quantity.setText(String.format("%.2f kg", cutting.getQuantityKg()));
            }
            holder.fabricType.setText(cutting.getFabricType());
            holder.date.setText("Date: " + (cutting.getCreatedAt() != null ? dateFormat.format(cutting.getCreatedAt()) : ""));

            // View mode on item click
            holder.itemView.setOnClickListener(v -> {
                fragment.showCuttingViewDialog(v, cutting);
            });

            // 3-dot menu
            holder.menu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.menu_cutting_item, popup.getMenu());
                // Add Update option if converted
                if (cutting.getQuantityPcs() > 0) {
                    popup.getMenu().add("Update");
                }
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        fragment.showDeleteCuttingDialog(v, cutting);
                        return true;
                    } else if ("Update".equals(item.getTitle())) {
                        fragment.showUpdateCuttingDialog(cutting);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });

            // Sync icon click: show convert dialog
            holder.sync.setOnClickListener(v -> {
                fragment.showConvertCuttingDialog(cutting);
            });
        }
        @Override
        public int getItemCount() {
            return cuttingList.size();
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
} 