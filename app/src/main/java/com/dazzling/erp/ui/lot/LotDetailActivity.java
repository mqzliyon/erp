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
import android.widget.Spinner;
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
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.dazzling.erp.services.FirestoreService;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.ArrayAdapter;
import android.text.InputType;
import java.util.Date;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

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
    private int previousTab = 0; // Track previous main tab
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
    
    // Factory Balance sub-menu variables
    private int selectedFactoryBalanceTab = 0; // 0 = Total, 1 = A Grade, 2 = B Grade
    private LinearLayout factoryBalanceSubMenuBar;
    private TextView factoryBalanceTotalText;
    private TextView factoryBalanceAGradeText;
    private TextView factoryBalanceBGradeText;
    
    // Factory Balance chart variables
    private LineChart factoryBalanceTotalChart;
    private LineChart factoryBalanceAGradeChart;
    private LineChart factoryBalanceBGradeChart;
    private LineDataSet factoryBalanceTotalDataSet;
    private LineDataSet factoryBalanceAGradeDataSet;
    private LineDataSet factoryBalanceBGradeDataSet;
    
    // Lot data
    private String lotId;
    private com.dazzling.erp.models.Lot currentLot;
    private com.dazzling.erp.services.FirestoreService firestoreService;

    private LineChart factoryBalanceAGradeRejectChart;
    private LineDataSet factoryBalanceAGradeRejectDataSet;
    private TextView factoryBalanceAGradeRejectText;
    
    private LineChart factoryBalanceBGradeRejectChart;
    private LineDataSet factoryBalanceBGradeRejectDataSet;
    private TextView factoryBalanceBGradeRejectText;

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
        // Only reset selectedFactoryBalanceTab if switching from a different tab
        if (tabIndex == 3 && previousTab != 3) {
            selectedFactoryBalanceTab = 0;
        }
        previousTab = selectedTab;
        selectedTab = tabIndex;
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
                contentView = createFactoryBalanceContent();
                break;
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
        
        // If it's factory balance tab, ensure the total content is shown
        if (tabIndex == 3) {
            // Small delay to ensure the view is properly added
            new Handler(Looper.getMainLooper()).post(() -> {
                showFactoryBalanceSubContent(selectedFactoryBalanceTab);
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

    // Factory Balance tab content: Card with sub-menu and content
    private View createFactoryBalanceContent() {
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

        factoryBalanceSubMenuBar = new LinearLayout(this);
        factoryBalanceSubMenuBar.setOrientation(LinearLayout.HORIZONTAL);
        factoryBalanceSubMenuBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        factoryBalanceSubMenuBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        factoryBalanceSubMenuBar.setGravity(Gravity.CENTER_HORIZONTAL);
        factoryBalanceSubMenuBar.setMinimumHeight(dp(48)); // Ensure minimum height for visibility

        // Create sub-menu tabs
        String[] factoryBalanceTabs = {"Total Balance", "A Grade", "B Grade"};
        for (int i = 0; i < factoryBalanceTabs.length; i++) {
            final int index = i;
            LinearLayout subTab = createFactoryBalanceSubTab(factoryBalanceTabs[i], i == selectedFactoryBalanceTab);
            subTab.setOnClickListener(v -> {
                if (selectedFactoryBalanceTab != index) {
                    selectedFactoryBalanceTab = index;
                    updateFactoryBalanceSubMenu();
                    showFactoryBalanceSubContent(index);
                }
            });
            factoryBalanceSubMenuBar.addView(subTab);
            Log.d("FactoryBalanceSubMenu", "Added sub-tab: " + factoryBalanceTabs[i]);
        }

        scrollView.addView(factoryBalanceSubMenuBar);
        mainContainer.addView(scrollView);
        
        // Update the sub-menu to ensure proper styling
        updateFactoryBalanceSubMenu();
        
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
            int menuBarWidth = factoryBalanceSubMenuBar.getWidth();
            if (scrollViewWidth > menuBarWidth) {
                int scrollX = (menuBarWidth - scrollViewWidth) / 2;
                scrollView.scrollTo(scrollX, 0);
            }
        });

        // Content container
        FrameLayout factoryBalanceContentContainer = new FrameLayout(this);
        factoryBalanceContentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(factoryBalanceContentContainer);

        // Show initial content (Total Balance by default)
        // Small delay to ensure the view hierarchy is properly set up
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d("FactoryBalanceContent", "Showing initial content for tab: " + selectedFactoryBalanceTab);
            showFactoryBalanceSubContent(selectedFactoryBalanceTab);
        });

        Log.d("FactoryBalanceContent", "Created factory balance content with " + factoryBalanceSubMenuBar.getChildCount() + " sub-tabs");
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

    // Create factory balance sub-tab
    private LinearLayout createFactoryBalanceSubTab(String label, boolean selected) {
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
        setFactoryBalanceSubTabBackground(tabLayout, selected);
        tabLayout.setElevation(selected ? dp(4) : dp(0));

        // Label
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        labelView.setTextColor(selected ? Color.WHITE : Color.BLACK);
        tabLayout.addView(labelView);
        
        Log.d("FactoryBalanceSubTab", "Created sub-tab with label: " + label + ", selected: " + selected);

        // Ripple effect
        tabLayout.setForeground(ContextCompat.getDrawable(this, R.drawable.ripple_effect));
        return tabLayout;
    }

    // Set factory balance sub-tab background
    private void setFactoryBalanceSubTabBackground(LinearLayout tabLayout, boolean selected) {
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

    // Update factory balance sub-menu appearance
    private void updateFactoryBalanceSubMenu() {
        if (factoryBalanceSubMenuBar != null) {
            Log.d("FactoryBalanceSubMenu", "Updating sub-menu with " + factoryBalanceSubMenuBar.getChildCount() + " tabs, selected: " + selectedFactoryBalanceTab);
            for (int i = 0; i < factoryBalanceSubMenuBar.getChildCount(); i++) {
                LinearLayout tabView = (LinearLayout) factoryBalanceSubMenuBar.getChildAt(i);
                boolean isSelected = (i == selectedFactoryBalanceTab);
                setFactoryBalanceSubTabBackground(tabView, isSelected);
                tabView.setElevation(isSelected ? dp(4) : dp(0));
                tabView.animate().scaleX(isSelected ? 1.05f : 1f).scaleY(isSelected ? 1.05f : 1f).setDuration(150).start();
            }
        } else {
            Log.e("FactoryBalanceSubMenu", "factoryBalanceSubMenuBar is null");
        }
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

    // Show factory balance sub-content
    private void showFactoryBalanceSubContent(int subTabIndex) {
        Log.d("FactoryBalanceSubContent", "Showing sub-content for index: " + subTabIndex);
        
        // Find the content container (third child of main container after scroll view and underline)
        if (contentContainer.getChildCount() > 0) {
            View mainContainer = contentContainer.getChildAt(0);
            if (mainContainer instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) mainContainer;
                Log.d("FactoryBalanceSubContent", "Container child count: " + container.getChildCount());
                
                if (container.getChildCount() > 2) {
                    FrameLayout factoryBalanceContentContainer = (FrameLayout) container.getChildAt(2);
                    factoryBalanceContentContainer.removeAllViews();
                    
                    View subContent;
                    switch (subTabIndex) {
                        case 0: // Total Balance
                            subContent = createFactoryBalanceTotalContent();
                            Log.d("FactoryBalanceSubContent", "Created Total Balance content");
                            break;
                        case 1: // A Grade
                            subContent = createFactoryBalanceAGradeContent();
                            Log.d("FactoryBalanceSubContent", "Created A Grade content");
                            break;
                        case 2: // B Grade
                            subContent = createFactoryBalanceBGradeContent();
                            Log.d("FactoryBalanceSubContent", "Created B Grade content");
                            break;
                        default:
                            subContent = createPlaceholderContent("Factory Balance");
                            Log.d("FactoryBalanceSubContent", "Created placeholder content");
                            break;
                    }
                    
                    factoryBalanceContentContainer.addView(subContent);
                    
                    // Force layout update
                    factoryBalanceContentContainer.requestLayout();
                    factoryBalanceContentContainer.invalidate();
                    Log.d("FactoryBalanceSubContent", "Content added and layout updated");
                } else {
                    Log.e("FactoryBalanceSubContent", "Container doesn't have enough children. Expected > 2, got: " + container.getChildCount());
                }
            } else {
                Log.e("FactoryBalanceSubContent", "Main container is not LinearLayout");
            }
        } else {
            Log.e("FactoryBalanceSubContent", "Content container has no children");
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
            showSendToFactoryBalanceDialog();
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

        // History Button
        MaterialButton historyBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        historyBtn.setText("History");
        historyBtn.setCornerRadius(dp(30));
        historyBtn.setTextSize(16);
        historyBtn.setElevation(dp(4));
        historyBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        historyBtn.setTextColor(Color.WHITE);
        historyBtn.setBackgroundColor(Color.parseColor("#1976D2")); // Blue color for History
        historyBtn.setOnClickListener(v -> {
            showEmbroideryRejectHistoryDialog();
        });
        vbox.addView(historyBtn);

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

    // Update factory balance total
    private void updateFactoryBalanceTotal() {
        if (factoryBalanceTotalText != null && currentLot != null) {
            // Use the actual factory balance total data from the lot
            int totalPcs = currentLot.getTotalFactoryBalancePcs();
            factoryBalanceTotalText.setText("Total Factory Balance: " + totalPcs + " Pcs.");
        }
    }

    // Update factory balance A Grade
    private void updateFactoryBalanceAGrade() {
        if (factoryBalanceAGradeText != null && currentLot != null) {
            // Use the actual factory balance A Grade data from the lot
            int aGradePcs = currentLot.getFactoryBalanceAGradePcs();
            factoryBalanceAGradeText.setText("A Grade Balance: " + aGradePcs + " Pcs.");
        }
    }

    // Update factory balance B Grade
    private void updateFactoryBalanceBGrade() {
        if (factoryBalanceBGradeText != null && currentLot != null) {
            // Use the actual factory balance B Grade data from the lot
            int bGradePcs = currentLot.getFactoryBalanceBGradePcs();
            factoryBalanceBGradeText.setText("B Grade Balance: " + bGradePcs + " Pcs.");
        }
    }

    // Factory Balance Total Chart Simulation
    private void startFactoryBalanceTotalChartSimulation() {
        if (factoryBalanceTotalChart == null) return;
        
        // Get the current factory balance total from the lot data
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getTotalFactoryBalancePcs();
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
        
        factoryBalanceTotalDataSet = new LineDataSet(entries, "Factory Balance Total");
        factoryBalanceTotalDataSet.setColor(Color.parseColor("#9C27B0")); // Purple color for total
        factoryBalanceTotalDataSet.setCircleColor(Color.parseColor("#9C27B0"));
        factoryBalanceTotalDataSet.setLineWidth(3f);
        factoryBalanceTotalDataSet.setCircleRadius(5f);
        factoryBalanceTotalDataSet.setDrawValues(true);
        factoryBalanceTotalDataSet.setValueTextSize(10f);
        factoryBalanceTotalDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceTotalDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        factoryBalanceTotalDataSet.setDrawCircles(true);
        factoryBalanceTotalDataSet.setDrawCircleHole(true);
        factoryBalanceTotalDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(factoryBalanceTotalDataSet);
        factoryBalanceTotalChart.setData(data);
        
        // Configure chart appearance
        factoryBalanceTotalChart.getXAxis().setDrawLabels(true);
        factoryBalanceTotalChart.getXAxis().setGranularity(1f);
        factoryBalanceTotalChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        factoryBalanceTotalChart.getAxisRight().setEnabled(false);
        factoryBalanceTotalChart.getAxisLeft().setDrawGridLines(true);
        factoryBalanceTotalChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        factoryBalanceTotalChart.getAxisLeft().setGridLineWidth(0.5f);
        factoryBalanceTotalChart.getXAxis().setDrawGridLines(false);
        factoryBalanceTotalChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        factoryBalanceTotalChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            factoryBalanceTotalChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            factoryBalanceTotalChart.getAxisLeft().setAxisMaximum(50f); // Lower default for empty state
        }
        
        factoryBalanceTotalChart.invalidate();
    }

    // Factory Balance A Grade Chart Simulation
    private void startFactoryBalanceAGradeChartSimulation() {
        if (factoryBalanceAGradeChart == null) return;
        
        // Get the current factory balance A Grade from the lot data
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getFactoryBalanceAGradePcs();
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
        
        factoryBalanceAGradeDataSet = new LineDataSet(entries, "Factory Balance A Grade");
        factoryBalanceAGradeDataSet.setColor(Color.parseColor("#4CAF50")); // Green color for A Grade
        factoryBalanceAGradeDataSet.setCircleColor(Color.parseColor("#4CAF50"));
        factoryBalanceAGradeDataSet.setLineWidth(3f);
        factoryBalanceAGradeDataSet.setCircleRadius(5f);
        factoryBalanceAGradeDataSet.setDrawValues(true);
        factoryBalanceAGradeDataSet.setValueTextSize(10f);
        factoryBalanceAGradeDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceAGradeDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        factoryBalanceAGradeDataSet.setDrawCircles(true);
        factoryBalanceAGradeDataSet.setDrawCircleHole(true);
        factoryBalanceAGradeDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(factoryBalanceAGradeDataSet);
        factoryBalanceAGradeChart.setData(data);
        
        // Configure chart appearance
        factoryBalanceAGradeChart.getXAxis().setDrawLabels(true);
        factoryBalanceAGradeChart.getXAxis().setGranularity(1f);
        factoryBalanceAGradeChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        factoryBalanceAGradeChart.getAxisRight().setEnabled(false);
        factoryBalanceAGradeChart.getAxisLeft().setDrawGridLines(true);
        factoryBalanceAGradeChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        factoryBalanceAGradeChart.getAxisLeft().setGridLineWidth(0.5f);
        factoryBalanceAGradeChart.getXAxis().setDrawGridLines(false);
        factoryBalanceAGradeChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        factoryBalanceAGradeChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            factoryBalanceAGradeChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            factoryBalanceAGradeChart.getAxisLeft().setAxisMaximum(50f); // Lower default for empty state
        }
        
        factoryBalanceAGradeChart.invalidate();
    }

    // Factory Balance B Grade Chart Simulation
    private void startFactoryBalanceBGradeChartSimulation() {
        if (factoryBalanceBGradeChart == null) return;
        
        // Get the current factory balance B Grade from the lot data
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getFactoryBalanceBGradePcs();
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
        
        factoryBalanceBGradeDataSet = new LineDataSet(entries, "Factory Balance B Grade");
        factoryBalanceBGradeDataSet.setColor(Color.parseColor("#FF9800")); // Orange color for B Grade
        factoryBalanceBGradeDataSet.setCircleColor(Color.parseColor("#FF9800"));
        factoryBalanceBGradeDataSet.setLineWidth(3f);
        factoryBalanceBGradeDataSet.setCircleRadius(5f);
        factoryBalanceBGradeDataSet.setDrawValues(true);
        factoryBalanceBGradeDataSet.setValueTextSize(10f);
        factoryBalanceBGradeDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceBGradeDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        factoryBalanceBGradeDataSet.setDrawCircles(true);
        factoryBalanceBGradeDataSet.setDrawCircleHole(true);
        factoryBalanceBGradeDataSet.setCircleHoleColor(Color.WHITE);
        
        LineData data = new LineData(factoryBalanceBGradeDataSet);
        factoryBalanceBGradeChart.setData(data);
        
        // Configure chart appearance
        factoryBalanceBGradeChart.getXAxis().setDrawLabels(true);
        factoryBalanceBGradeChart.getXAxis().setGranularity(1f);
        factoryBalanceBGradeChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        
        // Configure axes
        factoryBalanceBGradeChart.getAxisRight().setEnabled(false);
        factoryBalanceBGradeChart.getAxisLeft().setDrawGridLines(true);
        factoryBalanceBGradeChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        factoryBalanceBGradeChart.getAxisLeft().setGridLineWidth(0.5f);
        factoryBalanceBGradeChart.getXAxis().setDrawGridLines(false);
        factoryBalanceBGradeChart.getLegend().setEnabled(false);
        
        // Set Y-axis label
        factoryBalanceBGradeChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            factoryBalanceBGradeChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            factoryBalanceBGradeChart.getAxisLeft().setAxisMaximum(50f); // Lower default for empty state
        }
        
        factoryBalanceBGradeChart.invalidate();
    }

    // Factory Balance Total content
    private View createFactoryBalanceTotalContent() {
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
        factoryBalanceTotalChart = new LineChart(this);
        factoryBalanceTotalChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        factoryBalanceTotalChart.setNoDataText("Loading chart...");
        factoryBalanceTotalChart.setTouchEnabled(false);
        factoryBalanceTotalChart.setDescription(new Description());
        vbox.addView(factoryBalanceTotalChart);

        // Factory Balance Total
        factoryBalanceTotalText = new TextView(this);
        if (currentLot != null) {
            // Use the actual factory balance total data from the lot
            int totalPcs = currentLot.getTotalFactoryBalancePcs();
            factoryBalanceTotalText.setText("Total Factory Balance: " + totalPcs + " Pcs.");
        } else {
            factoryBalanceTotalText.setText("Total Factory Balance: 0 Pcs.");
        }
        factoryBalanceTotalText.setTextSize(14);
        factoryBalanceTotalText.setTypeface(null, android.graphics.Typeface.BOLD);
        factoryBalanceTotalText.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceTotalText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(factoryBalanceTotalText);

        // Add to A Grade Button
        MaterialButton aGradeBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        aGradeBtn.setText("History");
        aGradeBtn.setCornerRadius(dp(30));
        aGradeBtn.setTextSize(16);
        aGradeBtn.setElevation(dp(4));
        aGradeBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        aGradeBtn.setTextColor(Color.WHITE);
        aGradeBtn.setBackgroundColor(Color.parseColor("#1976D2")); // Blue color for History
        aGradeBtn.setOnClickListener(v -> {
            showHistoryDialog();
        });

        vbox.addView(aGradeBtn);

        card.addView(vbox);
        
        // Start chart simulation
        startFactoryBalanceTotalChartSimulation();
        return card;
    }

    // Factory Balance A Grade content
    private View createFactoryBalanceAGradeContent() {
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
        factoryBalanceAGradeChart = new com.github.mikephil.charting.charts.LineChart(this);
        factoryBalanceAGradeChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        factoryBalanceAGradeChart.setNoDataText("Loading chart...");
        factoryBalanceAGradeChart.setTouchEnabled(false);
        factoryBalanceAGradeChart.setDescription(new com.github.mikephil.charting.components.Description());
        vbox.addView(factoryBalanceAGradeChart);

        // Factory Balance A Grade
        factoryBalanceAGradeText = new TextView(this);
        if (currentLot != null) {
            int aGradePcs = currentLot.getFactoryBalanceAGradePcs();
            factoryBalanceAGradeText.setText("A Grade Balance: " + aGradePcs + " Pcs.");
        } else {
            factoryBalanceAGradeText.setText("A Grade Balance: 0 Pcs.");
        }
        factoryBalanceAGradeText.setTextSize(14);
        factoryBalanceAGradeText.setTypeface(null, android.graphics.Typeface.BOLD);
        factoryBalanceAGradeText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceAGradeText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(factoryBalanceAGradeText);

        // Send to Office Button
        com.google.android.material.button.MaterialButton officeBtn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        officeBtn.setText("Send to Office");
        officeBtn.setCornerRadius(dp(30));
        officeBtn.setTextSize(16);
        officeBtn.setElevation(dp(4));
        officeBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        officeBtn.setTextColor(android.graphics.Color.BLACK);
        officeBtn.setBackgroundColor(android.graphics.Color.parseColor("#2196F3")); // Blue color for office
        officeBtn.setOnClickListener(v -> {
            showSendToOfficeFromAGradeDialog();
        });
        vbox.addView(officeBtn);

        // Add Mark as Reject button below the Send to Office button
        com.google.android.material.button.MaterialButton markAsRejectBtn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        markAsRejectBtn.setText("Mark as Reject");
        markAsRejectBtn.setCornerRadius(dp(30));
        markAsRejectBtn.setTextSize(16);
        markAsRejectBtn.setElevation(dp(4));
        LinearLayout.LayoutParams markAsRejectBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        markAsRejectBtnLp.setMargins(0, 0, 0, dp(16)); // Add bottom margin
        markAsRejectBtn.setLayoutParams(markAsRejectBtnLp);
        markAsRejectBtn.setTextColor(android.graphics.Color.WHITE);
        markAsRejectBtn.setBackgroundColor(android.graphics.Color.parseColor("#F44336")); // Red color
        markAsRejectBtn.setOnClickListener(v -> showMarkAsARejectDialog());
        vbox.addView(markAsRejectBtn);

        // --- REJECT BALANCE & CHART SECTION ---
        // Add Reject Balance chart and then text below it
        factoryBalanceAGradeRejectChart = new com.github.mikephil.charting.charts.LineChart(this);
        factoryBalanceAGradeRejectChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        factoryBalanceAGradeRejectChart.setNoDataText("Loading chart...");
        factoryBalanceAGradeRejectChart.setTouchEnabled(false);
        factoryBalanceAGradeRejectChart.setDescription(new com.github.mikephil.charting.components.Description());
        vbox.addView(factoryBalanceAGradeRejectChart);

        factoryBalanceAGradeRejectText = new TextView(this);
        if (currentLot != null) {
            int rejectPcs = currentLot.getFactoryBalanceRejectPcs();
            factoryBalanceAGradeRejectText.setText("A Grade Reject Balance: " + rejectPcs + " Pcs.");
        } else {
            factoryBalanceAGradeRejectText.setText("A Grade Reject Balance: 0 Pcs.");
        }
        factoryBalanceAGradeRejectText.setTextSize(14);
        factoryBalanceAGradeRejectText.setTypeface(null, android.graphics.Typeface.BOLD);
        factoryBalanceAGradeRejectText.setTextColor(android.graphics.Color.BLACK);
        factoryBalanceAGradeRejectText.setPadding(0, dp(8), 0, dp(8));
        vbox.addView(factoryBalanceAGradeRejectText);

        card.addView(vbox);
        // Start chart simulation and update reject balance
        updateFactoryBalanceAGradeReject();
        startFactoryBalanceAGradeRejectChartSimulation();
        startFactoryBalanceAGradeChartSimulation();

        // --- Wrap the card in a vertical ScrollView ---
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(card);
        return scrollView;
    }

    // Factory Balance B Grade content
    private View createFactoryBalanceBGradeContent() {
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
        factoryBalanceBGradeChart = new com.github.mikephil.charting.charts.LineChart(this);
        factoryBalanceBGradeChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        factoryBalanceBGradeChart.setNoDataText("Loading chart...");
        factoryBalanceBGradeChart.setTouchEnabled(false);
        factoryBalanceBGradeChart.setDescription(new com.github.mikephil.charting.components.Description());
        vbox.addView(factoryBalanceBGradeChart);

        // Factory Balance B Grade
        factoryBalanceBGradeText = new TextView(this);
        if (currentLot != null) {
            int bGradePcs = currentLot.getFactoryBalanceBGradePcs();
            factoryBalanceBGradeText.setText("B Grade Balance: " + bGradePcs + " Pcs.");
        } else {
            factoryBalanceBGradeText.setText("B Grade Balance: 0 Pcs.");
        }
        factoryBalanceBGradeText.setTextSize(14);
        factoryBalanceBGradeText.setTypeface(null, android.graphics.Typeface.BOLD);
        factoryBalanceBGradeText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceBGradeText.setPadding(0, dp(16), 0, dp(16));
        vbox.addView(factoryBalanceBGradeText);

        // Send to Office Button
        com.google.android.material.button.MaterialButton officeBtn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        officeBtn.setText("Send to Office");
        officeBtn.setCornerRadius(dp(30));
        officeBtn.setTextSize(16);
        officeBtn.setElevation(dp(4));
        officeBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        officeBtn.setTextColor(android.graphics.Color.BLACK);
        officeBtn.setBackgroundColor(android.graphics.Color.parseColor("#2196F3")); // Blue color for office
        officeBtn.setOnClickListener(v -> {
            showSendToOfficeFromBGradeDialog();
        });
        vbox.addView(officeBtn);

        // Add Mark as Reject button below the Send to Office button
        com.google.android.material.button.MaterialButton markAsRejectBtn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        markAsRejectBtn.setText("Mark as Reject");
        markAsRejectBtn.setCornerRadius(dp(30));
        markAsRejectBtn.setTextSize(16);
        markAsRejectBtn.setElevation(dp(4));
        markAsRejectBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        markAsRejectBtn.setTextColor(android.graphics.Color.WHITE);
        markAsRejectBtn.setBackgroundColor(android.graphics.Color.parseColor("#F44336")); // Red color
        markAsRejectBtn.setOnClickListener(v -> showMarkAsBRejectDialog());
        vbox.addView(markAsRejectBtn);

        card.addView(vbox);
        // Start chart simulation
        startFactoryBalanceBGradeChartSimulation();

        // --- REJECT BALANCE & CHART SECTION FOR B GRADE ---
        // Add Reject Balance chart and then text below it
        factoryBalanceBGradeRejectChart = new com.github.mikephil.charting.charts.LineChart(this);
        factoryBalanceBGradeRejectChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        factoryBalanceBGradeRejectChart.setNoDataText("Loading chart...");
        factoryBalanceBGradeRejectChart.setTouchEnabled(false);
        factoryBalanceBGradeRejectChart.setDescription(new com.github.mikephil.charting.components.Description());
        vbox.addView(factoryBalanceBGradeRejectChart);

        factoryBalanceBGradeRejectText = new TextView(this);
        if (currentLot != null) {
            int rejectPcs = currentLot.getFactoryBalanceRejectPcs();
            factoryBalanceBGradeRejectText.setText("B Grade Reject Balance: " + rejectPcs + " Pcs.");
        } else {
            factoryBalanceBGradeRejectText.setText("B Grade Reject Balance: 0 Pcs.");
        }
        factoryBalanceBGradeRejectText.setTextSize(14);
        factoryBalanceBGradeRejectText.setTypeface(null, android.graphics.Typeface.BOLD);
        factoryBalanceBGradeRejectText.setTextColor(android.graphics.Color.BLACK);
        factoryBalanceBGradeRejectText.setPadding(0, dp(8), 0, dp(8));
        vbox.addView(factoryBalanceBGradeRejectText);

        // --- Wrap the card in a vertical ScrollView ---
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(card);
        
        // Start B Grade reject chart simulation and update reject balance
        updateFactoryBalanceBGradeReject();
        startFactoryBalanceBGradeRejectChartSimulation();
        
        return scrollView;
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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
        
        android.app.AlertDialog dialog = builder.create();
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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
        
        android.app.AlertDialog dialog = builder.create();
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
                    
                    // Add to embroidery reject history
                    java.util.List<com.dazzling.erp.models.Transfer> history = currentLot.getEmbroideryRejectHistory();
                    if (history == null) history = new java.util.ArrayList<>();
                    history.add(new com.dazzling.erp.models.Transfer(quantity, new java.util.Date(), "Embroidery → Reject on " + date));
                    currentLot.setEmbroideryRejectHistory(history);
                    
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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
        
        android.app.AlertDialog dialog = builder.create();
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
        final int availablePcs;
        if (currentLot != null) {
            availablePcs = currentLot.getEmbroideryReceivePcs();
            currentQuantityText.setText("Available Quantity: " + availablePcs + " Pcs.");
        } else {
            availablePcs = 0;
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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
                       if (quantity > availablePcs) {
                           Toast.makeText(this, "Quantity cannot exceed available quantity (" + availablePcs + " Pcs)", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Process the send to office operation
                       processSendToOfficeFromEmbroidery(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        android.app.AlertDialog dialog = builder.create();
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

    // Factory Balance Dialog Methods
    private void showAddToAGradeDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Add Quantity (Pcs.):");
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add to A Grade")
               .setView(dialogLayout)
               .setPositiveButton("Add", (dialog, which) -> {
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
                       
                       // Process the add to A Grade operation
                       processAddToAGrade(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAddToBGradeDialog() {
        // Create dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Quantity input field
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Add Quantity (Pcs.):");
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add to B Grade")
               .setView(dialogLayout)
               .setPositiveButton("Add", (dialog, which) -> {
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
                       
                       // Process the add to B Grade operation
                       processAddToBGrade(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSendToOfficeFromAGradeDialog() {
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
        final int availablePcs;
        if (currentLot != null) {
            availablePcs = currentLot.getFactoryBalanceAGradePcs();
            currentQuantityText.setText("Available A Grade Quantity: " + availablePcs + " Pcs.");
        } else {
            availablePcs = 0;
            currentQuantityText.setText("Available A Grade Quantity: 0 Pcs.");
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Send A Grade to Office")
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
                       if (quantity > availablePcs) {
                           Toast.makeText(this, "Quantity cannot exceed available quantity (" + availablePcs + " Pcs)", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Process the send to office operation
                       processSendToOfficeFromAGrade(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSendToOfficeFromBGradeDialog() {
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
        final int availablePcs;
        if (currentLot != null) {
            availablePcs = currentLot.getFactoryBalanceBGradePcs();
            currentQuantityText.setText("Available B Grade Quantity: " + availablePcs + " Pcs.");
        } else {
            availablePcs = 0;
            currentQuantityText.setText("Available B Grade Quantity: 0 Pcs.");
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Send B Grade to Office")
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
                       if (quantity > availablePcs) {
                           Toast.makeText(this, "Quantity cannot exceed available quantity (" + availablePcs + " Pcs)", Toast.LENGTH_SHORT).show();
                           return;
                       }
                       
                       // Process the send to office operation
                       processSendToOfficeFromBGrade(quantity, dateStr);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void processAddToAGrade(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate add to A Grade process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing add to A Grade...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual add
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("✅ Successfully added %d Pcs to A Grade!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the add to A Grade
                if (currentLot != null) {
                    // Add to A Grade balance
                    int currentAGradePcs = currentLot.getFactoryBalanceAGradePcs();
                    currentLot.setFactoryBalanceAGradePcs(currentAGradePcs + quantity);
                    
                    // Update A Grade balance display
                    updateFactoryBalanceAGrade();
                    
                    // Update A Grade chart
                    startFactoryBalanceAGradeChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after add to A Grade - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - A Grade: " + currentLot.getFactoryBalanceAGradePcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after add to A Grade");
                            Log.d("LotDetailActivity", "Firebase confirmed - A Grade: " + updatedLot.getFactoryBalanceAGradePcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateFactoryBalanceAGrade();
                            startFactoryBalanceAGradeChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after add to A Grade: " + error);
                            Toast.makeText(LotDetailActivity.this, "Add to A Grade completed but failed to update database", Toast.LENGTH_SHORT).show();
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
            }, 1500); // 1.5 second delay for add simulation
        }, 1000); // 1 second delay for initial processing
    }

    private void processAddToBGrade(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate add to B Grade process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing add to B Grade...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual add
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("✅ Successfully added %d Pcs to B Grade!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the add to B Grade
                if (currentLot != null) {
                    // Add to B Grade balance
                    int currentBGradePcs = currentLot.getFactoryBalanceBGradePcs();
                    currentLot.setFactoryBalanceBGradePcs(currentBGradePcs + quantity);
                    
                    // Update B Grade balance display
                    updateFactoryBalanceBGrade();
                    
                    // Update B Grade chart
                    startFactoryBalanceBGradeChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after add to B Grade - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - B Grade: " + currentLot.getFactoryBalanceBGradePcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after add to B Grade");
                            Log.d("LotDetailActivity", "Firebase confirmed - B Grade: " + updatedLot.getFactoryBalanceBGradePcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateFactoryBalanceBGrade();
                            startFactoryBalanceBGradeChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after add to B Grade: " + error);
                            Toast.makeText(LotDetailActivity.this, "Add to B Grade completed but failed to update database", Toast.LENGTH_SHORT).show();
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
            }, 1500); // 1.5 second delay for add simulation
        }, 1000); // 1 second delay for initial processing
    }

    private void processSendToOfficeFromAGrade(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate send to office from A Grade process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing send to office from A Grade...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual send
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("📦 Successfully sent %d Pcs to office from A Grade!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the send to office from A Grade
                if (currentLot != null) {
                    // Add to office shipment balance (in Pcs)
                    int currentOfficeShipmentPcs = currentLot.getOfficeShipmentPcs();
                    currentLot.setOfficeShipmentPcs(currentOfficeShipmentPcs + quantity);
                    
                    // Subtract from A Grade balance
                    int currentAGradePcs = currentLot.getFactoryBalanceAGradePcs();
                    currentLot.setFactoryBalanceAGradePcs(currentAGradePcs - quantity);
                    
                    // Update office shipment balance display
                    updateOfficeShipmentBalance();
                    
                    // Update office shipment chart
                    startOfficeShipmentChartSimulation();
                    
                    // Update A Grade balance display
                    updateFactoryBalanceAGrade();
                    
                    // Update A Grade chart
                    startFactoryBalanceAGradeChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after send to office from A Grade - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Office Shipment: " + currentLot.getOfficeShipmentPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase save - A Grade: " + currentLot.getFactoryBalanceAGradePcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after send to office from A Grade");
                            Log.d("LotDetailActivity", "Firebase confirmed - Office Shipment: " + updatedLot.getOfficeShipmentPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase confirmed - A Grade: " + updatedLot.getFactoryBalanceAGradePcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateOfficeShipmentBalance();
                            startOfficeShipmentChartSimulation();
                            updateFactoryBalanceAGrade();
                            startFactoryBalanceAGradeChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after send to office from A Grade: " + error);
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

    private void processSendToOfficeFromBGrade(int quantity, String date) {
        // Show loading animation
        showLoading(true);
        
        // Simulate send to office from B Grade process with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Show progress message
            Toast.makeText(this, "Processing send to office from B Grade...", Toast.LENGTH_SHORT).show();
            
            // Simulate another delay for the actual send
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Hide loading and show success message
                showLoading(false);
                
                // Show success message with details
                String successMessage = String.format("📦 Successfully sent %d Pcs to office from B Grade!", quantity);
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Update the lot data to reflect the send to office from B Grade
                if (currentLot != null) {
                    // Add to office shipment balance (in Pcs)
                    int currentOfficeShipmentPcs = currentLot.getOfficeShipmentPcs();
                    currentLot.setOfficeShipmentPcs(currentOfficeShipmentPcs + quantity);
                    
                    // Subtract from B Grade balance
                    int currentBGradePcs = currentLot.getFactoryBalanceBGradePcs();
                    currentLot.setFactoryBalanceBGradePcs(currentBGradePcs - quantity);
                    
                    // Update office shipment balance display
                    updateOfficeShipmentBalance();
                    
                    // Update office shipment chart
                    startOfficeShipmentChartSimulation();
                    
                    // Update B Grade balance display
                    updateFactoryBalanceBGrade();
                    
                    // Update B Grade chart
                    startFactoryBalanceBGradeChartSimulation();
                    
                    // Verify data before saving to Firebase
                    firestoreService.verifyLotDataSave(currentLot, 
                        () -> {
                            // Data is valid, proceed with Firebase save
                            Log.d("LotDetailActivity", "Saving lot to Firebase after send to office from B Grade - ID: " + currentLot.getId());
                            Log.d("LotDetailActivity", "Firebase save - Office Shipment: " + currentLot.getOfficeShipmentPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase save - B Grade: " + currentLot.getFactoryBalanceBGradePcs() + " pcs");
                            
                            firestoreService.updateLot(currentLot, new FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            Log.d("LotDetailActivity", "✅ Lot successfully updated in Firebase after send to office from B Grade");
                            Log.d("LotDetailActivity", "Firebase confirmed - Office Shipment: " + updatedLot.getOfficeShipmentPcs() + " pcs");
                            Log.d("LotDetailActivity", "Firebase confirmed - B Grade: " + updatedLot.getFactoryBalanceBGradePcs() + " pcs");
                            // Update the charts to reflect the new balances
                            updateOfficeShipmentBalance();
                            startOfficeShipmentChartSimulation();
                            updateFactoryBalanceBGrade();
                            startFactoryBalanceBGradeChartSimulation();
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("LotDetailActivity", "❌ Failed to update lot in Firebase after send to office from B Grade: " + error);
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

    // --- Send to Factory Balance Dialog ---
    private void showSendToFactoryBalanceDialog() {
        // Dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(24), dp(24), dp(24), dp(24));

        // Grade Spinner
        TextView gradeLabel = new TextView(this);
        gradeLabel.setText("Grade:");
        gradeLabel.setTextSize(16);
        gradeLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        gradeLabel.setPadding(0, 0, 0, dp(8));
        dialogLayout.addView(gradeLabel);

        Spinner gradeSpinner = new Spinner(this);
        String[] grades = {"A Grade", "B Grade"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gradeSpinner.setAdapter(gradeAdapter);
        dialogLayout.addView(gradeSpinner);

        // Show available embroidery quantity
        int availableEmbroideryLabel = (currentLot != null) ? currentLot.getEmbroideryReceivePcs() : 0;
        TextView availableQtyLabel = new TextView(this);
        availableQtyLabel.setText("Available: " + availableEmbroideryLabel + " Pcs.");
        availableQtyLabel.setTextSize(14);
        availableQtyLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        availableQtyLabel.setPadding(0, dp(8), 0, dp(8));
        dialogLayout.addView(availableQtyLabel);

        // Quantity input
        TextView quantityLabel = new TextView(this);
        quantityLabel.setText("Send Quantity (Pcs.):");
        quantityLabel.setTextSize(16);
        quantityLabel.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        quantityLabel.setPadding(0, dp(16), 0, dp(8));
        dialogLayout.addView(quantityLabel);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Enter quantity");
        quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        quantityInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        quantityInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        quantityInput.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_edittext_focused_selector));
        dialogLayout.addView(quantityInput);

        // Date input
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
        // Date picker
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

        // Dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Send to Factory Balance")
            .setView(dialogLayout)
            .setPositiveButton("Send", null) // Set to null for custom click handling
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        android.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button sendButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            sendButton.setOnClickListener(v -> {
                String grade = gradeSpinner.getSelectedItem().toString();
                String quantityStr = quantityInput.getText().toString().trim();
                String dateStr = dateInput.getText().toString().trim();
                if (TextUtils.isEmpty(quantityStr)) {
                    quantityInput.setError("Please enter quantity");
                    quantityInput.requestFocus();
                    return;
                }
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityStr);
                } catch (NumberFormatException e) {
                    quantityInput.setError("Please enter a valid number");
                    quantityInput.requestFocus();
                    return;
                }
                if (quantity <= 0) {
                    quantityInput.setError("Quantity must be greater than 0");
                    quantityInput.requestFocus();
                    return;
                }
                // If date is empty, use current date
                if (TextUtils.isEmpty(dateStr)) {
                    dateStr = currentDate;
                }
                if (currentLot != null) {
                    int availableEmbroidery = currentLot.getEmbroideryReceivePcs();
                    if (quantity > availableEmbroidery) {
                        quantityInput.setError("Cannot exceed available quantity (" + availableEmbroidery + ")");
                        quantityInput.requestFocus();
                        return;
                    }
                    // Show loading indicator and disable button
                    showLoading(true);
                    sendButton.setEnabled(false);
                    // Subtract from embroidery, add to selected grade
                    currentLot.setEmbroideryReceivePcs(availableEmbroidery - quantity);
                    if (grade.equals("A Grade")) {
                        int current = currentLot.getFactoryBalanceAGradePcs();
                        currentLot.setFactoryBalanceAGradePcs(current + quantity);
                    } else if (grade.equals("B Grade")) {
                        int current = currentLot.getFactoryBalanceBGradePcs();
                        currentLot.setFactoryBalanceBGradePcs(current + quantity);
                    }
                    firestoreService.updateLot(currentLot, new com.dazzling.erp.services.FirestoreService.LotCallback() {
                        @Override
                        public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                            currentLot = updatedLot;
                            updateFactoryBalanceTotal();
                            updateFactoryBalanceAGrade();
                            updateFactoryBalanceBGrade();
                            startFactoryBalanceTotalChartSimulation();
                            startFactoryBalanceAGradeChartSimulation();
                            startFactoryBalanceBGradeChartSimulation();
                            showLoading(false);
                            sendButton.setEnabled(true);
                            // Update available quantity label instantly
                            availableQtyLabel.setText("Available: " + currentLot.getEmbroideryReceivePcs() + " Pcs.");
                            // Clear quantity input for next send
                            quantityInput.setText("");
                            Toast.makeText(LotDetailActivity.this, "Successfully sent to Factory Balance!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                        @Override public void onError(String error) {
                            showLoading(false);
                            sendButton.setEnabled(true);
                            Toast.makeText(LotDetailActivity.this, "Failed to update: " + error, Toast.LENGTH_LONG).show();
                        }
                        @Override public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
                        @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
                        @Override public void onLotDeleted(String lotId) {}
                    });
                }
            });
        });
        dialog.show();
    }

    // --- NEW: A Grade Reject Chart Simulation ---
    private void startFactoryBalanceAGradeRejectChartSimulation() {
        if (factoryBalanceAGradeRejectChart == null) return;
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getFactoryBalanceRejectPcs();
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) {
                value = currentBalance;
            } else if (i == 5) {
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) {
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else {
                float progress = (float) i / 5f;
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        factoryBalanceAGradeRejectDataSet = new LineDataSet(entries, "A Grade Reject");
        factoryBalanceAGradeRejectDataSet.setColor(Color.parseColor("#F44336"));
        factoryBalanceAGradeRejectDataSet.setCircleColor(Color.parseColor("#F44336"));
        factoryBalanceAGradeRejectDataSet.setLineWidth(3f);
        factoryBalanceAGradeRejectDataSet.setCircleRadius(5f);
        factoryBalanceAGradeRejectDataSet.setDrawValues(true);
        factoryBalanceAGradeRejectDataSet.setValueTextSize(10f);
        factoryBalanceAGradeRejectDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceAGradeRejectDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        factoryBalanceAGradeRejectDataSet.setDrawCircles(true);
        factoryBalanceAGradeRejectDataSet.setDrawCircleHole(true);
        factoryBalanceAGradeRejectDataSet.setCircleHoleColor(Color.WHITE);
        LineData data = new LineData(factoryBalanceAGradeRejectDataSet);
        factoryBalanceAGradeRejectChart.setData(data);
        factoryBalanceAGradeRejectChart.getXAxis().setDrawLabels(true);
        factoryBalanceAGradeRejectChart.getXAxis().setGranularity(1f);
        factoryBalanceAGradeRejectChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        factoryBalanceAGradeRejectChart.getAxisRight().setEnabled(false);
        factoryBalanceAGradeRejectChart.getAxisLeft().setDrawGridLines(true);
        factoryBalanceAGradeRejectChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        factoryBalanceAGradeRejectChart.getAxisLeft().setGridLineWidth(0.5f);
        factoryBalanceAGradeRejectChart.getXAxis().setDrawGridLines(false);
        factoryBalanceAGradeRejectChart.getLegend().setEnabled(false);
        factoryBalanceAGradeRejectChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            factoryBalanceAGradeRejectChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            factoryBalanceAGradeRejectChart.getAxisLeft().setAxisMaximum(50f);
        }
        factoryBalanceAGradeRejectChart.invalidate();
    }

    // --- NEW: B Grade Reject Chart Simulation ---
    private void startFactoryBalanceBGradeRejectChartSimulation() {
        if (factoryBalanceBGradeRejectChart == null) return;
        int currentBalance = 0;
        if (currentLot != null) {
            currentBalance = currentLot.getFactoryBalanceRejectPcs();
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            float value;
            if (i == 6) {
                value = currentBalance;
            } else if (i == 5) {
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.1f));
            } else if (i == 4) {
                value = Math.max(0, currentBalance - (int)(currentBalance * 0.2f));
            } else {
                float progress = (float) i / 5f;
                value = Math.max(0, (int)(currentBalance * progress * 0.8f));
            }
            entries.add(new Entry(i, value));
        }
        factoryBalanceBGradeRejectDataSet = new LineDataSet(entries, "B Grade Reject");
        factoryBalanceBGradeRejectDataSet.setColor(Color.parseColor("#F44336"));
        factoryBalanceBGradeRejectDataSet.setCircleColor(Color.parseColor("#F44336"));
        factoryBalanceBGradeRejectDataSet.setLineWidth(3f);
        factoryBalanceBGradeRejectDataSet.setCircleRadius(5f);
        factoryBalanceBGradeRejectDataSet.setDrawValues(true);
        factoryBalanceBGradeRejectDataSet.setValueTextSize(10f);
        factoryBalanceBGradeRejectDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        factoryBalanceBGradeRejectDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        factoryBalanceBGradeRejectDataSet.setDrawCircles(true);
        factoryBalanceBGradeRejectDataSet.setDrawCircleHole(true);
        factoryBalanceBGradeRejectDataSet.setCircleHoleColor(Color.WHITE);
        LineData data = new LineData(factoryBalanceBGradeRejectDataSet);
        factoryBalanceBGradeRejectChart.setData(data);
        factoryBalanceBGradeRejectChart.getXAxis().setDrawLabels(true);
        factoryBalanceBGradeRejectChart.getXAxis().setGranularity(1f);
        factoryBalanceBGradeRejectChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int idx = (int) value;
                return (idx >= 0 && idx < days.length) ? days[idx] : "";
            }
        });
        factoryBalanceBGradeRejectChart.getAxisRight().setEnabled(false);
        factoryBalanceBGradeRejectChart.getAxisLeft().setDrawGridLines(true);
        factoryBalanceBGradeRejectChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        factoryBalanceBGradeRejectChart.getAxisLeft().setGridLineWidth(0.5f);
        factoryBalanceBGradeRejectChart.getXAxis().setDrawGridLines(false);
        factoryBalanceBGradeRejectChart.getLegend().setEnabled(false);
        factoryBalanceBGradeRejectChart.getAxisLeft().setAxisMinimum(0f);
        if (currentBalance > 0) {
            factoryBalanceBGradeRejectChart.getAxisLeft().setAxisMaximum(currentBalance * 1.2f);
        } else {
            factoryBalanceBGradeRejectChart.getAxisLeft().setAxisMaximum(50f);
        }
        factoryBalanceBGradeRejectChart.invalidate();
    }

    // --- NEW: Show Mark As A Reject Dialog ---
    private void showMarkAsARejectDialog() {
        // This dialog should match the embroidery reject dialog, but update A Grade reject pcs
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Mark A Grade As Rejected");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(4));
        final EditText quantityInput = new EditText(this);
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityInput.setHint("Quantity (Pcs)");
        layout.addView(quantityInput);
        final EditText dateInput = new EditText(this);
        dateInput.setInputType(InputType.TYPE_CLASS_DATETIME);
        dateInput.setHint("Date (yyyy-MM-dd)");
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        layout.addView(dateInput);
        builder.setView(layout);
        builder.setPositiveButton("Mark as Rejected", (dialog, which) -> {
            String qtyStr = quantityInput.getText().toString().trim();
            String dateStr = dateInput.getText().toString().trim();
            if (qtyStr.isEmpty() || Integer.parseInt(qtyStr) <= 0) {
                Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty = Integer.parseInt(qtyStr);
            processMarkAsAReject(qty, dateStr);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- NEW: Process Mark As A Reject ---
    private void processMarkAsAReject(int quantity, String date) {
        if (currentLot == null) return;
        int available = currentLot.getFactoryBalanceAGradePcs();
        if (quantity > available) {
            Toast.makeText(this, "Cannot reject more than available A Grade quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        int prevReject = currentLot.getFactoryBalanceAGradeRejectPcs();
        currentLot.setFactoryBalanceAGradePcs(available - quantity);
        currentLot.setFactoryBalanceAGradeRejectPcs(prevReject + quantity);
        // Add to history for audit
        List<com.dazzling.erp.models.Transfer> history = currentLot.getFactoryBalanceHistory();
        if (history == null) history = new java.util.ArrayList<>();
        history.add(new com.dazzling.erp.models.Transfer(quantity, new java.util.Date(), "A Grade → Reject on " + date));
        currentLot.setFactoryBalanceHistory(history);
        // Show loading indicator
        showLoading(true);
        firestoreService.updateLot(currentLot, new com.dazzling.erp.services.FirestoreService.LotCallback() {
            @Override
            public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                currentLot = updatedLot;
                updateFactoryBalanceAGrade();
                updateFactoryBalanceAGradeReject();
                startFactoryBalanceAGradeChartSimulation();
                startFactoryBalanceAGradeRejectChartSimulation();
                showLoading(false);
                Toast.makeText(LotDetailActivity.this, "Marked as rejected successfully!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(LotDetailActivity.this, "Failed to update reject balance: " + error, Toast.LENGTH_LONG).show();
                android.util.Log.e("LotDetailActivity", "Error updating reject balance", new Exception(error));
            }
            @Override public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
            @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
            @Override public void onLotDeleted(String lotId) {}
        });
    }

    // --- NEW: Update A Grade Reject Balance ---
    private void updateFactoryBalanceAGradeReject() {
        if (factoryBalanceAGradeRejectText != null && currentLot != null) {
            int rejectPcs = currentLot.getFactoryBalanceAGradeRejectPcs();
            factoryBalanceAGradeRejectText.setText("A Grade Reject Balance: " + rejectPcs + " Pcs.");
        }
    }

    // --- NEW: Update B Grade Reject Balance ---
    private void updateFactoryBalanceBGradeReject() {
        if (factoryBalanceBGradeRejectText != null && currentLot != null) {
            int rejectPcs = currentLot.getFactoryBalanceBGradeRejectPcs();
            factoryBalanceBGradeRejectText.setText("B Grade Reject Balance: " + rejectPcs + " Pcs.");
        }
    }

    // --- B GRADE: Show Mark As Reject Dialog ---
    private void showMarkAsBRejectDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Mark B Grade As Rejected");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(4));
        final EditText quantityInput = new EditText(this);
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityInput.setHint("Quantity (Pcs)");
        layout.addView(quantityInput);
        final EditText dateInput = new EditText(this);
        dateInput.setInputType(InputType.TYPE_CLASS_DATETIME);
        dateInput.setHint("Date (yyyy-MM-dd)");
        dateInput.setText(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        layout.addView(dateInput);
        builder.setView(layout);
        builder.setPositiveButton("Mark as Rejected", (dialog, which) -> {
            String qtyStr = quantityInput.getText().toString().trim();
            String dateStr = dateInput.getText().toString().trim();
            if (qtyStr.isEmpty() || Integer.parseInt(qtyStr) <= 0) {
                Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty = Integer.parseInt(qtyStr);
            processMarkAsBReject(qty, dateStr);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- B GRADE: Process Mark As Reject ---
    private void processMarkAsBReject(int quantity, String date) {
        if (currentLot == null) return;
        int available = currentLot.getFactoryBalanceBGradePcs();
        if (quantity > available) {
            Toast.makeText(this, "Cannot reject more than available B Grade quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        int prevReject = currentLot.getFactoryBalanceBGradeRejectPcs();
        currentLot.setFactoryBalanceBGradePcs(available - quantity);
        currentLot.setFactoryBalanceBGradeRejectPcs(prevReject + quantity);
        // Add to history for audit
        java.util.List<com.dazzling.erp.models.Transfer> history = currentLot.getFactoryBalanceHistory();
        if (history == null) history = new java.util.ArrayList<>();
        history.add(new com.dazzling.erp.models.Transfer(quantity, new java.util.Date(), "B Grade → Reject on " + date));
        currentLot.setFactoryBalanceHistory(history);
        // Show loading indicator
        showLoading(true);
        firestoreService.updateLot(currentLot, new com.dazzling.erp.services.FirestoreService.LotCallback() {
            @Override
            public void onLotUpdated(com.dazzling.erp.models.Lot updatedLot) {
                currentLot = updatedLot;
                updateFactoryBalanceBGrade();
                updateFactoryBalanceBGradeReject();
                startFactoryBalanceBGradeChartSimulation();
                startFactoryBalanceBGradeRejectChartSimulation();
                showLoading(false);
                Toast.makeText(LotDetailActivity.this, "Marked as rejected successfully!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(LotDetailActivity.this, "Failed to update reject balance: " + error, Toast.LENGTH_LONG).show();
                android.util.Log.e("LotDetailActivity", "Error updating B Grade reject balance", new Exception(error));
            }
            @Override public void onLotsLoaded(java.util.List<com.dazzling.erp.models.Lot> lots) {}
            @Override public void onLotAdded(com.dazzling.erp.models.Lot lot) {}
            @Override public void onLotDeleted(String lotId) {}
        });
    }

    // Add this method to the class:
    private void showHistoryDialog() {
        if (currentLot == null || currentLot.getFactoryBalanceHistory() == null || currentLot.getFactoryBalanceHistory().isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("History")
                .setMessage("No history available.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
            return;
        }
        StringBuilder historyText = new StringBuilder();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        for (com.dazzling.erp.models.Transfer t : currentLot.getFactoryBalanceHistory()) {
            historyText.append("Date: ").append(sdf.format(t.getDate()))
                .append("\nQty: ").append(t.getQuantity())
                .append("\nNote: ").append(t.getNote() == null ? "" : t.getNote())
                .append("\n\n");
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Factory Balance History")
            .setMessage(historyText.toString())
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .show();
    }

    // Show embroidery reject history dialog
    private void showEmbroideryRejectHistoryDialog() {
        if (currentLot == null || currentLot.getEmbroideryRejectHistory() == null || currentLot.getEmbroideryRejectHistory().isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Embroidery Reject History")
                .setMessage("No history available.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
            return;
        }
        StringBuilder historyText = new StringBuilder();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        for (com.dazzling.erp.models.Transfer t : currentLot.getEmbroideryRejectHistory()) {
            historyText.append("Date: ").append(sdf.format(t.getDate()))
                .append("\nQty: ").append(t.getQuantity())
                .append("\nNote: ").append(t.getNote() == null ? "" : t.getNote())
                .append("\n\n");
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Embroidery Reject History")
            .setMessage(historyText.toString())
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .show();
    }
}