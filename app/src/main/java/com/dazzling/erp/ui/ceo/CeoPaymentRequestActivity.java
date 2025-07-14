package com.dazzling.erp.ui.ceo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dazzling.erp.R;
import com.dazzling.erp.adapters.PaymentRequestAdapter;
import com.dazzling.erp.models.PaymentRequest;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import android.widget.Button;
import android.graphics.drawable.GradientDrawable;

public class CeoPaymentRequestActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PaymentRequestAdapter adapter;
    private List<PaymentRequest> paymentRequests = new ArrayList<>();
    private FirestoreService firestoreService;
    private View loadingOverlay;
    private TextView loadingText;
    private View emptyState;
    private TextView tabPending, tabApproved, tabRejected;
    private String currentTab = "Pending";
    private List<PaymentRequest> allPaymentRequests = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ceo_payment_request);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Payment Requests");
        }

        firestoreService = new FirestoreService();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentRequestAdapter(this, paymentRequests);
        recyclerView.setAdapter(adapter);

        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingText = findViewById(R.id.loading_text);
        emptyState = findViewById(R.id.empty_state);

        tabPending = findViewById(R.id.tab_pending);
        tabApproved = findViewById(R.id.tab_approved);
        tabRejected = findViewById(R.id.tab_rejected);

        adapter.setOnItemClickListener(this::showPaymentRequestDetails);
        adapter.setOnMenuClickListener(this::showApproveMenu);

        tabPending.setOnClickListener(v -> selectTab("Pending"));
        tabApproved.setOnClickListener(v -> selectTab("Approved"));
        tabRejected.setOnClickListener(v -> selectTab("Rejected"));

        selectTab("Pending"); // Default tab
        loadPaymentRequests();
    }

    private void selectTab(String tab) {
        currentTab = tab;
        updateTabStyles();
        filterAndShowRequests();
    }

    private void updateTabStyles() {
        // Reset all
        tabPending.setBackgroundResource(android.R.color.transparent);
        tabApproved.setBackgroundResource(android.R.color.transparent);
        tabRejected.setBackgroundResource(android.R.color.transparent);
        tabPending.setTextColor(0xFF888888);
        tabApproved.setTextColor(0xFF888888);
        tabRejected.setTextColor(0xFF888888);
        // Highlight selected
        if ("Pending".equals(currentTab)) {
            tabPending.setBackgroundResource(R.drawable.bg_chip_date); // yellow chip
            tabPending.setTextColor(getResources().getColor(R.color.white));
        } else if ("Approved".equals(currentTab)) {
            tabApproved.setBackgroundColor(0xFF4CAF50); // Green
            tabApproved.setTextColor(getResources().getColor(R.color.white));
        } else if ("Rejected".equals(currentTab)) {
            tabRejected.setBackgroundResource(R.drawable.bg_chip_reject); // red chip
            tabRejected.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void filterAndShowRequests() {
        paymentRequests.clear();
        for (PaymentRequest req : allPaymentRequests) {
            if (currentTab.equalsIgnoreCase(req.getStatus())) {
                paymentRequests.add(req);
            }
        }
        adapter.updateData(paymentRequests);
        if (paymentRequests.isEmpty()) showEmptyState();
        else hideEmptyState();
    }

    private void loadPaymentRequests() {
        showLoading("Loading payment requests...");
        firestoreService.getPaymentRequestsByOffice("Manager", new FirestoreService.PaymentRequestCallback() {
            @Override
            public void onPaymentRequestsLoaded(List<PaymentRequest> requests) {
                runOnUiThread(() -> {
                    allPaymentRequests.clear();
                    allPaymentRequests.addAll(requests);
                    filterAndShowRequests();
                    hideLoading();
                });
            }
            @Override public void onPaymentRequestAdded(PaymentRequest paymentRequest) {}
            @Override public void onPaymentRequestUpdated(PaymentRequest paymentRequest) {}
            @Override public void onPaymentRequestDeleted(String paymentRequestId) {}
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    showError("Failed to load data. Please try again.");
                });
            }
        });
    }

    private void showPaymentRequestDetails(PaymentRequest paymentRequest) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment_request_details, null);
        TextView textPaymentMethod = dialogView.findViewById(R.id.text_payment_method);
        TextView textInvoiceNumber = dialogView.findViewById(R.id.text_invoice_number);
        TextView textAmount = dialogView.findViewById(R.id.text_amount);
        TextView textDate = dialogView.findViewById(R.id.text_date);
        TextView textTime = dialogView.findViewById(R.id.text_time);
        TextView textStatus = dialogView.findViewById(R.id.text_status);
        Button btnApprove = dialogView.findViewById(R.id.btn_approve);
        Button btnReject = dialogView.findViewById(R.id.btn_reject);
        Button btnClose = dialogView.findViewById(R.id.btn_close);
        textPaymentMethod.setText(paymentRequest.getPaymentMethod());
        textInvoiceNumber.setText(paymentRequest.getInvoiceNumber());
        textAmount.setText("৳ " + String.format("%,.2f", paymentRequest.getAmount()));
        textDate.setText(paymentRequest.getDate());
        textTime.setText(paymentRequest.getTime());
        textStatus.setText(paymentRequest.getStatus());
        // Set status badge color in dialog to match sub menu
        if ("Pending".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundResource(R.drawable.bg_chip_date); // yellow chip
            textStatus.setTextColor(getResources().getColor(R.color.white));
        } else if ("Approved".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundColor(0xFF4CAF50); // Green
            textStatus.setTextColor(getResources().getColor(R.color.white));
        } else if ("Rejected".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundResource(R.drawable.bg_chip_reject); // red chip
            textStatus.setTextColor(getResources().getColor(R.color.white));
        } else {
            textStatus.setBackgroundResource(R.drawable.bg_chip_date);
            textStatus.setTextColor(getResources().getColor(R.color.white));
        }
        android.app.AlertDialog dialog = builder.setView(dialogView).create();
        btnApprove.setVisibility(View.VISIBLE);
        btnApprove.setOnClickListener(v -> {
            approvePaymentRequest(paymentRequest, dialog);
        });
        btnReject.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Reject Payment Request")
                .setMessage("Are you sure you want to reject this payment request?")
                .setPositiveButton("Yes", (d, i) -> {
                    rejectPaymentRequest(paymentRequest, dialog);
                })
                .setNegativeButton("No", null)
                .show();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showApproveMenu(View view, PaymentRequest paymentRequest) {
        // Only show options if status is pending
        if ("Pending".equalsIgnoreCase(paymentRequest.getStatus())) {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
            popup.getMenu().add("Approve");
            popup.getMenu().add("Reject");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Approve")) {
                    approvePaymentRequest(paymentRequest);
                    return true;
                } else if (item.getTitle().equals("Reject")) {
                    rejectPaymentRequest(paymentRequest);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }

    // Overloaded for dialog auto-close
    private void approvePaymentRequest(PaymentRequest paymentRequest, android.app.AlertDialog dialog) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Approve Payment Request")
            .setMessage("Are you sure you want to approve this payment request?")
            .setPositiveButton("Yes", (dialogInterface, i) -> {
                showLoading("Approving payment request...");
                paymentRequest.setStatus("Approved");
                firestoreService.updatePaymentRequest(paymentRequest, new FirestoreService.PaymentRequestCallback() {
                    @Override
                    public void onPaymentRequestsLoaded(List<PaymentRequest> paymentRequests) {}
                    @Override
                    public void onPaymentRequestAdded(PaymentRequest paymentRequest) {}
                    @Override
                    public void onPaymentRequestUpdated(PaymentRequest updatedRequest) {
                        runOnUiThread(() -> {
                            hideLoading();
                            showSuccess("Payment request approved.");
                            for (int idx = 0; idx < allPaymentRequests.size(); idx++) {
                                if (allPaymentRequests.get(idx).getId().equals(updatedRequest.getId())) {
                                    allPaymentRequests.set(idx, updatedRequest);
                                    break;
                                }
                            }
                            filterAndShowRequests();
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                        });
                    }
                    @Override
                    public void onPaymentRequestDeleted(String paymentRequestId) {}
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            hideLoading();
                            showError("Failed to approve payment request. Please try again.");
                        });
                    }
                });
            })
            .setNegativeButton("No", null)
            .show();
    }

    // Restore original method for backward compatibility
    private void approvePaymentRequest(PaymentRequest paymentRequest) {
        approvePaymentRequest(paymentRequest, null);
    }
    private void rejectPaymentRequest(PaymentRequest paymentRequest) {
        rejectPaymentRequest(paymentRequest, null);
    }

    // Overloaded for dialog auto-close
    private void rejectPaymentRequest(PaymentRequest paymentRequest, android.app.AlertDialog dialog) {
        showLoading("Rejecting payment request...");
        paymentRequest.setStatus("Rejected");
        firestoreService.updatePaymentRequest(paymentRequest, new FirestoreService.PaymentRequestCallback() {
            @Override
            public void onPaymentRequestsLoaded(List<PaymentRequest> paymentRequests) {}
            @Override
            public void onPaymentRequestAdded(PaymentRequest paymentRequest) {}
            @Override
            public void onPaymentRequestUpdated(PaymentRequest updatedRequest) {
                runOnUiThread(() -> {
                    hideLoading();
                    showSuccess("Payment request rejected.");
                    for (int idx = 0; idx < allPaymentRequests.size(); idx++) {
                        if (allPaymentRequests.get(idx).getId().equals(updatedRequest.getId())) {
                            allPaymentRequests.set(idx, updatedRequest);
                            break;
                        }
                    }
                    filterAndShowRequests();
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                });
            }
            @Override
            public void onPaymentRequestDeleted(String paymentRequestId) {}
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    showError("Failed to reject payment request. Please try again.");
                });
            }
        });
    }

    private void showLoading(String message) {
        if (loadingOverlay != null && loadingText != null) {
            loadingText.setText(message);
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }
    private void hideLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
    }
    private void showEmptyState() {
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
    }
    private void hideEmptyState() {
        if (emptyState != null) emptyState.setVisibility(View.GONE);
    }
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
} 