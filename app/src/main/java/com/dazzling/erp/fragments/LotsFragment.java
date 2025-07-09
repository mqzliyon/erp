package com.dazzling.erp.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.Window;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.os.Handler;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dazzling.erp.R;
import com.dazzling.erp.models.Lot;
import com.dazzling.erp.services.FirestoreService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

/**
 * Fragment for managing lots
 * Displays list of lots and allows CRUD operations
 */
public class LotsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAdd;
    private List<Lot> lotsList;
    private FirestoreService firestoreService;
    private LotsAdapter lotsAdapter;
    private Dialog loadingDialog;
    private java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lots, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadLots();
        
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Search bar logic
        View searchBar = view.findViewById(R.id.search_bar);
        View searchOverlay = view.findViewById(R.id.search_overlay);
        if (searchBar != null) {
            final View searchCard = searchBar;
            final ImageView searchIcon = searchBar.findViewById(R.id.search_icon);
            final ImageView clearIcon = searchBar.findViewById(R.id.clear_icon);
            final EditText editText = searchBar.findViewById(R.id.edit_text_search);

            // Overlay click hides search bar (only when clicking outside search bar)
            if (searchOverlay != null) {
                searchOverlay.setOnClickListener(v2 -> {
                    // Only hide if clicking on the overlay, not on the search bar itself
                    hideSearchBar();
                });
            }

            // Hide search bar on back press
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (searchBar.getVisibility() == View.VISIBLE) {
                        hideSearchBar();
                    } else {
                        setEnabled(false);
                        requireActivity().onBackPressed();
                        setEnabled(true);
                    }
                }
            });

            // Animate search bar: slide in from top and fade in
            searchCard.setTranslationY(-100f);
            searchCard.setAlpha(0f);
            searchBar.setVisibility(View.GONE);
            if (searchOverlay != null) searchOverlay.setVisibility(View.GONE);

            // Allow clicking on EditText to focus (for when user clicks on it later)
            editText.setOnClickListener(v -> editText.requestFocus());
            
            // Prevent search bar from closing when clicking on it
            searchBar.setOnClickListener(v -> {
                // Do nothing - prevent event from bubbling to overlay
            });

            // Show clear icon only when text is not empty
            editText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Update clear icon visibility
                    clearIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    
                    // Filter lots based on current text
                    if (lotsAdapter != null) {
                        lotsAdapter.filterLots(s.toString());
                    }
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            // Clear text when clear icon is pressed
            clearIcon.setOnClickListener(v -> {
                editText.setText("");
                // Reset the filter to show all lots
                if (lotsAdapter != null) {
                    lotsAdapter.filterLots("");
                }
                // Close the search bar when X is clicked (optional behavior)
                // Uncomment the next line if you want the search bar to close when X is clicked
                // hideSearchBar();
            });

            // Optional: handle search icon click (e.g., trigger search)
            searchIcon.setOnClickListener(v -> editText.requestFocus());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_lot_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_download) {
            // Handle download functionality
            showDownloadDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_lots);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        fabAdd = view.findViewById(R.id.fab_add_lot);
        
        lotsList = new ArrayList<>();
        firestoreService = new FirestoreService();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lotsAdapter = new LotsAdapter(lotsList);
        recyclerView.setAdapter(lotsAdapter);

        lotsAdapter.setOnLotMenuClickListener(new LotsAdapter.OnLotMenuClickListener() {
            @Override
            public void onView(Lot lot) {
                // Launch lot detail activity
                android.content.Intent intent = new android.content.Intent(requireContext(), com.dazzling.erp.ui.lot.LotDetailActivity.class);
                intent.putExtra("lot_id", lot.getId());
                intent.putExtra("lot_number", lot.getLotNumber());
                intent.putExtra("lot_date", lot.getOrderDate() != null ? dateFormat.format(lot.getOrderDate()) : "");
                intent.putExtra("lot_created", lot.getCreatedAt() != null ? dateFormat.format(lot.getCreatedAt()) : "");
                startActivity(intent);
            }
            @Override
            public void onEdit(Lot lot) {}
            @Override
            public void onDelete(Lot lot) {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Lot")
                    .setMessage("Are you sure you want to delete this lot?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        showLoadingDialog();
                        firestoreService.deleteLot(lot.getId(), new FirestoreService.LotCallback() {
                            @Override
                            public void onLotsLoaded(List<Lot> lots) {}
                            @Override
                            public void onLotAdded(Lot lot) {}
                            @Override
                            public void onLotUpdated(Lot lot) {}
                            @Override
                            public void onLotDeleted(String lotId) {
                                hideLoadingDialog();
                                showSuccessAlert("Lot deleted successfully!");
                                loadLots();
                            }
                            @Override
                            public void onError(String error) {
                                hideLoadingDialog();
                                new android.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Error")
                                    .setMessage("Failed to delete lot: " + error)
                                    .setPositiveButton("OK", null)
                                    .show();
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> showAddLotDialog());
    }

    private void showAddLotDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_lot);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Get dialog root view and input fields
        View dialogRoot = dialog.findViewById(R.id.dialog_root);
        EditText editLotNumber = dialog.findViewById(R.id.edit_lot_number);
        EditText editLotDate = dialog.findViewById(R.id.edit_lot_date);
        Button buttonAdd = dialog.findViewById(R.id.button_add);
        Button buttonCancel = dialog.findViewById(R.id.button_cancel);

        // Colors for background change
        int highlightColor = Color.parseColor("#dad5d4");
        int defaultColor = Color.WHITE; // or whatever your normal background is

        // Focus change listener (only dialog background)
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                dialogRoot.setBackgroundColor(highlightColor);
            } else if (!editLotNumber.hasFocus() && !editLotDate.hasFocus()) {
                dialogRoot.setBackgroundColor(defaultColor);
            }
        };

        // Set focus listeners
        editLotNumber.setOnFocusChangeListener(focusListener);
        editLotDate.setOnFocusChangeListener(focusListener);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());
        editLotDate.setText(currentDate);

        editLotDate.setOnClickListener(v -> {
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                java.util.Calendar selected = java.util.Calendar.getInstance();
                selected.set(y, m, d);
                editLotDate.setText(sdf.format(selected.getTime()));
            }, year, month, day);
            datePickerDialog.show();
        });

        buttonAdd.setOnClickListener(v -> {
            String lotNumber = editLotNumber.getText().toString().trim();
            String lotDate = editLotDate.getText().toString().trim();
            
            if (lotNumber.isEmpty()) {
                editLotNumber.setError("Please enter lot number");
                return;
            }
            
            // Prepend 'Lot-' to the lot number
            final String finalLotNumber = "Lot-" + lotNumber;
            
            // Parse date string to Date object
            final java.util.Date parsedDate;
            try {
                parsedDate = sdf.parse(lotDate);
            } catch (Exception e) {
                editLotDate.setError("Invalid date");
                return;
            }
            
            showLoadingDialog();
            
            // Create lot with all required fields properly initialized
            Lot newLot = new Lot();
            newLot.setLotNumber(finalLotNumber);
            newLot.setOrderDate(parsedDate);
            newLot.setCreatedAt(new java.util.Date());
            newLot.setUpdatedAt(new java.util.Date());
            
            // Initialize all required fields with default values to prevent serialization issues
            newLot.setFabricType(""); // Will be set later
            newLot.setColor("");
            newLot.setSupplier("");
            newLot.setCustomer("");
            newLot.setOrderNumber("");
            newLot.setTotalFabricKg(0.0);
            newLot.setCuttingKg(0.0);
            newLot.setCuttingPcs(0);
            newLot.setEmbroideryReceiveKg(0.0);
            newLot.setEmbroideryRejectKg(0.0);
            newLot.setOfficeShipmentKg(0.0);
            newLot.setFactoryBalanceAGradeKg(0.0);
            newLot.setFactoryBalanceBGradeKg(0.0);
            newLot.setFactoryBalanceRejectKg(0.0);
            newLot.setStatus("active");
            newLot.setPriority("medium");
            newLot.setQuality("A");
            newLot.setCreatedBy("user"); // Default user
            newLot.setUpdatedBy("user");
            newLot.setNotes("");
            
            // Set delivery date to same as order date for now
            newLot.setDeliveryDate(parsedDate);
            
            // Initialize all date fields to prevent null pointer issues
            newLot.setCuttingStartDate(null);
            newLot.setCuttingEndDate(null);
            newLot.setEmbroideryStartDate(null);
            newLot.setEmbroideryEndDate(null);
            newLot.setShipmentDate(null);
            
            android.util.Log.d("LotsFragment", "Creating new lot: " + finalLotNumber);
            
            firestoreService.addLot(newLot, new FirestoreService.LotCallback() {
                @Override
                public void onLotsLoaded(List<Lot> lots) {}
                @Override
                public void onLotAdded(Lot lot) {
                    hideLoadingDialog();
                    dialog.dismiss();
                    dialogRoot.setBackgroundColor(defaultColor); // Reset background
                    showSuccessAlert("Lot added successfully!");
                    loadLots(); // Refresh the list
                    android.util.Log.d("LotsFragment", "Successfully created lot: " + lot.getLotNumber());
                }
                @Override
                public void onLotUpdated(Lot lot) {}
                @Override
                public void onLotDeleted(String lotId) {}
                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    android.widget.Toast.makeText(requireContext(), "Error adding lot: " + error, android.widget.Toast.LENGTH_LONG).show();
                    android.util.Log.e("LotsFragment", "Failed to create lot: " + error);
                }
            });
        });

        buttonCancel.setOnClickListener(v -> {
            dialog.dismiss();
            dialogRoot.setBackgroundColor(defaultColor); // Reset background
        });

        dialog.show();
    }

    private void showSuccessAlert(String message) {
        Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(Color.TRANSPARENT); // transparent background
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.parseColor("#137333")); // dark green text (or use your preferred color)
        textView.setTextSize(16);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // center align text
        snackbar.show();
    }

    private void loadLots() {
        firestoreService.getLots(new FirestoreService.LotCallback() {
            @Override
            public void onLotsLoaded(List<Lot> lots) {
                lotsList.clear();
                if (lots != null && !lots.isEmpty()) {
                    lotsList.addAll(lots);
                    lotsAdapter.setLots(lotsList);
                    showLotsList();
                } else {
                    lotsAdapter.setLots(lotsList);
                    showEmptyState();
                }
            }
            @Override
            public void onLotAdded(Lot lot) {}
            @Override
            public void onLotUpdated(Lot lot) {}
            @Override
            public void onLotDeleted(String lotId) {}
            @Override
            public void onError(String error) {
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("No lots found.\nTap + to add a new lot.");
    }

    private void showLotsList() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }

    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;
        loadingDialog = new Dialog(requireContext());
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading_lottie);
        loadingDialog.setCancelable(false);
        LottieAnimationView lottie = loadingDialog.findViewById(R.id.lottie_loader);
        lottie.setAnimation("loader.json");
        lottie.playAnimation();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void hideSearchBar() {
        View searchBar = getView().findViewById(R.id.search_bar);
        View searchOverlay = getView().findViewById(R.id.search_overlay);
        if (searchBar != null && searchBar.getVisibility() == View.VISIBLE) {
            searchBar.animate()
                    .translationY(-100f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        searchBar.setVisibility(View.GONE);
                        if (searchOverlay != null) searchOverlay.setVisibility(View.GONE);
                        EditText editText = searchBar.findViewById(R.id.edit_text_search);
                        if (editText != null) {
                            editText.clearFocus();
                            editText.setText(""); // Clear the search text
                            // Reset the filter to show all lots
                            if (lotsAdapter != null) {
                                lotsAdapter.filterLots("");
                            }
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                            }
                        }
                    })
                    .start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        try {
            // Hide keyboard when fragment is paused
            if (getActivity() != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null && getView() != null) {
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                }
            }
            
            // Hide loading dialog if showing
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("LotsFragment", "Error in onPause", e);
        }
    }

    private void showDownloadDialog() {
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Download Options")
            .setMessage("What would you like to download?")
            .setPositiveButton("Download All Lots", (dialog, which) -> {
                downloadAllLots();
            })
            .setNegativeButton("Download Selected", (dialog, which) -> {
                downloadSelectedLots();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void downloadAllLots() {
        // Show loading dialog
        showLoadingDialog();
        
        // Simulate download process
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hideLoadingDialog();
            android.widget.Toast.makeText(requireContext(), "All lots downloaded successfully!", android.widget.Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void downloadSelectedLots() {
        // Show loading dialog
        showLoadingDialog();
        
        // Simulate download process
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hideLoadingDialog();
            android.widget.Toast.makeText(requireContext(), "Selected lots downloaded successfully!", android.widget.Toast.LENGTH_SHORT).show();
        }, 2000);
    }
} 