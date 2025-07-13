package com.dazzling.erp.ui.ceo;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.dazzling.erp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;
import com.dazzling.erp.services.FirestoreService;
import com.dazzling.erp.services.FirebaseAuthService;
import com.dazzling.erp.adapters.CeoStockSummaryAdapter;
import com.dazzling.erp.models.StockSummary;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.Button;

public class CeoRongdhonuOfficeActivity extends AppCompatActivity {

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

    private int selectedTab = 0; // Default selected tab (Polo)
    private String currentSelectedTab = "Polo"; // Default selected tab
    private LinearLayout menuBar;
    private FrameLayout contentContainer;
    private Handler chartHandler;
    private Runnable chartUpdater;
    private FirestoreService firestoreService;
    private FirebaseAuthService authService;
    
    // Stock Summary variables
    private RecyclerView recyclerView;
    private CeoStockSummaryAdapter adapter;
    private List<StockSummary> allStockSummaries = new ArrayList<>();
    private List<StockSummary> filteredStockSummaries = new ArrayList<>();
    private LinearLayout loadingOverlay;
    private TextView loadingText;
    private LinearLayout emptyState;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ceo_rongdhonu_office);

        // Initialize services
        firestoreService = new FirestoreService();
        authService = new FirebaseAuthService();
        
        // Verify Firebase connectivity
        firestoreService.verifyFirebaseConnection(
            () -> Log.d("CeoRongdhonuOffice", "✅ Firebase connectivity verified"),
            () -> Log.e("CeoRongdhonuOffice", "❌ Firebase connectivity failed")
        );

        // Set up the top app bar (MaterialToolbar)
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Rongdhonu Office");
        }

        menuBar = findViewById(R.id.bottom_menu_bar);
        contentContainer = findViewById(R.id.content_container);

        setupMenuBar();
        showTabContent(0, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        try {
            // Hide keyboard when activity pauses
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
            
            // Pause chart updates
            if (chartHandler != null && chartUpdater != null) {
                chartHandler.removeCallbacks(chartUpdater);
            }
        } catch (Exception e) {
            Log.e("CeoRongdhonuOffice", "Error in onPause", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update content when activity is resumed
        showTabContent(selectedTab, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            // Stop chart updates
            if (chartHandler != null && chartUpdater != null) {
                chartHandler.removeCallbacks(chartUpdater);
            }
        } catch (Exception e) {
            Log.e("CeoRongdhonuOffice", "Error in onDestroy", e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupMenuBar() {
        menuBar.removeAllViews();
        
        for (int i = 0; i < menuTabs.length; i++) {
            MenuTab tab = menuTabs[i];
            LinearLayout tabLayout = createMenuTab(tab, i == selectedTab);
            final int tabIndex = i;
            tabLayout.setOnClickListener(v -> {
                if (selectedTab != tabIndex) {
                    animateTabSelection(tabIndex);
                    currentSelectedTab = tab.label;
                    loadTabData(currentSelectedTab);
                }
            });
            menuBar.addView(tabLayout);
        }
    }

    private LinearLayout createMenuTab(MenuTab tab, boolean selected) {
        LinearLayout tabLayout = new LinearLayout(this);
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
        ImageView icon = new ImageView(this);
        icon.setImageResource(tab.iconRes);
        icon.setColorFilter(selected ? Color.WHITE : Color.parseColor("#666666"));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconLp.setMarginEnd(dp(8));
        icon.setLayoutParams(iconLp);
        tabLayout.addView(icon);

        // Label
        TextView text = new TextView(this);
        text.setText(tab.label);
        text.setTextSize(16);
        text.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        text.setTextColor(selected ? Color.WHITE : Color.parseColor("#666666"));
        tabLayout.addView(text);

        // Badge (if needed)
        if (tab.hasBadge) {
            View badge = new View(this);
            int badgeSize = dp(10);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
            badgeLp.setMarginStart(dp(4));
            badge.setLayoutParams(badgeLp);
            badge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_notification_dot));
            tabLayout.addView(badge);
        }

        // Ripple effect
        tabLayout.setForeground(ContextCompat.getDrawable(this, R.drawable.ripple_effect));
        return tabLayout;
    }

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
            bg.setFillColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_surfaceVariant)));
            bg.setElevation(0);
            tabLayout.setBackground(bg);
            if (icon != null) icon.setColorFilter(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void animateTabSelection(int newIndex) {
        int oldIndex = selectedTab;
        selectedTab = newIndex;
        for (int i = 0; i < menuBar.getChildCount(); i++) {
            LinearLayout tabView = (LinearLayout) menuBar.getChildAt(i);
            boolean isSelected = (i == newIndex);
            setTabBackground(tabView, isSelected);
            tabView.setElevation(isSelected ? dp(6) : dp(0));
            tabView.animate().scaleX(isSelected ? 1.08f : 1f).scaleY(isSelected ? 1.08f : 1f).setDuration(180).start();
        }
        currentSelectedTab = menuTabs[newIndex].label;
        loadTabData(currentSelectedTab);
    }



    private void showTabContent(int tabIndex, boolean animate) {
        selectedTab = tabIndex;
        
        // Always show stock summary content for all tabs
        View contentView = createStockSummaryContent();
        
        if (contentView != null) {
            contentContainer.removeAllViews();
            contentContainer.addView(contentView);
        }
    }

    private View createStockSummaryContent() {
        // Create main container
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Initialize RecyclerView with manager styling
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, dp(8), 0, dp(8));

        // Setup adapter (read-only mode)
        adapter = new CeoStockSummaryAdapter(this, filteredStockSummaries);
        recyclerView.setAdapter(adapter);
        
        // Setup click listeners (read-only)
        adapter.setOnItemClickListener(this::showStockSummaryDetails);

        // Create loading overlay matching manager design
        loadingOverlay = new LinearLayout(this);
        loadingOverlay.setOrientation(LinearLayout.VERTICAL);
        loadingOverlay.setGravity(Gravity.CENTER);
        loadingOverlay.setBackgroundColor(Color.parseColor("#80000000"));
        loadingOverlay.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        loadingOverlay.setVisibility(View.GONE);

        // Progress bar
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        progressBar.setIndeterminateTintList(ContextCompat.getColorStateList(this, R.color.primary_color));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        progressLp.setMargins(0, 0, 0, dp(16));
        progressBar.setLayoutParams(progressLp);
        loadingOverlay.addView(progressBar);

        // Loading text
        loadingText = new TextView(this);
        loadingText.setText("Loading...");
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(16);
        loadingOverlay.addView(loadingText);

        // Create empty state matching manager design
        emptyState = new LinearLayout(this);
        emptyState.setOrientation(LinearLayout.VERTICAL);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        emptyState.setVisibility(View.GONE);
        emptyState.setPadding(dp(32), dp(32), dp(32), dp(32));

        // Empty state icon
        ImageView emptyIcon = new ImageView(this);
        emptyIcon.setImageResource(R.drawable.ic_info);
        emptyIcon.setAlpha(0.5f);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(80), dp(80));
        iconLp.setMargins(0, 0, 0, dp(16));
        emptyIcon.setLayoutParams(iconLp);
        emptyState.addView(emptyIcon);

        // Empty state title
        TextView emptyTitle = new TextView(this);
        emptyTitle.setText("No stock summaries found");
        emptyTitle.setTextSize(18);
        emptyTitle.setTextColor(Color.parseColor("#666666"));
        emptyTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleLp.setMargins(0, 0, 0, dp(8));
        emptyTitle.setLayoutParams(titleLp);
        emptyState.addView(emptyTitle);

        // Empty state subtitle
        TextView emptySubtitle = new TextView(this);
        emptySubtitle.setText("No data available for the selected product type");
        emptySubtitle.setTextSize(14);
        emptySubtitle.setTextColor(Color.parseColor("#999999"));
        emptySubtitle.setGravity(Gravity.CENTER);
        emptyState.addView(emptySubtitle);

        // Add views to layout
        layout.addView(recyclerView);
        layout.addView(loadingOverlay);
        layout.addView(emptyState);

        // Load data from Firebase
        loadStockSummariesFromFirebase();

        return layout;
    }



    private View createPlaceholderContent(String label) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(32), dp(32), dp(32), dp(32));

        TextView text = new TextView(this);
        text.setText(label + " - Coming Soon");
        text.setTextSize(18);
        text.setTextColor(Color.parseColor("#666666"));
        text.setGravity(Gravity.CENTER);
        layout.addView(text);

        return layout;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadStockSummariesFromFirebase() {
        // Show loading state
        showLoading("Loading stock summaries...");
        
        // Load stock summaries for Rongdhonu office
        firestoreService.getStockSummariesByOffice("Rongdhonu", new FirestoreService.StockSummaryCallback() {
            @Override
            public void onStockSummariesLoaded(List<StockSummary> summaries) {
                runOnUiThread(() -> {
                    allStockSummaries.clear();
                    allStockSummaries.addAll(summaries);
                    
                    // Filter by current selected tab
                    loadTabData(currentSelectedTab);
                    
                    // Log loaded data
                    Log.d("CeoRongdhonuOffice", "Loaded " + summaries.size() + " stock summaries from Firebase");
                    
                    // Only hide loading on initial load, not on real-time updates
                    if (isInitialLoad) {
                        hideLoading();
                        isInitialLoad = false;
                    }
                });
            }

            @Override
            public void onStockSummaryAdded(StockSummary stockSummary) {
                // Real-time listener will handle this
            }

            @Override
            public void onStockSummaryUpdated(StockSummary stockSummary) {
                // Real-time listener will handle this
            }

            @Override
            public void onStockSummaryDeleted(String stockSummaryId) {
                // Real-time listener will handle this
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (isInitialLoad) {
                        hideLoading();
                        isInitialLoad = false;
                    }
                    showError("Failed to load data. Please try again.");
                    // Load empty data to prevent crashes
                    allStockSummaries.clear();
                    filteredStockSummaries.clear();
                    updateStockSummaryList();
                });
            }
        });
    }
    
    private void loadTabData(String productType) {
        filteredStockSummaries.clear();
        
        for (StockSummary summary : allStockSummaries) {
            if (productType.equals(summary.getProductType())) {
                filteredStockSummaries.add(summary);
            }
        }
        
        updateStockSummaryList();
    }
    
    private void updateStockSummaryList() {
        adapter.updateData(filteredStockSummaries);
        
        // Show/hide empty state
        if (filteredStockSummaries.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }
    
    private void showStockSummaryDetails(StockSummary stockSummary) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
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

        androidx.appcompat.app.AlertDialog dialog = builder.setView(dialogView).create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    
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
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
} 