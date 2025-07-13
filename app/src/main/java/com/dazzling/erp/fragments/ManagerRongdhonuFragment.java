package com.dazzling.erp.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.adapters.StockSummaryAdapter;
import com.dazzling.erp.models.StockSummary;
import com.dazzling.erp.services.FirebaseAuthService;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManagerRongdhonuFragment extends Fragment {
    
    // Menu tab data structure
    private static class MenuTab {
        String label;
        int iconRes;
        boolean hasBadge;
        public MenuTab(String label, int iconRes, boolean hasBadge) {
            this.label = label;
            this.iconRes = iconRes;
            this.hasBadge = hasBadge;
        }
    }

    private final MenuTab[] menuTabs = new MenuTab[] {
        new MenuTab("Polo", R.drawable.ic_polo, false),
        new MenuTab("T-Shirt", R.drawable.ic_tshirt, false),
        new MenuTab("Stripe Polo", R.drawable.ic_stripe_polo, false)
    };
    
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    
    // Menu variables
    private LinearLayout menuBar;
    private int selectedTab = 0; // Default selected tab
    private String currentSelectedTab = "Polo"; // Default selected tab
    
    // List variables
    private RecyclerView recyclerView;
    private StockSummaryAdapter adapter;
    private List<StockSummary> allStockSummaries = new ArrayList<>();
    private List<StockSummary> filteredStockSummaries = new ArrayList<>();
    
    // UI State variables
    private View loadingOverlay;
    private TextView loadingText;
    private View emptyState;
    private boolean isInitialLoad = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_rongdhonu, container, false);
        
            // Initialize services
    authService = new FirebaseAuthService();
    firestoreService = new FirestoreService();
        
        // Initialize date utilities
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Initialize views and setup data
        setupViews(view);
        
        return view;
    }
    
    private void setupViews(View view) {
        // Initialize menu bar
        menuBar = view.findViewById(R.id.menu_bar);
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Setup adapter
        adapter = new StockSummaryAdapter(getContext(), filteredStockSummaries);
        recyclerView.setAdapter(adapter);
        
        // Setup click listeners
        adapter.setOnItemClickListener(this::showStockSummaryDetails);
        adapter.setOnMenuClickListener(this::showPopupMenu);
        
        // Initialize UI state views
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        loadingText = view.findViewById(R.id.loading_text);
        emptyState = view.findViewById(R.id.empty_state);
        
        // Setup menu bar
        setupMenuBar();
        
        // Setup FloatingActionButton
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle plus icon click
                showStockSummaryDialog();
            }
        });
        
        // Load initial data from Firebase
        loadStockSummariesFromFirebase();
    }
    
    private void setupMenuBar() {
        menuBar.removeAllViews();
        for (int i = 0; i < menuTabs.length; i++) {
            final int index = i;
            MenuTab tab = menuTabs[i];
            LinearLayout tabView = createMenuTab(tab, i == selectedTab);
            tabView.setOnClickListener(v -> {
                if (selectedTab != index) {
                    animateTabSelection(index);
                }
            });
            menuBar.addView(tabView);
        }
    }
    
    // Create a single menu tab view
    private LinearLayout createMenuTab(MenuTab tab, boolean selected) {
        LinearLayout tabLayout = new LinearLayout(getContext());
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setGravity(Gravity.CENTER);
        int padH = dp(18);
        int padV = dp(8);
        tabLayout.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        tabLayout.setLayoutParams(lp);
        tabLayout.setClickable(true);
        tabLayout.setFocusable(true);
        setTabBackground(tabLayout, selected);
        tabLayout.setElevation(selected ? dp(6) : dp(0));

        // Icon
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(tab.iconRes);
        icon.setColorFilter(selected ? Color.WHITE : ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconLp.setMarginEnd(dp(8));
        icon.setLayoutParams(iconLp);
        tabLayout.addView(icon);

        // Label
        TextView label = new TextView(getContext());
        label.setText(tab.label);
        label.setTextSize(16);
        label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        label.setTextColor(selected ? Color.WHITE : ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        tabLayout.addView(label);

        // Badge (if needed)
        if (tab.hasBadge) {
            View badge = new View(getContext());
            int badgeSize = dp(10);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
            badgeLp.setMarginStart(dp(4));
            badge.setLayoutParams(badgeLp);
            badge.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_notification_dot));
            tabLayout.addView(badge);
        }

        // Ripple effect
        tabLayout.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ripple_effect));
        return tabLayout;
    }
    
    // Create pill-shaped background with gradient for selected, neutral for unselected
    private void setTabBackground(LinearLayout tabLayout, boolean selected) {
        ImageView icon = null;
        TextView label = null;
        // Find first ImageView and TextView in tabLayout
        for (int i = 0; i < tabLayout.getChildCount(); i++) {
            View child = tabLayout.getChildAt(i);
            if (icon == null && child instanceof ImageView) icon = (ImageView) child;
            if (label == null && child instanceof TextView) label = (TextView) child;
            if (icon != null && label != null) break;
        }
        if (selected) {
            android.graphics.drawable.GradientDrawable solid = new android.graphics.drawable.GradientDrawable();
            solid.setColor(android.graphics.Color.rgb(158, 231, 114));
            solid.setCornerRadius(dp(50));
            tabLayout.setBackground(solid);
            if (icon != null) icon.setColorFilter(android.graphics.Color.WHITE);
            if (label != null) {
                label.setTextColor(android.graphics.Color.WHITE);
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        } else {
            ShapeAppearanceModel shape = new ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, dp(50))
                    .build();
            MaterialShapeDrawable bg = new MaterialShapeDrawable(shape);
            bg.setFillColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.md_theme_surfaceVariant)));
            bg.setElevation(0);
            tabLayout.setBackground(bg);
            if (icon != null) icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }
    
    // Animate tab selection and update content
    private void animateTabSelection(int newIndex) {
        int oldIndex = selectedTab;
        selectedTab = newIndex;
        currentSelectedTab = menuTabs[newIndex].label;
        
        for (int i = 0; i < menuBar.getChildCount(); i++) {
            LinearLayout tabView = (LinearLayout) menuBar.getChildAt(i);
            boolean isSelected = (i == newIndex);
            setTabBackground(tabView, isSelected);
            tabView.setElevation(isSelected ? dp(6) : dp(0));
            tabView.animate().scaleX(isSelected ? 1.08f : 1f).scaleY(isSelected ? 1.08f : 1f).setDuration(180).start();
        }
        
        // Load data for selected tab
        loadTabData(currentSelectedTab);
    }
    
    private void loadStockSummariesFromFirebase() {
        // Show loading state
        showLoading("Loading stock summaries...");
        
        firestoreService.getStockSummariesByOffice("Rongdhonu", new FirestoreService.StockSummaryCallback() {
            @Override
            public void onStockSummariesLoaded(List<StockSummary> stockSummaries) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allStockSummaries.clear();
                        allStockSummaries.addAll(stockSummaries);
                        loadTabData(currentSelectedTab);
                        
                        // Only hide loading on initial load, not on real-time updates
                        if (isInitialLoad) {
                            hideLoading();
                            isInitialLoad = false;
                        }
                    });
                }
            }

            @Override
            public void onStockSummaryAdded(StockSummary stockSummary) {
                // This will be handled by the real-time listener
            }

            @Override
            public void onStockSummaryUpdated(StockSummary stockSummary) {
                // This will be handled by the real-time listener
            }

            @Override
            public void onStockSummaryDeleted(String stockSummaryId) {
                // This will be handled by the real-time listener
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isInitialLoad) {
                            hideLoading();
                            isInitialLoad = false;
                        }
                        showError("Failed to load data. Please try again.");
                        // Load empty data to prevent crashes
                        allStockSummaries.clear();
                        loadTabData(currentSelectedTab);
                    });
                }
            }
        });
    }
    
    private void loadTabData(String productType) {
        // Filter stock summaries by product type
        filteredStockSummaries.clear();
        for (StockSummary summary : allStockSummaries) {
            if (summary.getProductType().equals(productType)) {
                filteredStockSummaries.add(summary);
            }
        }
        
        // Update adapter
        adapter.updateData(filteredStockSummaries);
        
        // Show/hide empty state
        if (filteredStockSummaries.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }
    
    private void showStockSummaryDetails(StockSummary stockSummary) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_stock_summary_details, null);
        
        // Set data
        TextView textProductType = dialogView.findViewById(R.id.text_product_type);
        TextView textDate = dialogView.findViewById(R.id.text_date);
        TextView textOffice = dialogView.findViewById(R.id.text_office);
        TextView textOpeningStock = dialogView.findViewById(R.id.text_opening_stock);
        TextView textReceiptFactory = dialogView.findViewById(R.id.text_receipt_factory);
        TextView textReturnProduct = dialogView.findViewById(R.id.text_return_product);
        TextView textTodaySale = dialogView.findViewById(R.id.text_today_sale);
        TextView textClosingStock = dialogView.findViewById(R.id.text_closing_stock);
        Button btnClose = dialogView.findViewById(R.id.btn_close);
        
        textProductType.setText(stockSummary.getProductType());
        textDate.setText(stockSummary.getDate());
        textOffice.setText(stockSummary.getOffice() != null ? stockSummary.getOffice() : "Unknown Office");
        textOpeningStock.setText(stockSummary.getOpeningStock() + " Pcs");
        textReceiptFactory.setText(stockSummary.getReceiptFromFactory() + " Pcs");
        textReturnProduct.setText(stockSummary.getReturnProduct() + " Pcs");
        textTodaySale.setText(stockSummary.getTodaySaleQuantity() + " Pcs");
        textClosingStock.setText(stockSummary.getClosingStock() + " Pcs");
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void showPopupMenu(View view, StockSummary stockSummary) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Delete");
        popup.getMenu().add("Edit");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete")) {
                deleteStockSummary(stockSummary);
                return true;
            } else if (item.getTitle().equals("Edit")) {
                editStockSummary(stockSummary);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void deleteStockSummary(StockSummary stockSummary) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Stock Summary")
            .setMessage("Are you sure you want to delete this stock summary?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete from Firebase
                firestoreService.deleteStockSummary(stockSummary.getId(), new FirestoreService.StockSummaryCallback() {
                    @Override
                    public void onStockSummariesLoaded(List<StockSummary> stockSummaries) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onStockSummaryAdded(StockSummary stockSummary) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onStockSummaryUpdated(StockSummary stockSummary) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onStockSummaryDeleted(String stockSummaryId) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showSuccess("Stock summary deleted successfully");
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showError("Failed to delete stock summary. Please try again.");
                            });
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void editStockSummary(StockSummary stockSummary) {
        // TODO: Implement edit functionality with Firebase
        Toast.makeText(getContext(), "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    // ==================== UI STATE MANAGEMENT ====================
    
    private void showLoading(String message) {
        if (loadingOverlay != null && loadingText != null) {
            loadingText.setText(message);
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }
    
    private void showEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }
    
    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
    
    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    // Helper method to get integer value or default to 0 if empty
    private int getIntValueOrDefault(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private void showStockSummaryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_stock_summary, null);
        
        // Initialize dialog views
        Spinner spinnerProductType = dialogView.findViewById(R.id.spinner_product_type);
        EditText etOffice = dialogView.findViewById(R.id.et_office);
        EditText etOpeningStock = dialogView.findViewById(R.id.et_opening_stock);
        EditText etReceiptFactory = dialogView.findViewById(R.id.et_receipt_factory);
        EditText etReturnProduct = dialogView.findViewById(R.id.et_return_product);
        EditText etTodaySale = dialogView.findViewById(R.id.et_today_sale);
        EditText etClosingStock = dialogView.findViewById(R.id.et_closing_stock);
        EditText etDate = dialogView.findViewById(R.id.et_date);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        
        // Setup product type spinner
        String[] productTypes = {"Polo", "T-Shirt", "Stripe Polo"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, productTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProductType.setAdapter(spinnerAdapter);
        
        // Set the current selected tab as default in spinner
        int defaultPosition = 0;
        for (int i = 0; i < productTypes.length; i++) {
            if (productTypes[i].equals(currentSelectedTab)) {
                defaultPosition = i;
                break;
            }
        }
        spinnerProductType.setSelection(defaultPosition);
        
        // Set current date as default
        etDate.setText(dateFormat.format(new Date()));
        
        // Set office automatically (disabled field)
        etOffice.setText("Rongdhonu");
        
        // Setup date picker
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etDate.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Auto-calculate closing stock when values change
        View.OnFocusChangeListener calculateListener = (v, hasFocus) -> {
            if (!hasFocus) {
                calculateClosingStock(etOpeningStock, etReceiptFactory, etReturnProduct, etTodaySale, etClosingStock);
            }
        };
        
        // Also calculate on text change for real-time updates
        android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                calculateClosingStock(etOpeningStock, etReceiptFactory, etReturnProduct, etTodaySale, etClosingStock);
            }
        };
        
        etOpeningStock.setOnFocusChangeListener(calculateListener);
        etOpeningStock.addTextChangedListener(textWatcher);
        etReceiptFactory.setOnFocusChangeListener(calculateListener);
        etReceiptFactory.addTextChangedListener(textWatcher);
        etReturnProduct.setOnFocusChangeListener(calculateListener);
        etReturnProduct.addTextChangedListener(textWatcher);
        etTodaySale.setOnFocusChangeListener(calculateListener);
        etTodaySale.addTextChangedListener(textWatcher);
        
        // Create dialog
        AlertDialog dialog = builder.setView(dialogView).create();
        
        // Setup button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            // Validate and save data
            if (validateInputs(etOpeningStock, etReceiptFactory, etReturnProduct, etTodaySale, etClosingStock, etDate)) {
                // Calculate closing stock one final time before saving
                calculateClosingStock(etOpeningStock, etReceiptFactory, etReturnProduct, etTodaySale, etClosingStock);
                
                saveStockSummary(
                    spinnerProductType.getSelectedItem().toString(),
                    getIntValueOrDefault(etOpeningStock.getText().toString(), 0),
                    getIntValueOrDefault(etReceiptFactory.getText().toString(), 0),
                    getIntValueOrDefault(etReturnProduct.getText().toString(), 0),
                    getIntValueOrDefault(etTodaySale.getText().toString(), 0),
                    getIntValueOrDefault(etClosingStock.getText().toString(), 0),
                    etDate.getText().toString()
                );
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private void calculateClosingStock(EditText etOpeningStock, EditText etReceiptFactory, 
                                     EditText etReturnProduct, EditText etTodaySale, EditText etClosingStock) {
        int openingStock = getIntValueOrDefault(etOpeningStock.getText().toString(), 0);
        int receiptFactory = getIntValueOrDefault(etReceiptFactory.getText().toString(), 0);
        int returnProduct = getIntValueOrDefault(etReturnProduct.getText().toString(), 0);
        int todaySale = getIntValueOrDefault(etTodaySale.getText().toString(), 0);
        
        int closingStock = openingStock + receiptFactory + returnProduct - todaySale;
        
        // Set the calculated value in the closing stock field
        etClosingStock.setText(String.valueOf(closingStock));
        
        // Also set the text color to indicate it's calculated
        etClosingStock.setTextColor(android.graphics.Color.rgb(102, 102, 102)); // #666666
    }
    
    private boolean validateInputs(EditText etOpeningStock, EditText etReceiptFactory, 
                                 EditText etReturnProduct, EditText etTodaySale, 
                                 EditText etClosingStock, EditText etDate) {
        
        // Validate opening stock as required
        String openingStockText = etOpeningStock.getText().toString().trim();
        if (openingStockText.isEmpty()) {
            etOpeningStock.setError("Opening stock is required");
            etOpeningStock.requestFocus();
            return false;
        }
        
        // Validate that opening stock is a valid number
        try {
            int openingStock = Integer.parseInt(openingStockText);
            if (openingStock < 0) {
                etOpeningStock.setError("Opening stock cannot be negative");
                etOpeningStock.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etOpeningStock.setError("Please enter a valid number for opening stock");
            etOpeningStock.requestFocus();
            return false;
        }
        
        // Validate date as required
        if (etDate.getText().toString().isEmpty()) {
            etDate.setError("Date is required");
            etDate.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void saveStockSummary(String productType, int openingStock, int receiptFactory, 
                                int returnProduct, int todaySale, int closingStock, String date) {
        
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show saving indicator
        showLoading("Saving stock summary...");
        
        StockSummary stockSummary = new StockSummary(
            productType, openingStock, receiptFactory, returnProduct, 
            todaySale, closingStock, date, userId, "Rongdhonu"
        );
        
        // Save to Firebase
        firestoreService.addStockSummary(stockSummary, new FirestoreService.StockSummaryCallback() {
            @Override
            public void onStockSummariesLoaded(List<StockSummary> stockSummaries) {
                // Not used for add operation
            }

            @Override
            public void onStockSummaryAdded(StockSummary stockSummary) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        showSuccess("Stock summary saved successfully!");
                        // Real-time listener will automatically update the list
                    });
                }
            }

            @Override
            public void onStockSummaryUpdated(StockSummary stockSummary) {
                // Not used for add operation
            }

            @Override
            public void onStockSummaryDeleted(String stockSummaryId) {
                // Not used for add operation
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        showError("Failed to save stock summary. Please try again.");
                    });
                }
            }
        });
    }
} 