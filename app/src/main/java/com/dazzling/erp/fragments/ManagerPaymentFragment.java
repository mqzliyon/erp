package com.dazzling.erp.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.adapters.PaymentRequestAdapter;
import com.dazzling.erp.models.PaymentRequest;
import com.dazzling.erp.services.FirebaseAuthService;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManagerPaymentFragment extends Fragment {
    
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    
    // List variables
    private RecyclerView recyclerView;
    private PaymentRequestAdapter adapter;
    private List<PaymentRequest> paymentRequests = new ArrayList<>();
    
    // UI State variables
    private View loadingOverlay;
    private TextView loadingText;
    private View emptyState;
    private boolean isInitialLoad = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_payment, container, false);
        
        // Initialize services
        authService = new FirebaseAuthService();
        firestoreService = new FirestoreService();
        
        // Initialize date utilities
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        // Initialize views and setup data
        setupViews(view);
        
        return view;
    }
    
    private void setupViews(View view) {
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Setup adapter
        adapter = new PaymentRequestAdapter(getContext(), paymentRequests);
        recyclerView.setAdapter(adapter);
        
        // Setup click listeners
        adapter.setOnItemClickListener(this::showPaymentRequestDetails);
        adapter.setOnMenuClickListener(this::showPopupMenu);
        
        // Initialize UI state views
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        loadingText = view.findViewById(R.id.loading_text);
        emptyState = view.findViewById(R.id.empty_state);
        
        // Setup FloatingActionButton
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle plus icon click
                showPaymentRequestDialog();
            }
        });
        
        // Load initial data from Firebase
        loadPaymentRequestsFromFirebase();
    }
    
    private void loadPaymentRequestsFromFirebase() {
        // Show loading state
        showLoading("Loading payment requests...");
        
        // Get current user's office - for manager, we'll use a general manager office
        // This should be determined based on the actual user's role and office assignment
        String office = "Manager"; // This should be dynamically determined based on user data
        
        firestoreService.getPaymentRequestsByOffice(office, new FirestoreService.PaymentRequestCallback() {
            @Override
            public void onPaymentRequestsLoaded(List<PaymentRequest> requests) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        paymentRequests.clear();
                        paymentRequests.addAll(requests);
                        updatePaymentRequestList();
                        
                        // Log loaded data
                        android.util.Log.d("PaymentRequest", "Loaded " + requests.size() + " payment requests from Firebase");
                        
                        // Only hide loading on initial load, not on real-time updates
                        if (isInitialLoad) {
                            hideLoading();
                            isInitialLoad = false;
                        }
                    });
                }
            }

            @Override
            public void onPaymentRequestAdded(PaymentRequest paymentRequest) {
                // Real-time listener will handle this
            }

            @Override
            public void onPaymentRequestUpdated(PaymentRequest paymentRequest) {
                // Real-time listener will handle this
            }

            @Override
            public void onPaymentRequestDeleted(String paymentRequestId) {
                // Real-time listener will handle this
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
                        paymentRequests.clear();
                        updatePaymentRequestList();
                    });
                }
            }
        });
    }
    
    private void updatePaymentRequestList() {
        adapter.updateData(paymentRequests);
        
        // Show/hide empty state
        if (paymentRequests.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }
    
    private void showPaymentRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment_request, null);
        
        // Initialize dialog views
        Spinner spinnerPaymentMethod = dialogView.findViewById(R.id.spinner_payment_method);
        EditText etInvoiceNumber = dialogView.findViewById(R.id.et_invoice_number);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etDate = dialogView.findViewById(R.id.et_date);
        EditText etTime = dialogView.findViewById(R.id.et_time);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnRequest = dialogView.findViewById(R.id.btn_request);
        
        // Setup payment method spinner
        String[] paymentMethods = {"Bkash", "Bank Transfer", "Nagad"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, paymentMethods);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(spinnerAdapter);
        
        // Set current date and time as default
        etDate.setText(dateFormat.format(new Date()));
        etTime.setText(timeFormat.format(new Date()));
        
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
        
        // Setup time picker
        etTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    etTime.setText(timeFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12-hour format
            );
            timePickerDialog.show();
        });
        
        // Create dialog
        AlertDialog dialog = builder.setView(dialogView).create();
        
        // Setup button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnRequest.setOnClickListener(v -> {
            // Validate and save data
            if (validateInputs(etInvoiceNumber, etAmount)) {
                savePaymentRequest(
                    spinnerPaymentMethod.getSelectedItem().toString(),
                    etInvoiceNumber.getText().toString().trim(),
                    Double.parseDouble(etAmount.getText().toString().trim()),
                    etDate.getText().toString(),
                    etTime.getText().toString()
                );
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private boolean validateInputs(EditText etInvoiceNumber, EditText etAmount) {
        // Validate invoice number as required
        String invoiceNumber = etInvoiceNumber.getText().toString().trim();
        if (invoiceNumber.isEmpty()) {
            etInvoiceNumber.setError("Invoice number is required");
            etInvoiceNumber.requestFocus();
            return false;
        }
        
        // Validate amount as required
        String amountText = etAmount.getText().toString().trim();
        if (amountText.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                etAmount.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Please enter a valid amount");
            etAmount.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void savePaymentRequest(String paymentMethod, String invoiceNumber, 
                                   double amount, String date, String time) {
        
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            showError("User not authenticated. Please login again.");
            return;
        }
        
        // Show saving indicator
        showLoading("Saving payment request...");
        
        // Create payment request with proper office assignment
        PaymentRequest paymentRequest = new PaymentRequest(
            paymentMethod, invoiceNumber, amount, date, time, userId, "Manager"
        );
        
        // Log the payment request details for debugging
        android.util.Log.d("PaymentRequest", "Creating payment request: " + 
            "Method=" + paymentMethod + 
            ", Invoice=" + invoiceNumber + 
            ", Amount=" + amount + 
            ", Date=" + date + 
            ", Time=" + time + 
            ", User=" + userId + 
            ", Office=Manager");
        
        // Save to Firebase
        firestoreService.addPaymentRequest(paymentRequest, new FirestoreService.PaymentRequestCallback() {
            @Override
            public void onPaymentRequestsLoaded(List<PaymentRequest> paymentRequests) {
                // Not used for add operation
            }

            @Override
            public void onPaymentRequestAdded(PaymentRequest paymentRequest) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        showSuccess("Payment request saved successfully!");
                        // Log successful save
                        android.util.Log.d("PaymentRequest", "Payment request saved to Firebase with ID: " + paymentRequest.getId());
                        // Real-time listener will automatically update the list
                    });
                }
            }

            @Override
            public void onPaymentRequestUpdated(PaymentRequest paymentRequest) {
                // Not used for add operation
            }

            @Override
            public void onPaymentRequestDeleted(String paymentRequestId) {
                // Not used for add operation
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoading();
                        showError("Failed to save payment request. Please try again.");
                        // Log error for debugging
                        android.util.Log.e("PaymentRequest", "Error saving payment request: " + error);
                    });
                }
            }
        });
    }
    
    private void showPaymentRequestDetails(PaymentRequest paymentRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment_request_details, null);
        
        // Set data
        TextView textPaymentMethod = dialogView.findViewById(R.id.text_payment_method);
        TextView textInvoiceNumber = dialogView.findViewById(R.id.text_invoice_number);
        TextView textAmount = dialogView.findViewById(R.id.text_amount);
        TextView textDate = dialogView.findViewById(R.id.text_date);
        TextView textTime = dialogView.findViewById(R.id.text_time);
        TextView textStatus = dialogView.findViewById(R.id.text_status);
        Button btnClose = dialogView.findViewById(R.id.btn_close);
        
        textPaymentMethod.setText(paymentRequest.getPaymentMethod());
        textInvoiceNumber.setText(paymentRequest.getInvoiceNumber());
        
        // Format amount with BDT symbol
        String formattedAmount = "৳ " + String.format("%,.2f", paymentRequest.getAmount());
        textAmount.setText(formattedAmount);
        
        textDate.setText(paymentRequest.getDate());
        textTime.setText(paymentRequest.getTime());
        textStatus.setText(paymentRequest.getStatus());
        
        // Set status background color and text color based on status
        if ("Pending".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundColor(0xFFFACC15); // #FACC15 yellow
            textStatus.setTextColor(0xFF000000); // Black text
        } else if ("Approved".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundResource(R.drawable.bg_chip_quantity);
            textStatus.setTextColor(0xFFFFFFFF); // White text
        } else if ("Rejected".equals(paymentRequest.getStatus())) {
            textStatus.setBackgroundResource(R.drawable.bg_notification_dot);
            textStatus.setTextColor(0xFFFFFFFF); // White text
        } else {
            // Default styling for other statuses
            textStatus.setBackgroundResource(R.drawable.bg_chip_date);
            textStatus.setTextColor(0xFFFFFFFF); // White text
        }
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void showPopupMenu(View view, PaymentRequest paymentRequest) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Delete");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete")) {
                deletePaymentRequest(paymentRequest);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void deletePaymentRequest(PaymentRequest paymentRequest) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Payment Request")
            .setMessage("Are you sure you want to delete this payment request?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete from Firebase
                firestoreService.deletePaymentRequest(paymentRequest.getId(), new FirestoreService.PaymentRequestCallback() {
                    @Override
                    public void onPaymentRequestsLoaded(List<PaymentRequest> paymentRequests) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onPaymentRequestAdded(PaymentRequest paymentRequest) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onPaymentRequestUpdated(PaymentRequest paymentRequest) {
                        // Not used for delete operation
                    }

                    @Override
                    public void onPaymentRequestDeleted(String paymentRequestId) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showSuccess("Payment request deleted successfully");
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showError("Failed to delete payment request. Please try again.");
                            });
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
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
} 