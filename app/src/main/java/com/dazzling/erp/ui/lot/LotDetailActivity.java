package com.dazzling.erp.ui.lot;

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
import android.widget.EditText;
import android.widget.DatePicker;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.dazzling.erp.services.FirestoreService;
import com.airbnb.lottie.LottieAnimationView;

public class LotDetailActivity extends AppCompatActivity {

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
            new MenuTab("Cutting", R.drawable.ic_cutting, false),
            new MenuTab("Embroidery", R.drawable.ic_fabric, false),
            new MenuTab("Office Shipment", R.drawable.ic_report, false),
            new MenuTab("Factory Balance", R.drawable.ic_dashboard, false)
    };

    private int selectedTab = 0;
    private LinearLayout menuBar;
    private FrameLayout contentContainer;
    private Handler chartHandler;
    private Runnable chartUpdater;
    private LineChart cuttingChart;
    private LineDataSet cuttingDataSet;
    private TextView cuttingBalanceText;
    
    // Embroidery sub-menu variables
    private int selectedEmbroideryTab = 0; // 0 = Receive, 1 = Reject
    private LinearLayout embroiderySubMenuBar;
    private TextView embroideryReceiveBalanceText;
    private TextView embroideryRejectBalanceText;
    
    // Embroidery chart variables
    private LineChart embroideryReceiveChart;
    private LineChart embroideryRejectChart;
    private LineDataSet embroideryReceiveDataSet;
    private LineDataSet embroideryRejectDataSet;
    
    // Office Shipment variables
    private LineChart officeShipmentChart;
    private LineDataSet officeShipmentDataSet;
    private TextView officeShipmentBalanceText;
    
    // Lot data
    private String lotId;
    private com.dazzling.erp.models.Lot currentLot;
    private com.dazzling.erp.services.FirestoreService firestoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lot_detail);

        // Get lot ID from intent
        lotId = getIntent().getStringExtra("lot_id");
        if (lotId == null) {
            android.widget.Toast.makeText(this, "Lot ID not found", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firestore service
        firestoreService = new com.dazzling.erp.services.FirestoreService();
        
        // Verify Firebase connectivity
        firestoreService.verifyFirebaseConnection(
            () -> Log.d("LotDetailActivity", "✅ Firebase connectivity verified"),
            () -> Log.e("LotDetailActivity", "❌ Firebase connectivity failed")
        );
        
        // Get Firebase statistics
        firestoreService.getFirebaseStats(() -> Log.d("LotDetailActivity", "📊 Firebase statistics retrieved"));

        // Set up the top app bar (MaterialToolbar)
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lot Details");
        }

        menuBar = findViewById(R.id.bottom_menu_bar);
        contentContainer = findViewById(R.id.content_container);

        setupMenuBar();
        loadLotData();
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
            Log.e("LotDetailActivity", "Error in onPause", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update chart with current data when activity is resumed
        if (selectedTab == 0 && cuttingChart != null && currentLot != null) {
            updateCuttingBalance();
        } else if (selectedTab == 1) {
            // Update embroidery charts when embroidery tab is active
            if (selectedEmbroideryTab == 0 && embroideryReceiveChart != null) {
                startEmbroideryReceiveChartSimulation();
            } else if (selectedEmbroideryTab == 1 && embroideryRejectChart != null) {
                startEmbroideryRejectChartSimulation();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.removeCallbacks(chartUpdater);
        }
        
        // Clear references
        if (menuBar != null) {
            menuBar.removeAllViews();
        }
        if (contentContainer != null) {
            contentContainer.removeAllViews();
        }
        
        firestoreService = null;
        currentLot = null;
    }

    @Override
    public void onBackPressed() {
        // Simply finish the activity to go back one step
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Simply finish the activity to go back one step
        finish();
        return true;
    }
    
    /**
     * Load lot data from Firestore
     */
    private void loadLotData() {
        firestoreService.getLotById(lotId, new com.dazzling.erp.services.FirestoreService.LotCallback() {
            @Override
            public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {
                if (lots != null && !lots.isEmpty()) {
                    currentLot = lots.get(0);
                    // Update toolbar title with lot number
                    if (getSupportActionBar() != null && currentLot.getLotNumber() != null) {
                        getSupportActionBar().setTitle("Lot: " + currentLot.getLotNumber());
                    }
                    // Show the cutting content with real data
                    showTabContent(0, false);
                    // Update the chart with the loaded data
                    updateCuttingBalance();
                } else {
                    android.widget.Toast.makeText(LotDetailActivity.this, "Lot not found", android.widget.Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
            
            @Override
            public void onLotUpdated(com.dazzling.erp.models.Lot lot) {
                // Refresh data when lot is updated
                if (currentLot != null && lot.getId().equals(currentLot.getId())) {
                    currentLot = lot;
                    updateCuttingBalance();
                }
            }
            
            @Override
            public void onLotDeleted(String lotId) {}
            
            @Override
            public void onError(String error) {
                android.widget.Toast.makeText(LotDetailActivity.this, "Failed to load lot: " + error, android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    /**
     * Update cutting balance display
     */
    private void updateCuttingBalance() {
        if (cuttingBalanceText != null && currentLot != null) {
            int cuttingPcs = currentLot.getCuttingPcs();
            cuttingBalanceText.setText("Total Cutting Balance: " + cuttingPcs + " Pcs.");
            
            // Update the chart to reflect the new balance
            if (cuttingChart != null && selectedTab == 0) {
                startCuttingChartSimulation();
            }
        }
    }

    // Set up the animated, pill-shaped menu bar
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
        icon.setColorFilter(selected ? Color.WHITE : ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconLp.setMarginEnd(dp(8));
        icon.setLayoutParams(iconLp);
        tabLayout.addView(icon);

        // Label
        TextView label = new TextView(this);
        label.setText(tab.label);
        label.setTextSize(16);
        label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        label.setTextColor(selected ? Color.WHITE : ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        tabLayout.addView(label);

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

    // Animate tab selection and update content
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
        showTabContent(newIndex, true);
    }

    // Show content for the selected tab
    private void showTabContent(int tabIndex, boolean animate) {
        contentContainer.removeAllViews();
        View contentView;
        switch (tabIndex) {
            case 0: // Cutting
                contentView = createCuttingContent();
                break;
            case 1: // Embroidery
                contentView = createEmbroideryContent();
                // Ensure receive content is shown by default
                selectedEmbroideryTab = 0;
                break;
            case 2: // Office Shipment
                contentView = createOfficeShipmentContent();
                break;
            case 3: // Factory Balance
            default:
                contentView = createPlaceholderContent(menuTabs[tabIndex].label);
                break;
        }
        if (animate) {
            contentView.setAlpha(0f);
            contentContainer.addView(contentView);
            contentView.animate().alpha(1f).setDuration(250).start();
        } else {
            contentContainer.addView(contentView);
        }
        
        // If it's embroidery tab, ensure the receive content is shown
        if (tabIndex == 1) {
            // Small delay to ensure the view is properly added
            new Handler(Looper.getMainLooper()).post(() -> {
                showEmbroiderySubContent(selectedEmbroideryTab);
            });
        }
    }

    // Cutting tab content: Card with chart, stats, and button
    private View createCuttingContent() {
        // CardView container
        CardView card = new CardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(24));
        card.setLayoutParams(cardLp);
        card.setContentPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout vbox = new LinearLayout(this);
        vbox.setOrientation(LinearLayout.VERTICAL);
        vbox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Chart
        cuttingChart = new LineChart(this);
        cuttingChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        cuttingChart.setNoDataText("Loading chart...");
        cuttingChart.setTouchEnabled(false);
        cuttingChart.setDescription(new Description());
        vbox.addView(cuttingChart);

        // Cutting Balance
        cuttingBalanceText = new TextView(this);
        if (currentLot != null) {
            int cuttingPcs = currentLot.getCuttingPcs();
            cuttingBalanceText.setText("Total Cutting Balance: " + cuttingPcs + " Pcs.");
        } else {
            cuttingBalanceText.setText("Total Cutting Balance: 0 Pcs.");
        }
        cuttingBalanceText.setTextSize(14);
        cuttingBalanceText.setTypeface(null, android.graphics.Typeface.BOLD);
        cuttingBalanceText.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        cuttingBalanceText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(cuttingBalanceText);

        // Transfer Button
        MaterialButton transferBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        transferBtn.setText("Send to Embroidery");
        transferBtn.setCornerRadius(dp(30));
        transferBtn.setTextSize(16);
        transferBtn.setElevation(dp(4));
        transferBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        transferBtn.setTextColor(Color.BLACK);
        transferBtn.setBackgroundColor(Color.parseColor("#9EE772"));
        transferBtn.setOnClickListener(v -> {
            showTransferToEmbroideryDialog();
        });

        vbox.addView(transferBtn);

        card.addView(vbox);
        // Start chart data simulation
        startCuttingChartSimulation();
        return card;
    }

    // Office Shipment tab content: Card with chart, stats, and button
    private View createOfficeShipmentContent() {
        // CardView container
        CardView card = new CardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(24));
        card.setLayoutParams(cardLp);
        card.setContentPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout vbox = new LinearLayout(this);
        vbox.setOrientation(LinearLayout.VERTICAL);
        vbox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Chart
        officeShipmentChart = new LineChart(this);
        officeShipmentChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        officeShipmentChart.setNoDataText("Loading chart...");
        officeShipmentChart.setTouchEnabled(false);
        officeShipmentChart.setDescription(new Description());
        vbox.addView(officeShipmentChart);

        // Office Shipment Balance
        officeShipmentBalanceText = new TextView(this);
        if (currentLot != null) {
            int officeShipmentPcs = currentLot.getOfficeShipmentPcs(); // Convert to Pcs for display
            officeShipmentBalanceText.setText("Total Office Balance: " + officeShipmentPcs + " Pcs.");
        } else {
            officeShipmentBalanceText.setText("Total Office Balance: 0 Pcs.");
        }
        officeShipmentBalanceText.setTextSize(14);
        officeShipmentBalanceText.setTypeface(null, android.graphics.Typeface.BOLD);
        officeShipmentBalanceText.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        officeShipmentBalanceText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(officeShipmentBalanceText);

        // Send to Office Button
        MaterialButton sendToOfficeBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        sendToOfficeBtn.setText("Send to Office");
        sendToOfficeBtn.setCornerRadius(dp(30));
        sendToOfficeBtn.setTextSize(16);
        sendToOfficeBtn.setElevation(dp(4));
        sendToOfficeBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sendToOfficeBtn.setTextColor(Color.BLACK);
        sendToOfficeBtn.setBackgroundColor(Color.parseColor("#FFB74D")); // Orange color for office shipment
        sendToOfficeBtn.setOnClickListener(v -> {
            showSendToOfficeDialog();
        });

        vbox.addView(sendToOfficeBtn);

        card.addView(vbox);
        // Start chart data simulation
        startOfficeShipmentChartSimulation();
        return card;
    }

    // Embroidery tab content: Card with sub-menu and content
    private View createEmbroideryContent() {
        // Main container
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Sub-menu bar (horizontal scrollable)
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.setPadding(dp(16), dp(4), dp(16), dp(8)); // Reduced top padding
        scrollView.setBackgroundColor(Color.parseColor("#EDEEF3"));
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setMinimumHeight(dp(56)); // Ensure minimum height for visibility

        embroiderySubMenuBar = new LinearLayout(this);
        embroiderySubMenuBar.setOrientation(LinearLayout.HORIZONTAL);
        embroiderySubMenuBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        embroiderySubMenuBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        embroiderySubMenuBar.setGravity(Gravity.CENTER_HORIZONTAL);
        embroiderySubMenuBar.setMinimumHeight(dp(48)); // Ensure minimum height for visibility
        


        // Create sub-menu tabs
        String[] embroideryTabs = {"Receive", "Reject"};
        for (int i = 0; i < embroideryTabs.length; i++) {
            final int index = i;
            LinearLayout subTab = createEmbroiderySubTab(embroideryTabs[i], i == selectedEmbroideryTab);
            subTab.setOnClickListener(v -> {
                if (selectedEmbroideryTab != index) {
                    selectedEmbroideryTab = index;
                    updateEmbroiderySubMenu();
                    showEmbroiderySubContent(index);
                }
            });
            embroiderySubMenuBar.addView(subTab);
            Log.d("EmbroiderySubMenu", "Added sub-tab: " + embroideryTabs[i]);
        }

        scrollView.addView(embroiderySubMenuBar);
        mainContainer.addView(scrollView);
        
        // Update the sub-menu to ensure proper styling
        updateEmbroiderySubMenu();
        
        // Add underline after the scroll view
        View underline = new View(this);
        LinearLayout.LayoutParams underlineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        underlineParams.setMargins(0, 0, 0, 0); // Remove any margins
        underline.setLayoutParams(underlineParams);
        underline.setBackgroundColor(Color.parseColor("#666666"));
        mainContainer.addView(underline);
        
        // Center the scroll view content
        scrollView.post(() -> {
            int scrollViewWidth = scrollView.getWidth();
            int menuBarWidth = embroiderySubMenuBar.getWidth();
            if (scrollViewWidth > menuBarWidth) {
                int scrollX = (menuBarWidth - scrollViewWidth) / 2;
                scrollView.scrollTo(scrollX, 0);
            }
        });

        // Content container
        FrameLayout embroideryContentContainer = new FrameLayout(this);
        embroideryContentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(embroideryContentContainer);

        // Show initial content (Embroidery Receive by default)
        // Small delay to ensure the view hierarchy is properly set up
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d("EmbroideryContent", "Showing initial content for tab: " + selectedEmbroideryTab);
            showEmbroiderySubContent(selectedEmbroideryTab);
        });

        Log.d("EmbroideryContent", "Created embroidery content with " + embroiderySubMenuBar.getChildCount() + " sub-tabs");
        return mainContainer;
    }

    // Create embroidery sub-tab
    private LinearLayout createEmbroiderySubTab(String label, boolean selected) {
        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setGravity(Gravity.CENTER);
        int padH = dp(16);
        int padV = dp(8);
        tabLayout.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), 0, dp(12), 0); // Add horizontal gap between tabs
        tabLayout.setLayoutParams(lp);
        tabLayout.setClickable(true);
        tabLayout.setFocusable(true);
        tabLayout.setMinimumHeight(dp(40)); // Ensure minimum height
        setEmbroiderySubTabBackground(tabLayout, selected);
        tabLayout.setElevation(selected ? dp(4) : dp(0));

        // Label
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        labelView.setTextColor(selected ? Color.WHITE : Color.BLACK);
        tabLayout.addView(labelView);
        
        Log.d("EmbroiderySubTab", "Created sub-tab with label: " + label + ", selected: " + selected);

        // Ripple effect
        tabLayout.setForeground(ContextCompat.getDrawable(this, R.drawable.ripple_effect));
        return tabLayout;
    }

    // Set embroidery sub-tab background
    private void setEmbroiderySubTabBackground(LinearLayout tabLayout, boolean selected) {
        TextView label = null;
        // Find TextView in tabLayout
        for (int i = 0; i < tabLayout.getChildCount(); i++) {
            View child = tabLayout.getChildAt(i);
            if (child instanceof TextView) {
                label = (TextView) child;
                break;
            }
        }
        
        if (selected) {
            android.graphics.drawable.GradientDrawable solid = new android.graphics.drawable.GradientDrawable();
            solid.setColor(android.graphics.Color.rgb(158, 231, 114));
            solid.setCornerRadius(dp(25));
            tabLayout.setBackground(solid);
            if (label != null) {
                label.setTextColor(android.graphics.Color.WHITE);
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        } else {
            ShapeAppearanceModel shape = new ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, dp(25))
                    .build();
            MaterialShapeDrawable bg = new MaterialShapeDrawable(shape);
            bg.setFillColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
            bg.setElevation(0);
            tabLayout.setBackground(bg);
            if (label != null) {
                label.setTextColor(Color.BLACK);
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    // Update embroidery sub-menu appearance
    private void updateEmbroiderySubMenu() {
        if (embroiderySubMenuBar != null) {
            Log.d("EmbroiderySubMenu", "Updating sub-menu with " + embroiderySubMenuBar.getChildCount() + " tabs, selected: " + selectedEmbroideryTab);
            for (int i = 0; i < embroiderySubMenuBar.getChildCount(); i++) {
                LinearLayout tabView = (LinearLayout) embroiderySubMenuBar.getChildAt(i);
                boolean isSelected = (i == selectedEmbroideryTab);
                setEmbroiderySubTabBackground(tabView, isSelected);
                tabView.setElevation(isSelected ? dp(4) : dp(0));
                tabView.animate().scaleX(isSelected ? 1.05f : 1f).scaleY(isSelected ? 1.05f : 1f).setDuration(150).start();
            }
        } else {
            Log.e("EmbroiderySubMenu", "embroiderySubMenuBar is null");
        }
    }

    // Show embroidery sub-content
    private void showEmbroiderySubContent(int subTabIndex) {
        Log.d("EmbroiderySubContent", "Showing sub-content for index: " + subTabIndex);
        
        // Find the content container (third child of main container after scroll view and underline)
        if (contentContainer.getChildCount() > 0) {
            View mainContainer = contentContainer.getChildAt(0);
            if (mainContainer instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) mainContainer;
                Log.d("EmbroiderySubContent", "Container child count: " + container.getChildCount());
                
                if (container.getChildCount() > 2) {
                    FrameLayout embroideryContentContainer = (FrameLayout) container.getChildAt(2);
                    embroideryContentContainer.removeAllViews();
                    
                    View subContent;
                    switch (subTabIndex) {
                        case 0: // Embroidery Receive
                            subContent = createEmbroideryReceiveContent();
                            Log.d("EmbroiderySubContent", "Created Receive content");
                            break;
                        case 1: // Embroidery Reject
                            subContent = createEmbroideryRejectContent();
                            Log.d("EmbroiderySubContent", "Created Reject content");
                            break;
                        default:
                            subContent = createPlaceholderContent("Embroidery");
                            Log.d("EmbroiderySubContent", "Created placeholder content");
                            break;
                    }
                    
                    embroideryContentContainer.addView(subContent);
                    
                    // Force layout update
                    embroideryContentContainer.requestLayout();
                    embroideryContentContainer.invalidate();
                    Log.d("EmbroiderySubContent", "Content added and layout updated");
                } else {
                    Log.e("EmbroiderySubContent", "Container doesn't have enough children. Expected > 2, got: " + container.getChildCount());
                }
            } else {
                Log.e("EmbroiderySubContent", "Main container is not LinearLayout");
            }
        } else {
            Log.e("EmbroiderySubContent", "Content container has no children");
        }
    }

    // Embroidery Receive content
    private View createEmbroideryReceiveContent() {
        // CardView container
        CardView card = new CardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(16), dp(16), dp(16), dp(24));
        card.setLayoutParams(cardLp);
        card.setContentPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout vbox = new LinearLayout(this);
        vbox.setOrientation(LinearLayout.VERTICAL);
        vbox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Chart
        embroideryReceiveChart = new LineChart(this);
        embroideryReceiveChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        embroideryReceiveChart.setNoDataText("Loading chart...");
        embroideryReceiveChart.setTouchEnabled(false);
        embroideryReceiveChart.setDescription(new Description());
        vbox.addView(embroideryReceiveChart);

        // Embroidery Receive Balance
        embroideryReceiveBalanceText = new TextView(this);
        if (currentLot != null) {
            // Use the actual embroidery receive data from the lot
            int receivePcs = currentLot.getEmbroideryReceivePcs();
            embroideryReceiveBalanceText.setText("Total Received: " + receivePcs + " Pcs.");
        } else {
            embroideryReceiveBalanceText.setText("Total Received: 0 Pcs.");
        }
        embroideryReceiveBalanceText.setTextSize(14);
        embroideryReceiveBalanceText.setTypeface(null, android.graphics.Typeface.BOLD);
        embroideryReceiveBalanceText.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        embroideryReceiveBalanceText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(embroideryReceiveBalanceText);

        // Receive Button
        MaterialButton receiveBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        receiveBtn.setText("Send to Office");
        receiveBtn.setCornerRadius(dp(30));
        receiveBtn.setTextSize(16);
        receiveBtn.setElevation(dp(4));
        receiveBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        receiveBtn.setTextColor(Color.BLACK);
        receiveBtn.setBackgroundColor(Color.parseColor("#9EE772"));
        receiveBtn.setOnClickListener(v -> {
            showSendToOfficeFromEmbroideryDialog();
        });

        vbox.addView(receiveBtn);

        // Send to Factory Button
        MaterialButton factoryBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        factoryBtn.setText("Send to Factory Balance");
        factoryBtn.setCornerRadius(dp(30));
        factoryBtn.setTextSize(16);
        factoryBtn.setElevation(dp(4));
        factoryBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        factoryBtn.setTextColor(Color.BLACK);
        factoryBtn.setBackgroundColor(Color.parseColor("#FF9800")); // Orange color
        factoryBtn.setOnClickListener(v -> {
            // Show loading animation
            showLoading(true);
            
            // Disable button to prevent multiple clicks
            factoryBtn.setEnabled(false);
            
            // Simulate factory process with delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Show progress message
                Toast.makeText(this, "Processing factory transfer...", Toast.LENGTH_SHORT).show();
                
                // Simulate another delay for the actual factory transfer
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Hide loading and show success message
                    showLoading(false);
                    
                    // Show success message with details
                    String successMessage = "🏭 Successfully sent to factory!";
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                    
                    // Re-enable button
                    factoryBtn.setEnabled(true);
                    
                    // Update the embroidery receive balance and chart
                    updateEmbroideryReceiveBalance();
                    startEmbroideryReceiveChartSimulation();
                }, 1500); // 1.5 second delay for factory simulation
            }, 1000); // 1 second delay for initial processing
        });

        vbox.addView(factoryBtn);

        // Mark as Reject Button
        MaterialButton rejectBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        rejectBtn.setText("Mark as Reject");
        rejectBtn.setCornerRadius(dp(30));
        rejectBtn.setTextSize(16);
        rejectBtn.setElevation(dp(4));
        rejectBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rejectBtn.setTextColor(Color.WHITE);
        rejectBtn.setBackgroundColor(Color.parseColor("#F44336")); // Red color
        rejectBtn.setOnClickListener(v -> {
            showMarkAsRejectDialog();
        });

        vbox.addView(rejectBtn);

        card.addView(vbox);
        
        // Start chart simulation
        startEmbroideryReceiveChartSimulation();
        return card;
    }

    // Embroidery Reject content
    private View createEmbroideryRejectContent() {
        // CardView container
        CardView card = new CardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(16), dp(16), dp(16), dp(24));
        card.setLayoutParams(cardLp);
        card.setContentPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout vbox = new LinearLayout(this);
        vbox.setOrientation(LinearLayout.VERTICAL);
        vbox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Chart
        embroideryRejectChart = new LineChart(this);
        embroideryRejectChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        embroideryRejectChart.setNoDataText("Loading chart...");
        embroideryRejectChart.setTouchEnabled(false);
        embroideryRejectChart.setDescription(new Description());
        vbox.addView(embroideryRejectChart);

        // Embroidery Reject Balance
        embroideryRejectBalanceText = new TextView(this);
        if (currentLot != null) {
            // Use the actual embroidery reject data from the lot
            int rejectPcs = currentLot.getEmbroideryRejectPcs();
            embroideryRejectBalanceText.setText("Total Rejected: " + rejectPcs + " Pcs.");
        } else {
            embroideryRejectBalanceText.setText("Total Rejected: 0 Pcs.");
        }
        embroideryRejectBalanceText.setTextSize(14);
        embroideryRejectBalanceText.setTypeface(null, android.graphics.Typeface.BOLD);
        embroideryRejectBalanceText.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        embroideryRejectBalanceText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(embroideryRejectBalanceText);

        // Reject Button
        MaterialButton rejectBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        rejectBtn.setText("Mark as Rejected");
        rejectBtn.setCornerRadius(dp(30));
        rejectBtn.setTextSize(16);
        rejectBtn.setElevation(dp(4));
        rejectBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rejectBtn.setTextColor(Color.WHITE);
        rejectBtn.setBackgroundColor(Color.parseColor("#F44336"));
        rejectBtn.setOnClickListener(v -> {
            showMarkAsRejectDialog();
        });

        vbox.addView(rejectBtn);

        card.addView(vbox);
        
        // Start chart simulation
        startEmbroideryRejectChartSimulation();
        return card;
    }

    // Update embroidery receive balance
    private void updateEmbroideryReceiveBalance() {
        if (embroideryReceiveBalanceText != null && currentLot != null) {
            // Use the actual embroidery receive data from the lot
            int receivePcs = currentLot.getEmbroideryReceivePcs();
            embroideryReceiveBalanceText.setText("Total Received: " + receivePcs + " Pcs.");
        }
    }

    // Update embroidery reject balance
    private void updateEmbroideryRejectBalance() {
        if (embroideryRejectBalanceText != null && currentLot != null) {
            // Use the actual embroidery reject data from the lot
            int rejectPcs = currentLot.getEmbroideryRejectPcs();
            embroideryRejectBalanceText.setText("Total Rejected: " + rejectPcs + " Pcs.");
            Log.d("LotDetailActivity", "Updated embroidery reject balance: " + rejectPcs + " pcs");
        } else {
            Log.e("LotDetailActivity", "Cannot update embroidery reject balance: text=" + (embroideryRejectBalanceText != null) + ", lot=" + (currentLot != null));
        }
    }

    // Embroidery Receive Chart Simulation
    private void startEmbroideryReceiveChartSimulation() {
        if (embroideryReceiveChart == null) return;
        
        // Get the current embroidery receive balance from the lot data
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getEmbroideryReceivePcs();
        }
        
        List<Entry> entries = new ArrayList<>();
        
        // Create a realistic chart based on the current balance
        // Show the last 7 days with the current balance as the peak
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) { // Today - show current balance
                value = currentBalance;
            } else if (i == 5) { // Yesterday - show slightly lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) { // 2 days ago - show even lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else { // Earlier days - show progression from 0 to current
                float progress = (float) i / 5f; // 0 to 1 over 5 days
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        
        embroideryReceiveDataSet = new LineDataSet(entries, "Embroidery Receive");
        embroideryReceiveDataSet.setColor(Color.parseColor("#4CAF50")); // Green color for receive
        embroideryReceiveDataSet.setCircleColor(Color.parseColor("#4CAF50"));
        embroideryReceiveDataSet.setLineWidth(3f);
        embroideryReceiveDataSet.setCircleRadius(5f);
        embroideryReceiveDataSet.setDrawValues(true);
        embroideryReceiveDataSet.setValueTextSize(10f);
        embroideryReceiveDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        embroideryReceiveDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        embroideryReceiveDataSet.setDrawCircles(true);
        embroideryReceiveDataSet.setDrawCircleHole(true);
        embroideryReceiveDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(embroideryReceiveDataSet);
        embroideryReceiveChart.setData(data);
        
        // Configure chart appearance
        embroideryReceiveChart.getXAxis().setDrawLabels(true);
        embroideryReceiveChart.getXAxis().setGranularity(1f);
        embroideryReceiveChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        embroideryReceiveChart.getAxisRight().setEnabled(false);
        embroideryReceiveChart.getAxisLeft().setDrawGridLines(true);
        embroideryReceiveChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        embroideryReceiveChart.getAxisLeft().setGridLineWidth(0.5f);
        embroideryReceiveChart.getXAxis().setDrawGridLines(false);
        embroideryReceiveChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        embroideryReceiveChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            embroideryReceiveChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            embroideryReceiveChart.getAxisLeft().setAxisMaximum(50f); // Lower default for empty state
        }
        
        embroideryReceiveChart.invalidate();
    }

    // Embroidery Reject Chart Simulation
    private void startEmbroideryRejectChartSimulation() {
        if (embroideryRejectChart == null) return;
        
        // Get the current embroidery reject balance from the lot data
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getEmbroideryRejectPcs();
        }
        
        List<Entry> entries = new ArrayList<>();
        
        // Create a realistic chart based on the current balance
        // Show the last 7 days with the current balance as the peak
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) { // Today - show current balance
                value = currentBalance;
            } else if (i == 5) { // Yesterday - show slightly lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) { // 2 days ago - show even lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else { // Earlier days - show progression from 0 to current
                float progress = (float) i / 5f; // 0 to 1 over 5 days
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        
        embroideryRejectDataSet = new LineDataSet(entries, "Embroidery Reject");
        embroideryRejectDataSet.setColor(Color.parseColor("#F44336")); // Red color for reject
        embroideryRejectDataSet.setCircleColor(Color.parseColor("#F44336"));
        embroideryRejectDataSet.setLineWidth(3f);
        embroideryRejectDataSet.setCircleRadius(5f);
        embroideryRejectDataSet.setDrawValues(true);
        embroideryRejectDataSet.setValueTextSize(10f);
        embroideryRejectDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        embroideryRejectDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        embroideryRejectDataSet.setDrawCircles(true);
        embroideryRejectDataSet.setDrawCircleHole(true);
        embroideryRejectDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(embroideryRejectDataSet);
        embroideryRejectChart.setData(data);
        
        // Configure chart appearance
        embroideryRejectChart.getXAxis().setDrawLabels(true);
        embroideryRejectChart.getXAxis().setGranularity(1f);
        embroideryRejectChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        embroideryRejectChart.getAxisRight().setEnabled(false);
        embroideryRejectChart.getAxisLeft().setDrawGridLines(true);
        embroideryRejectChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        embroideryRejectChart.getAxisLeft().setGridLineWidth(0.5f);
        embroideryRejectChart.getXAxis().setDrawGridLines(false);
        embroideryRejectChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        embroideryRejectChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            embroideryRejectChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            embroideryRejectChart.getAxisLeft().setAxisMaximum(100f);
        }
        
        embroideryRejectChart.invalidate();
    }

    // Placeholder for other tabs
    private View createPlaceholderContent(String label) {
        CardView card = new CardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(24));
        card.setLayoutParams(cardLp);
        card.setContentPadding(dp(20), dp(40), dp(20), dp(40));
        TextView tv = new TextView(this);
        tv.setText(label + " section coming soon");
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        card.addView(tv);
        return card;
    }

    // Update chart with real cutting balance data
    private void startCuttingChartSimulation() {
        if (cuttingChart == null) return;
        
        // Get the current cutting balance
        int currentBalance = (currentLot != null) ? currentLot.getCuttingPcs() : 0;
        
        List<Entry> entries = new ArrayList<>();
        
        // Create a realistic chart based on the current balance
        // Show the last 7 days with the current balance as the peak
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) { // Today - show current balance
                value = currentBalance;
            } else if (i == 5) { // Yesterday - show slightly lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) { // 2 days ago - show even lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else { // Earlier days - show progression from 0 to current
                float progress = (float) i / 5f; // 0 to 1 over 5 days
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        
        cuttingDataSet = new LineDataSet(entries, "Cutting Balance");
        cuttingDataSet.setColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        cuttingDataSet.setCircleColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        cuttingDataSet.setLineWidth(3f);
        cuttingDataSet.setCircleRadius(5f);
        cuttingDataSet.setDrawValues(true);
        cuttingDataSet.setValueTextSize(10f);
        cuttingDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        cuttingDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        cuttingDataSet.setDrawCircles(true);
        cuttingDataSet.setDrawCircleHole(true);
        cuttingDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(cuttingDataSet);
        cuttingChart.setData(data);
        
        // Configure chart appearance
        cuttingChart.getXAxis().setDrawLabels(true);
        cuttingChart.getXAxis().setGranularity(1f);
        cuttingChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        cuttingChart.getAxisRight().setEnabled(false);
        cuttingChart.getAxisLeft().setDrawGridLines(true);
        cuttingChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        cuttingChart.getAxisLeft().setGridLineWidth(0.5f);
        cuttingChart.getXAxis().setDrawGridLines(false);
        cuttingChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        cuttingChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            cuttingChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            cuttingChart.getAxisLeft().setAxisMaximum(100f);
        }
        
        cuttingChart.invalidate();
        
        // Remove the random updates - chart should only update when balance changes
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.removeCallbacks(chartUpdater);
        }
    }

    // Update office shipment balance display
    private void updateOfficeShipmentBalance() {
        if (officeShipmentBalanceText != null && currentLot != null) {
            int officeShipmentPcs = currentLot.getOfficeShipmentPcs();
            officeShipmentBalanceText.setText("Total Office Balance: " + officeShipmentPcs + " Pcs.");
        }
    }

    // Update chart with real office shipment balance data
    private void startOfficeShipmentChartSimulation() {
        if (officeShipmentChart == null) return;
        
        // Get the current office shipment balance
        int currentBalance = (currentLot != null) ? currentLot.getOfficeShipmentPcs() : 0;
        
        List<Entry> entries = new ArrayList<>();
        
        // Create a realistic chart based on the current balance
        // Show the last 7 days with the current balance as the peak
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) { // Today - show current balance
                value = currentBalance;
            } else if (i == 5) { // Yesterday - show slightly lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) { // 2 days ago - show even lower
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else { // Earlier days - show progression from 0 to current
                float progress = (float) i / 5f; // 0 to 1 over 5 days
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        
        officeShipmentDataSet = new LineDataSet(entries, "Office Shipment Balance");
        officeShipmentDataSet.setColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        officeShipmentDataSet.setCircleColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        officeShipmentDataSet.setLineWidth(3f);
        officeShipmentDataSet.setCircleRadius(5f);
        officeShipmentDataSet.setDrawValues(true);
        officeShipmentDataSet.setValueTextSize(10f);
        officeShipmentDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        officeShipmentDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        officeShipmentDataSet.setDrawCircles(true);
        officeShipmentDataSet.setDrawCircleHole(true);
        officeShipmentDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(officeShipmentDataSet);
        officeShipmentChart.setData(data);
        
        // Configure chart appearance
        officeShipmentChart.getXAxis().setDrawLabels(true);
        officeShipmentChart.getXAxis().setGranularity(1f);
        officeShipmentChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        officeShipmentChart.getAxisRight().setEnabled(false);
        officeShipmentChart.getAxisLeft().setDrawGridLines(true);
        officeShipmentChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        officeShipmentChart.getAxisLeft().setGridLineWidth(0.5f);
        officeShipmentChart.getXAxis().setDrawGridLines(false);
        officeShipmentChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        officeShipmentChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            officeShipmentChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            officeShipmentChart.getAxisLeft().setAxisMaximum(100f);
        }
        
        officeShipmentChart.invalidate();
    }

    /**
     * Show or hide loading animation
     */
    private void showLoading(boolean show) {
        try {
            // Find the global loading view from MainActivity
            View loadingView = findViewById(R.id.lottie_loading);
            if (loadingView != null && loadingView instanceof LottieAnimationView) {
                LottieAnimationView lottieLoading = (LottieAnimationView) loadingView;
                if (show) {
                    lottieLoading.setVisibility(View.VISIBLE);
                    lottieLoading.playAnimation();
                } else {
                    lottieLoading.pauseAnimation();
                    lottieLoading.setVisibility(View.GONE);
                }
            } else {
                // Fallback: show a simple progress dialog
                if (show) {
                    // Show a simple loading indicator
                    Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("LotDetailActivity", "Error showing/hiding loading", e);
        }
    }

    // Utility: dp to px
    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
    
    private void showTransferToEmbroideryDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Send Quantity (pcs):");
        quantityLabel.setTextSize(16);
        quantityLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        quantityLabel.setPadding(0, 0, 0, dp(8));
        dialogLayout.addView(quantityLabel);
        
        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Enter quantity");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        quantityInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        quantityInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        quantityInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        dialogLayout.addView(quantityInput);
        
        // Current Quantity display below input field
        TextView currentQuantityText = new TextView(this);
        if (currentLot != null) {
            int currentQuantity = currentLot.getCuttingPcs();
            currentQuantityText.setText("Current Quantity: " + currentQuantity + " Pcs");
        } else {
            currentQuantityText.setText("Current Quantity: 0 Pcs");
        }
        currentQuantityText.setTextSize(12); // Small text
        currentQuantityText.setTextColor(Color.RED); // Red color
        currentQuantityText.setPadding(0, dp(4), 0, dp(8));
        currentQuantityText.setBackground(null);
        currentQuantityText.setClickable(false);
        currentQuantityText.setFocusable(false);
        dialogLayout.addView(currentQuantityText);
        

        
        // Date input field
        TextView dateLabel = new TextView(this);
        dateLabel.setText("Date:");
        dateLabel.setTextSize(16);
        dateLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        dateLabel.setPadding(0, dp(16), 0, dp(8));
        dialogLayout.addView(dateLabel);
        
        EditText dateInput = new EditText(this);
        dateInput.setHint("Select date (optional)");
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dateInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        dateInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        
        // Set current date as default
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        dateInput.setText(currentDate);
        
        dialogLayout.addView(dateInput);
        
        // Date picker functionality
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    dateInput.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transfer to Embroidery")
               .setView(dialogLayout)
               .setPositiveButton("Send", (dialog, which) -> {
                   String quantityStr = quantityInput.getText().toString().trim();
                   String dateStr = dateInput.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(quantityStr)) {
                       Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   int quantity = Integer.parseInt(quantityStr);
                   int maxQuantity = currentLot != null ? currentLot.getCuttingPcs() : 0;
                   
                   if (quantity <= 0) {
                       Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   if (quantity > maxQuantity) {
                       Toast.makeText(this, "Quantity cannot exceed maximum available (" + maxQuantity + " pcs)", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   // Process the transfer
                   processTransferToEmbroidery(quantity, dateStr);
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showMarkAsRejectDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Mark As Reject Quantity (pcs):");
        quantityLabel.setTextSize(16);
        quantityLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        quantityLabel.setPadding(0, 0, 0, dp(8));
        dialogLayout.addView(quantityLabel);
        
        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Enter quantity");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        quantityInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        quantityInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        quantityInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        dialogLayout.addView(quantityInput);
        
        // Current Quantity display below input field (using total received quantity)
        TextView currentQuantityText = new TextView(this);
        int displayQuantity = 0;
        if (currentLot != null) {
            displayQuantity = currentLot.getEmbroideryReceivePcs();
            currentQuantityText.setText("Current Quantity: " + displayQuantity + " Pcs");
        } else {
            currentQuantityText.setText("Current Quantity: 0 Pcs");
        }
        final int finalDisplayQuantity = displayQuantity;
        currentQuantityText.setTextSize(12); // Small text
        currentQuantityText.setTextColor(Color.RED); // Red color
        currentQuantityText.setPadding(0, dp(4), 0, dp(8));
        currentQuantityText.setBackground(null);
        currentQuantityText.setClickable(false);
        currentQuantityText.setFocusable(false);
        dialogLayout.addView(currentQuantityText);
        
        // Date input field
        TextView dateLabel = new TextView(this);
        dateLabel.setText("Date:");
        dateLabel.setTextSize(16);
        dateLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        dateLabel.setPadding(0, dp(16), 0, dp(8));
        dialogLayout.addView(dateLabel);
        
        EditText dateInput = new EditText(this);
        dateInput.setHint("Select date (optional)");
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dateInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        dateInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        
        // Set current date as default
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        dateInput.setText(currentDate);
        
        dialogLayout.addView(dateInput);
        
        // Date picker functionality
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    dateInput.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mark As Reject")
               .setView(dialogLayout)
               .setPositiveButton("Reject", (dialog, which) -> {
                   String quantityStr = quantityInput.getText().toString().trim();
                   String dateStr = dateInput.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(quantityStr)) {
                       Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   int quantity = Integer.parseInt(quantityStr);
                   int maxQuantity = finalDisplayQuantity;
                   
                   if (quantity <= 0) {
                       Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   if (quantity > maxQuantity) {
                       Toast.makeText(this, "Quantity cannot exceed maximum available (" + maxQuantity + " pcs)", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   // Process the rejection
                   processMarkAsReject(quantity, dateStr);
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void processMarkAsReject(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate reject process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing rejection...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual reject
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("❌ Successfully marked %d pcs as rejected!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the rejection
                if (currentLot != null) {
                    // Reduce embroidery receive quantity by rejected quantity
                    int currentEmbroideryReceive = currentLot.getEmbroideryReceivePcs();
                    currentLot.setEmbroideryReceivePcs(currentEmbroideryReceive - quantity);
                    
                    // Add to embroidery reject balance
                    int currentEmbroideryReject = currentLot.getEmbroideryRejectPcs();
                    currentLot.setEmbroideryRejectPcs(currentEmbroideryReject + quantity);
                    
                    // Log the changes for debugging
                    Log.d("LotDetailActivity", "Rejection processed: " + quantity + " pcs");
                    Log.d("LotDetailActivity", "New embroidery receive: " + currentLot.getEmbroideryReceivePcs() + " pcs");
                    Log.d("LotDetailActivity", "New embroidery reject: " + currentLot.getEmbroideryRejectPcs() + " pcs");
                    
                    // Update embroidery receive balance display immediately
                    updateEmbroideryReceiveBalance();
                    
                    // Update embroidery reject balance display immediately
                    updateEmbroideryRejectBalance();
                    
                    // Update charts immediately
                    startEmbroideryReceiveChartSimulation();
                    startEmbroideryRejectChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after rejection - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Embroidery Receive: " + currentLot.getEmbroideryReceivePcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase save - Embroidery Reject: " + currentLot.getEmbroideryRejectPcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after rejection");
                            Log.d("LotDetailActivity", "Firebase confirmed - Embroidery Receive: " + updatedLot.getEmbroideryReceivePcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase confirmed - Embroidery Reject: " + updatedLot.getEmbroideryRejectPcs() + " pcs");
                            // Update the charts again after database sync
                            updateEmbroideryReceiveBalance();
                            updateEmbroideryRejectBalance();
                            startEmbroideryReceiveChartSimulation();
                            startEmbroideryRejectChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after rejection: " + error);
                            Toast.makeText(LotDetailActivity.this, "Rejection completed but failed to update database", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                        
                        @Override
                        public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                        
                        @Override
                        public void onLotDeleted(String lotId) {}
                    });
                        }, 
                        () -> {
                            // Data validation failed
                            Log.e("LotDetailActivity", "❌ Data validation failed before Firebase save");
                            Toast.makeText(LotDetailActivity.this, "Data validation failed", Toast.LENGTH_SHORT).show();
                        });
                }
            }, 1500); // 1.5 second delay for reject simulation
        }, 1000); // 1 second delay for initial processing
    }
    
    private void processTransferToEmbroidery(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate transfer process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing transfer to embroidery...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual transfer
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("✅ Successfully transferred %d pcs to embroidery!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the transfer
                if (currentLot != null) {
                    // Reduce cutting pcs by transferred quantity
                    int currentCuttingPcs = currentLot.getCuttingPcs();
                    currentLot.setCuttingPcs(currentCuttingPcs - quantity);
                    
                    // Add to embroidery receive balance
                    int currentEmbroideryReceive = currentLot.getEmbroideryReceivePcs();
                    currentLot.setEmbroideryReceivePcs(currentEmbroideryReceive + quantity);
                    
                    // Update cutting balance
                    updateCuttingBalance();
                    
                    // Update embroidery receive balance display
                    updateEmbroideryReceiveBalance();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after transfer - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Cutting Pcs: " + currentLot.getCuttingPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase save - Embroidery Receive: " + currentLot.getEmbroideryReceivePcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after transfer");
                            Log.d("LotDetailActivity", "Firebase confirmed - Cutting Pcs: " + updatedLot.getCuttingPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase confirmed - Embroidery Receive: " + updatedLot.getEmbroideryReceivePcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateCuttingBalance();
                            updateEmbroideryReceiveBalance();
                            startEmbroideryReceiveChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after transfer: " + error);
                            Toast.makeText(LotDetailActivity.this, "Transfer completed but failed to update database", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                        
                        @Override
                        public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                        
                        @Override
                        public void onLotDeleted(String lotId) {}
                    });
                        }, 
                        () -> {
                            // Data validation failed
                            Log.e("LotDetailActivity", "❌ Data validation failed before Firebase save");
                            Toast.makeText(LotDetailActivity.this, "Data validation failed", Toast.LENGTH_SHORT).show();
                        });
                }
            }, 1500); // 1.5 second delay for transfer simulation
        }, 1000); // 1 second delay for initial processing
    }
    
    private void showSendToOfficeDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Send Quantity (Pcs.):");
        quantityLabel.setTextSize(16);
        quantityLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        quantityLabel.setPadding(0, 0, 0, dp(8));
        dialogLayout.addView(quantityLabel);
        
        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Enter quantity");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        quantityInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        quantityInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        quantityInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        dialogLayout.addView(quantityInput);
        
        // Current Quantity display below input field
        TextView currentQuantityText = new TextView(this);
        if (currentLot != null) {
            int currentQuantityPcs = currentLot.getEmbroideryReceivePcs();
            currentQuantityText.setText("Available Quantity: " + currentQuantityPcs + " Pcs.");
        } else {
            currentQuantityText.setText("Available Quantity: 0 Pcs.");
        }
        currentQuantityText.setTextSize(12); // Small text
        currentQuantityText.setTextColor(Color.RED); // Red color
        currentQuantityText.setPadding(0, dp(4), 0, dp(8));
        currentQuantityText.setBackground(null);
        currentQuantityText.setClickable(false);
        currentQuantityText.setFocusable(false);
        dialogLayout.addView(currentQuantityText);
        
        // Date input field
        TextView dateLabel = new TextView(this);
        dateLabel.setText("Date:");
        dateLabel.setTextSize(16);
        dateLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        dateLabel.setPadding(0, dp(16), 0, dp(8));
        dialogLayout.addView(dateLabel);
        
        EditText dateInput = new EditText(this);
        dateInput.setHint("Select date (optional)");
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dateInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        dateInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        
        // Set current date as default
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        dateInput.setText(currentDate);
        
        dialogLayout.addView(dateInput);
        
        // Date picker functionality
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    dateInput.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send to Office")
               .setView(dialogLayout)
               .setPositiveButton("Send", (dialog, which) -> {
                   String quantityStr = quantityInput.getText().toString().trim();
                   String dateStr = dateInput.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(quantityStr)) {
                       Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   try {
                       double quantity = Double.parseDouble(quantityStr);
                       if (quantity <= 0) {
                           Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Validate against available quantity
                       if (currentLot != null) {
                           int availableQuantity = currentLot.getEmbroideryReceivePcs() - currentLot.getEmbroideryRejectPcs();
                           if (quantity > availableQuantity) {
                               Toast.makeText(this, "Quantity cannot exceed available quantity (" + availableQuantity + " Pcs)", Toast.LENGTH_SHORT).show();
                               return;
                           }
                       }
                       
                       // Process the send to office operation
                       processSendToOffice(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void processSendToOffice(double quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate send to office process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing send to office...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual send
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("📦 Successfully sent %d Pcs to office!", (int)quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the send to office
                if (currentLot != null) {
                    // Add to office shipment balance
                    int currentOfficeShipment = currentLot.getOfficeShipmentPcs();
                    currentLot.setOfficeShipmentPcs(currentOfficeShipment + (int)quantity);
                    
                    // Update office shipment balance display
                    updateOfficeShipmentBalance();
                    
                    // Update office shipment chart
                    startOfficeShipmentChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after send to office - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Office Shipment: " + currentLot.getOfficeShipmentPcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after send to office");
                            Log.d("LotDetailActivity", "Firebase confirmed - Office Shipment: " + updatedLot.getOfficeShipmentPcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateOfficeShipmentBalance();
                            startOfficeShipmentChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after send to office: " + error);
                            Toast.makeText(LotDetailActivity.this, "Send to office completed but failed to update database", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                        
                        @Override
                        public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                        
                        @Override
                        public void onLotDeleted(String lotId) {}
                    });
                        }, 
                        () -> {
                            // Data validation failed
                            Log.e("LotDetailActivity", "❌ Data validation failed before Firebase save");
                            Toast.makeText(LotDetailActivity.this, "Data validation failed", Toast.LENGTH_SHORT).show();
                        });
                }
            }, 1500); // 1.5 second delay for send simulation
        }, 1000); // 1 second delay for initial processing
    }
    
    private void showSendToOfficeFromEmbroideryDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Send Quantity (Pcs.):");
        quantityLabel.setTextSize(16);
        quantityLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        quantityLabel.setPadding(0, 0, 0, dp(8));
        dialogLayout.addView(quantityLabel);
        
        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Enter quantity");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        quantityInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        quantityInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        quantityInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        dialogLayout.addView(quantityInput);
        
        // Current Quantity display below input field
        TextView currentQuantityText = new TextView(this);
        int availablePcs = 0;
        if (currentLot != null) {
            availablePcs = currentLot.getEmbroideryReceivePcs();
            currentQuantityText.setText("Available Quantity: " + availablePcs + " Pcs.");
        } else {
            currentQuantityText.setText("Available Quantity: 0 Pcs.");
        }
        currentQuantityText.setTextSize(12); // Small text
        currentQuantityText.setTextColor(Color.RED); // Red color
        currentQuantityText.setPadding(0, dp(4), 0, dp(8));
        currentQuantityText.setBackground(null);
        currentQuantityText.setClickable(false);
        currentQuantityText.setFocusable(false);
        dialogLayout.addView(currentQuantityText);
        
        // Date input field
        TextView dateLabel = new TextView(this);
        dateLabel.setText("Date:");
        dateLabel.setTextSize(16);
        dateLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        dateLabel.setPadding(0, dp(16), 0, dp(8));
        dialogLayout.addView(dateLabel);
        
        EditText dateInput = new EditText(this);
        dateInput.setHint("Select date (optional)");
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dateInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        dateInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        
        // Set current date as default
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        dateInput.setText(currentDate);
        
        dialogLayout.addView(dateInput);
        
        // Date picker functionality
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    dateInput.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Create dialog
        int finalAvailablePcs = availablePcs;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send to Office")
               .setView(dialogLayout)
               .setPositiveButton("Send", (dialog, which) -> {
                   String quantityStr = quantityInput.getText().toString().trim();
                   String dateStr = dateInput.getText().toString().trim();
                   
                   if (TextUtils.isEmpty(quantityStr)) {
                       Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   try {
                       int quantity = Integer.parseInt(quantityStr);
                       if (quantity <= 0) {
                           Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Validate against available quantity
                       if (quantity > finalAvailablePcs) {
                           Toast.makeText(this, "Quantity cannot exceed available quantity (" + finalAvailablePcs + " Pcs)", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Process the send to office operation
                       processSendToOfficeFromEmbroidery(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void processSendToOfficeFromEmbroidery(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate send to office process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing send to office from embroidery...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual send
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("📦 Successfully sent %d Pcs to office from embroidery!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the send to office
                if (currentLot != null) {
                    // Add to office shipment balance (in Pcs)
                    int currentOfficeShipmentPcs = currentLot.getOfficeShipmentPcs();
                    currentLot.setOfficeShipmentPcs(currentOfficeShipmentPcs + quantity);
                    
                    // Subtract from embroidery receive balance
                    int currentEmbroideryReceivePcs = currentLot.getEmbroideryReceivePcs();
                    currentLot.setEmbroideryReceivePcs(currentEmbroideryReceivePcs - quantity);
                    
                    // Update office shipment balance display
                    updateOfficeShipmentBalance();
                    
                    // Update office shipment chart
                    startOfficeShipmentChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after send to office from embroidery - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Office Shipment: " + currentLot.getOfficeShipmentPcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after send to office from embroidery");
                            Log.d("LotDetailActivity", "Firebase confirmed - Office Shipment: " + updatedLot.getOfficeShipmentPcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateOfficeShipmentBalance();
                            startOfficeShipmentChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after send to office from embroidery: " + error);
                            Toast.makeText(LotDetailActivity.this, "Send to office completed but failed to update database", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                        
                        @Override
                        public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                        
                        @Override
                        public void onLotDeleted(String lotId) {}
                    });
                        }, 
                        () -> {
                            // Data validation failed
                            Log.e("LotDetailActivity", "❌ Data validation failed before Firebase save");
                            Toast.makeText(LotDetailActivity.this, "Data validation failed", Toast.LENGTH_SHORT).show();
                        });
                }
            }, 1500); // 1.5 second delay for send simulation
        }, 1000); // 1 second delay for initial processing
    }
}