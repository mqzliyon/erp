package com.dazzling.erp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dazzling.erp.MainActivity;
import com.dazzling.erp.R;
import com.dazzling.erp.databinding.FragmentDashboardBinding;
import com.dazzling.erp.models.Cutting;
import com.dazzling.erp.models.Fabric;
import com.dazzling.erp.models.Lot;
import com.dazzling.erp.services.FirestoreService;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard Fragment with date-filterable charts and summary cards
 */
public class DashboardFragment extends Fragment implements 
        FirestoreService.FabricCallback,
        FirestoreService.CuttingCallback,
        FirestoreService.LotCallback {
    
    private FragmentDashboardBinding binding;
    private FirestoreService firestoreService;
    
    private List<Fabric> fabrics = new ArrayList<>();
    private List<Cutting> cuttings = new ArrayList<>();
    private List<Lot> lots = new ArrayList<>();
    
    private Date startDate;
    private Date endDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firestore Service
        firestoreService = new FirestoreService();
        
        // Set default date range (last 30 days)
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        startDate = calendar.getTime();
        
        setupViews();
        setupCharts();
        loadData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
    
    /**
     * Setup UI components
     */
    private void setupViews() {
        // Date range picker
        binding.btnDateRange.setOnClickListener(v -> showDateRangePicker());
        
        // Update date range display
        updateDateRangeDisplay();
        
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        
        // Summary card click listeners
        binding.cardFabrics.setOnClickListener(v -> showFabricsDetails());
        binding.cardCutting.setOnClickListener(v -> showCuttingDetails());
        binding.cardLots.setOnClickListener(v -> showLotsDetails());
    }
    
    /**
     * Setup charts
     */
    private void setupCharts() {
        // Fabric Chart
        binding.chartFabrics.getDescription().setEnabled(false);
        binding.chartFabrics.setDrawGridBackground(false);
        binding.chartFabrics.setDrawBarShadow(false);
        binding.chartFabrics.setHighlightFullBarEnabled(false);
        
        XAxis xAxis = binding.chartFabrics.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        
        binding.chartFabrics.getAxisLeft().setDrawGridLines(false);
        binding.chartFabrics.getAxisRight().setEnabled(false);
        binding.chartFabrics.getLegend().setEnabled(false);
        
        // Lot Status Chart
        binding.chartLotStatus.getDescription().setEnabled(false);
        binding.chartLotStatus.setHoleRadius(35f);
        binding.chartLotStatus.setTransparentCircleRadius(40f);
        binding.chartLotStatus.setDrawHoleEnabled(true);
        binding.chartLotStatus.setRotationAngle(0);
        binding.chartLotStatus.setRotationEnabled(true);
        binding.chartLotStatus.setHighlightPerTapEnabled(true);
        binding.chartLotStatus.getLegend().setEnabled(true);
    }
    
    /**
     * Load data from Firestore
     */
    private void loadData() {
        // Check if user is authenticated before loading data
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            Log.w("DashboardFragment", "loadData: Activity is null or not MainActivity");
            return;
        }
        
        MainActivity mainActivity = (MainActivity) getActivity();
        if (!mainActivity.isUserAuthenticated()) {
            Log.w("DashboardFragment", "loadData: User not authenticated, skipping data load");
            showLoading(false);
            return;
        }
        
        showLoading(true);
        
        // Load fabrics
        firestoreService.getFabrics(this);
        
        // Load cuttings
        firestoreService.getCuttings(this);
        
        // Load lots
        firestoreService.getLots(this);
    }
    
    /**
     * Show date range picker
     */
    private void showDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        
        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = new Date(selection.first);
            endDate = new Date(selection.second);
            updateDateRangeDisplay();
            loadData();
        });
        
        picker.show(getParentFragmentManager(), "date_range_picker");
    }
    
    /**
     * Update date range display
     */
    private void updateDateRangeDisplay() {
        if (binding == null) return;
        String dateRange = dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
        binding.tvDateRange.setText(dateRange);
    }
    
    /**
     * Show loading state
     */
    private void showLoading(boolean show) {
        if (binding != null) {
            binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(show);
        }
    }
    
    /**
     * Update summary cards
     */
    private void updateSummaryCards() {
        if (binding == null) return;
        
        // Filter data by date range
        List<Fabric> filteredFabrics = filterByDateRange(fabrics);
        List<Cutting> filteredCuttings = filterByDateRange(cuttings);
        List<Lot> filteredLots = filterByDateRange(lots);
        
        // Calculate totals
        double totalFabricKg = filteredFabrics.stream().mapToDouble(Fabric::getQuantityKg).sum();
        double totalCuttingPcs = filteredCuttings.stream().mapToDouble(c -> c.getQuantityPcs() > 0 ? c.getQuantityPcs() : 0).sum();
        double totalCuttingKg = filteredCuttings.stream().mapToDouble(c -> c.getQuantityPcs() > 0 ? 0 : c.getQuantityKg()).sum();
        int totalLots = filteredLots.size();
        
        // Update UI
        binding.tvFabricTotal.setText(String.format(Locale.getDefault(), "%.1f KG", totalFabricKg));
        if (totalCuttingPcs > 0) {
            binding.tvCuttingTotal.setText(String.format(Locale.getDefault(), "%.0f PCS", totalCuttingPcs));
        } else {
            binding.tvCuttingTotal.setText(String.format(Locale.getDefault(), "%.1f KG", totalCuttingKg));
        }
        binding.tvLotsTotal.setText(String.valueOf(totalLots));
        
        // Update charts
        updateFabricChart(filteredFabrics);
        updateLotStatusChart(filteredLots);
    }
    
    /**
     * Filter list by date range
     */
    private <T> List<T> filterByDateRange(List<T> items) {
        // This is a simplified filter - in a real app, you'd filter by actual date fields
        return items;
    }
    
    /**
     * Update fabric chart
     */
    private void updateFabricChart(List<Fabric> fabrics) {
        if (binding == null) return;

        // Group fabrics by type and sum their quantities
        Map<String, Float> typeTotals = new LinkedHashMap<>();
        for (Fabric fabric : fabrics) {
            String type = fabric.getFabricType() != null ? fabric.getFabricType() : "Unknown";
            float qty = (float) fabric.getQuantityKg();
            typeTotals.put(type, typeTotals.getOrDefault(type, 0f) + qty);
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Float> entry : typeTotals.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Fabric Types");
        // Use a default color set, or you can customize as needed
        dataSet.setColors(Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), Color.rgb(217, 184, 162));
        BarData barData = new BarData(dataSet);
        binding.chartFabrics.setData(barData);

        XAxis xAxis = binding.chartFabrics.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        binding.chartFabrics.invalidate();
    }
    
    /**
     * Update lot status chart
     */
    private void updateLotStatusChart(List<Lot> lots) {
        if (binding == null) return;
        
        List<PieEntry> entries = new ArrayList<>();
        
        long activeLots = lots.stream().filter(Lot::isActive).count();
        long completedLots = lots.stream().filter(Lot::isCompleted).count();
        
        entries.add(new PieEntry(activeLots, "Active"));
        entries.add(new PieEntry(completedLots, "Completed"));
        
        PieDataSet dataSet = new PieDataSet(entries, "Lot Status");
        dataSet.setColors(Color.rgb(64, 89, 128), Color.rgb(149, 165, 124));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(binding.chartLotStatus));
        binding.chartLotStatus.setData(pieData);
        
        binding.chartLotStatus.invalidate();
    }
    
    /**
     * Show fabrics details
     */
    private void showFabricsDetails() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), "Fabrics: " + fabrics.size() + " items", Snackbar.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show cutting details
     */
    private void showCuttingDetails() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), "Cutting: " + cuttings.size() + " operations", Snackbar.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show lots details
     */
    private void showLotsDetails() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), "Lots: " + lots.size() + " lots", Snackbar.LENGTH_SHORT).show();
        }
    }
    
    // ==================== FIREBASE CALLBACKS ====================
    
    @Override
    public void onFabricsLoaded(List<Fabric> fabrics) {
        this.fabrics = fabrics;
        updateSummaryCards();
    }
    
    @Override
    public void onFabricAdded(Fabric fabric) {
        // Handle fabric added
    }
    
    @Override
    public void onFabricUpdated(Fabric fabric) {
        // Handle fabric updated
    }
    
    @Override
    public void onFabricDeleted(String fabricId) {
        // Handle fabric deleted
    }
    
    @Override
    public void onCuttingsLoaded(List<Cutting> cuttings) {
        this.cuttings = cuttings;
        updateSummaryCards();
    }
    
    @Override
    public void onCuttingAdded(Cutting cutting) {
        // Handle cutting added
    }
    
    @Override
    public void onCuttingUpdated(Cutting cutting) {
        // Handle cutting updated
    }
    
    @Override
    public void onCuttingDeleted(String cuttingId) {
        // Handle cutting deleted
    }
    
    @Override
    public void onLotsLoaded(List<Lot> lots) {
        this.lots = lots;
        updateSummaryCards();
        showLoading(false);
    }
    
    @Override
    public void onLotAdded(Lot lot) {
        // Handle lot added
    }
    
    @Override
    public void onLotUpdated(Lot lot) {
        // Handle lot updated
    }
    
    @Override
    public void onLotDeleted(String lotId) {
        // Handle lot deleted
    }
    
    @Override
    public void onError(String error) {
        showLoading(false);
        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void reloadDashboardData() {
        loadData();
    }
} 