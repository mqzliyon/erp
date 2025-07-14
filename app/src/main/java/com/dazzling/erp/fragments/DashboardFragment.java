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
import com.dazzling.erp.models.PaymentRequest;
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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.utils.ColorTemplate;

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
    private List<PaymentRequest> paymentRequests = new ArrayList<>();
    
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
        loadData();
        loadPaymentRequestAnalytics();
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
        // Payment Request Analytics card click listener
        binding.cardPaymentAnalytics.setOnClickListener(v -> {
            if (getActivity() instanceof com.dazzling.erp.MainActivity) {
                com.dazzling.erp.MainActivity mainActivity = (com.dazzling.erp.MainActivity) getActivity();
                com.dazzling.erp.models.User user = mainActivity.getCurrentUser();
                if (user != null && "CEO".equals(user.getRole())) {
                    // CEO: open CeoPaymentRequestActivity
                    android.content.Intent intent = new android.content.Intent(getContext(), com.dazzling.erp.ui.ceo.CeoPaymentRequestActivity.class);
                    startActivity(intent);
                } else if (user != null && "Manager".equals(user.getRole())) {
                    // Manager: navigate to ManagerPaymentFragment
                    mainActivity.navigateToFragment("manager_payment");
                }
            }
        });
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
    }
    
    /**
     * Filter list by date range
     */
    private <T> List<T> filterByDateRange(List<T> items) {
        // This is a simplified filter - in a real app, you'd filter by actual date fields
        return items;
    }
    
    /**
     * Load payment request analytics
     */
    private void loadPaymentRequestAnalytics() {
        // For CEO, fetch all payment requests
        firestoreService.getAllPaymentRequests(new FirestoreService.PaymentRequestCallback() {
            @Override
            public void onPaymentRequestsLoaded(List<PaymentRequest> requests) {
                paymentRequests = requests;
                updatePaymentAnalytics();
            }
            @Override public void onPaymentRequestAdded(PaymentRequest paymentRequest) {}
            @Override public void onPaymentRequestUpdated(PaymentRequest paymentRequest) {}
            @Override public void onPaymentRequestDeleted(String paymentRequestId) {}
            @Override public void onError(String error) {
                if (binding != null) binding.tvPaymentAnalytics.setText("Error loading analytics");
            }
        });
    }

    private void updatePaymentAnalytics() {
        if (binding == null) return;
        int total = paymentRequests.size();
        int pending = 0, approved = 0, rejected = 0;
        for (PaymentRequest req : paymentRequests) {
            if ("Pending".equalsIgnoreCase(req.getStatus())) pending++;
            else if ("Approved".equalsIgnoreCase(req.getStatus())) approved++;
            else if ("Rejected".equalsIgnoreCase(req.getStatus())) rejected++;
        }
        String analytics = "Total: " + total + "\nPending: " + pending + "\nApproved: " + approved + "\nRejected: " + rejected;
        binding.tvPaymentAnalytics.setText(analytics);

        // Update PieChart
        PieChart pieChart = binding.chartPaymentRequests;
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (pending > 0) entries.add(new PieEntry(pending, "Pending"));
        if (approved > 0) entries.add(new PieEntry(approved, "Approved"));
        if (rejected > 0) entries.add(new PieEntry(rejected, "Rejected"));
        PieDataSet dataSet = new PieDataSet(entries, "Payment Requests");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(14f);
        pieChart.setCenterText("Status");
        pieChart.setCenterTextSize(16f);
        pieChart.getLegend().setEnabled(true);
        pieChart.invalidate();
    }
    
    /**
     * Show fabrics details
     */
    private void showFabricsDetails() {
        if (getActivity() instanceof com.dazzling.erp.MainActivity) {
            ((com.dazzling.erp.MainActivity) getActivity()).navigateToFragment("fabrics");
        }
    }
    
    /**
     * Show cutting details
     */
    private void showCuttingDetails() {
        if (getActivity() instanceof com.dazzling.erp.MainActivity) {
            ((com.dazzling.erp.MainActivity) getActivity()).navigateToFragment("cutting");
        }
    }
    
    /**
     * Show lots details
     */
    private void showLotsDetails() {
        if (getActivity() instanceof com.dazzling.erp.MainActivity) {
            ((com.dazzling.erp.MainActivity) getActivity()).navigateToFragment("lots");
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