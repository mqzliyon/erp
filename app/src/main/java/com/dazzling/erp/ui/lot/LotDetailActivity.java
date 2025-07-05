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
import java.util.Random;

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
    public boolean onSupportNavigateUp() {
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
            case 2: // Office Shipment
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
            // TODO: Implement transfer logic
        });
        vbox.addView(transferBtn);

        card.addView(vbox);
        // Start chart data simulation
        startCuttingChartSimulation();
        return card;
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

    // Simulate real-time chart data updates
    private void startCuttingChartSimulation() {
        if (cuttingChart == null) return;
        List<Entry> entries = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, 900 + random.nextInt(400)));
        }
        cuttingDataSet = new LineDataSet(entries, "Cutting Performance");
        cuttingDataSet.setColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        cuttingDataSet.setCircleColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        cuttingDataSet.setLineWidth(2f);
        cuttingDataSet.setCircleRadius(4f);
        cuttingDataSet.setDrawValues(false);
        cuttingDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineData data = new LineData(cuttingDataSet);
        cuttingChart.setData(data);
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
        cuttingChart.getAxisRight().setEnabled(false);
        cuttingChart.getAxisLeft().setDrawGridLines(false);
        cuttingChart.getXAxis().setDrawGridLines(false);
        cuttingChart.getLegend().setEnabled(false);
        cuttingChart.invalidate();
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.removeCallbacks(chartUpdater);
        }
        chartHandler = new Handler(Looper.getMainLooper());
        chartUpdater = () -> {
            for (int i = 0; i < cuttingDataSet.getEntryCount(); i++) {
                float newY = 900 + new Random().nextInt(400);
                cuttingDataSet.getEntryForIndex(i).setY(newY);
            }
            cuttingChart.getData().notifyDataChanged();
            cuttingChart.notifyDataSetChanged();
            cuttingChart.invalidate();
            chartHandler.postDelayed(chartUpdater, 3000);
        };
        chartHandler.postDelayed(chartUpdater, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.removeCallbacks(chartUpdater);
        }
    }

    // Utility: dp to px
    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
} 