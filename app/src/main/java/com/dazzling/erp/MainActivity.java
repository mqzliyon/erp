package com.dazzling.erp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.dazzling.erp.databinding.ActivityMainBinding;
import com.dazzling.erp.fragments.DashboardFragment;
import com.dazzling.erp.fragments.FabricsFragment;
import com.dazzling.erp.fragments.LotsFragment;
import com.dazzling.erp.fragments.CuttingFragment;
import com.dazzling.erp.models.User;
import com.dazzling.erp.services.FirebaseAuthService;
import com.dazzling.erp.ui.auth.LoginActivity;

import com.google.android.material.snackbar.Snackbar;
import com.airbnb.lottie.LottieAnimationView;

/**
 * Main Activity for the ERP application
 * Handles navigation, authentication, and theme switching
 */
public class MainActivity extends AppCompatActivity implements 
        FirebaseAuthService.AuthCallback {
    
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuthService authService;
    User currentUser; // Package-private for fragment access
    private boolean isInitialized = false;
    private Fragment currentFragment = null;
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 250;
    private LottieAnimationView lottieLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Force status bar color to white and icons to dark
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(android.graphics.Color.WHITE);
                getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
            
            // Initialize Firebase Auth Service
            authService = new FirebaseAuthService();
            authService.setAuthCallback(this);
            
            // Setup toolbar
            // setSupportActionBar(binding.toolbar); // Removed: no toolbar in layout
            binding.topTitle.setText("ERP Dashboard");
            
            // Setup custom bottom menu listeners
            setupBottomMenu();
            
            isInitialized = true;
            
            // Check authentication status after UI is ready
            binding.getRoot().post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    checkAuthenticationStatus();
                }
            });
            
            // Load default fragment
            loadFragment(new DashboardFragment(), "Dashboard");
            highlightMenuItem(R.id.menu_dashboard);
            
            // Initialize global loader
            lottieLoading = findViewById(R.id.lottie_loading);
            showGlobalLoader(false); // Hide by default
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Fallback to login if initialization fails
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Only check authentication if not already initialized
        if (isInitialized && currentUser == null && authService != null) {
            checkAuthenticationStatus();
        }
    }
    
    /**
     * Setup custom bottom menu listeners
     */
    private void setupBottomMenu() {
        findViewById(R.id.menu_dashboard).setOnClickListener(v -> {
            if (!canClick()) return;
            loadFragment(new DashboardFragment(), "Dashboard");
            highlightMenuItem(R.id.menu_dashboard);
        });
        findViewById(R.id.menu_fabrics).setOnClickListener(v -> {
            if (!canClick()) return;
            loadFragment(new FabricsFragment(), "Fabrics Management");
            highlightMenuItem(R.id.menu_fabrics);
        });
        findViewById(R.id.menu_cutting).setOnClickListener(v -> {
            if (!canClick()) return;
            loadFragment(new CuttingFragment(), "Cutting Operations");
            highlightMenuItem(R.id.menu_cutting);
        });
        findViewById(R.id.menu_lots).setOnClickListener(v -> {
            if (!canClick()) return;
            loadFragment(new LotsFragment(), "Lot Management");
            highlightMenuItem(R.id.menu_lots);
        });
    }
    
    /**
     * Highlight selected menu item
     */
    private void highlightMenuItem(int selectedId) {
        int[] menuIds = {R.id.menu_dashboard, R.id.menu_fabrics, R.id.menu_cutting, R.id.menu_lots};
        int[] iconIds = {R.id.icon_dashboard, R.id.icon_fabrics, R.id.icon_cutting, R.id.icon_lots};
        int[] labelIds = {R.id.label_dashboard, R.id.label_fabrics, R.id.label_cutting, R.id.label_lots};
        for (int i = 0; i < menuIds.length; i++) {
            LinearLayout menuItem = findViewById(menuIds[i]);
            ImageView icon = findViewById(iconIds[i]);
            TextView label = findViewById(labelIds[i]);
            boolean selected = (menuIds[i] == selectedId);
            if (menuItem != null) {
                menuItem.setBackgroundResource(selected ? R.drawable.menu_item_selected_bg : android.R.color.transparent);
            }
            if (icon != null) {
                int color = getResources().getColor(R.color.icon_nav);
                icon.setColorFilter(color);
            }
            if (label != null) {
                if (selected) {
                    label.setAlpha(0f);
                    label.setVisibility(View.VISIBLE);
                    label.animate().alpha(1f).setDuration(150).start();
                    label.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    label.animate().alpha(0f).setDuration(100).withEndAction(() -> label.setVisibility(View.GONE)).start();
                    label.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
                label.setTextColor(getResources().getColor(R.color.black));
            }
        }
    }
    
    /**
     * Load fragment with title
     */
    private void loadFragment(Fragment fragment, String title) {
        if (isFinishing() || isDestroyed()) return;
        // Avoid reloading the same fragment
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            if (title != null && !title.isEmpty()) {
                binding.topTitle.setText(title);
            }
            return;
        }
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNowAllowingStateLoss(); // Instant, no lag
            currentFragment = fragment;
            if (title != null && !title.isEmpty()) {
                binding.topTitle.setText(title);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment", e);
        }
    }
    
    /**
     * Check if user is authenticated
     */
    private void checkAuthenticationStatus() {
        if (isFinishing() || isDestroyed() || authService == null) {
            return;
        }
        
        Log.d(TAG, "Checking authentication status...");
        
        try {
            if (!authService.isUserSignedIn()) {
                Log.d(TAG, "User not authenticated, redirecting to LoginActivity");
                // Hide loading indicator
                showGlobalLoader(false);
                // User not authenticated, redirect to login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.d(TAG, "User is authenticated, fetching user data");
                // User is authenticated, fetch user data
                String userId = authService.getCurrentUserId();
                if (userId != null) {
                    authService.fetchUserData(userId);
                } else {
                    Log.w(TAG, "User ID is null despite being signed in");
                    // Hide loading indicator
                    showGlobalLoader(false);
                    // Fallback: redirect to login
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking authentication status", e);
            // Fallback to login on error
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_toggle_theme) {
            toggleTheme();
            return true;
        } else if (itemId == R.id.action_profile) {
            showProfile();
            return true;
        } else if (itemId == R.id.action_settings) {
            showSettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Toggle between light and dark theme
     */
    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) 
                ? AppCompatDelegate.MODE_NIGHT_NO 
                : AppCompatDelegate.MODE_NIGHT_YES;
        
        AppCompatDelegate.setDefaultNightMode(newMode);
        
        String themeName = (newMode == AppCompatDelegate.MODE_NIGHT_YES) ? "Dark" : "Light";
        Snackbar.make(binding.getRoot(), "Switched to " + themeName + " theme", 
                Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * Show user profile
     */
    private void showProfile() {
        if (currentUser != null) {
            String message = "User: " + currentUser.getDisplayName() + 
                    "\nRole: " + currentUser.getRole() + 
                    "\nDepartment: " + currentUser.getDepartment();
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show settings
     */
    private void showSettings() {
        Snackbar.make(binding.getRoot(), "Settings coming soon!", Snackbar.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBackPressed() {
        // Since drawer is always open, just handle normal back press
        super.onBackPressed();
    }
    

    
    /**
     * Show a snackbar message
     */
    private void showSnackbar(String message) {
        if (binding != null && binding.getRoot() != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Logout user
     */
    private void logout() {
        authService.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    // ==================== AUTH CALLBACKS ====================
    
    @Override
    public void onAuthSuccess(User user) {
        Log.d(TAG, "onAuthSuccess: User authenticated successfully");
        this.currentUser = user;
        // Hide loading indicator
        showGlobalLoader(false);
        updateNavigationHeader(user);
        
        // Start background service to prevent process killing
        startBackgroundService();
        
        // Set default navigation item
        highlightMenuItem(R.id.menu_dashboard);
        // Load default dashboard fragment only after successful authentication
        loadFragment(new DashboardFragment(), "Dashboard");
    }
    
    @Override
    public void onAuthFailure(String error) {
        Log.e(TAG, "onAuthFailure: " + error);
        // Hide loading indicator
        showGlobalLoader(false);
        Toast.makeText(this, "Authentication error: " + error, Toast.LENGTH_LONG).show();
        // Redirect to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onUserCreated(User user) {
        Log.d(TAG, "onUserCreated: New user created");
        this.currentUser = user;
        // Hide loading indicator
        showGlobalLoader(false);
        updateNavigationHeader(user);
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        // Set default navigation item
        highlightMenuItem(R.id.menu_dashboard);
        // Load default dashboard fragment only after successful authentication
        loadFragment(new DashboardFragment(), "Dashboard");
    }
    
    @Override
    public void onUserFetched(User user) {
        Log.d(TAG, "onUserFetched: User data fetched successfully");
        this.currentUser = user;
        // Hide loading indicator
        showGlobalLoader(false);
        updateNavigationHeader(user);
        // Set default navigation item
        highlightMenuItem(R.id.menu_dashboard);
        // Load default dashboard fragment only after successful authentication
        loadFragment(new DashboardFragment(), "Dashboard");
    }
    
    /**
     * Update navigation header with user info
     */
    private void updateNavigationHeader(User user) {
        // Since we no longer have a NavigationView, we can update the toolbar title
        // or show user info in a different way if needed
        if (user != null && binding.topTitle != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                binding.topTitle.setText(displayName + " - ERP");
            } else {
                binding.topTitle.setText("ERP System");
            }
        }
    }
    
    /**
     * Get current user for fragment access
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isUserAuthenticated() {
        return currentUser != null;
    }
    
    /**
     * Start background service to prevent process killing
     */
    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, com.dazzling.erp.services.BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Background service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting background service", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
        
        // Clean up resources
        if (authService != null) {
            authService.setAuthCallback(null);
        }
        
        // Stop background service if app is being destroyed
        try {
            Intent serviceIntent = new Intent(this, com.dazzling.erp.services.BackgroundService.class);
            stopService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping background service", e);
        }
        
        // Clear binding reference
        binding = null;
    }

    private boolean canClick() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_DEBOUNCE_MS) return false;
        lastClickTime = now;
        return true;
    }

    /**
     * Show or hide the global Lottie loader. Fragments can call ((MainActivity) requireActivity()).showGlobalLoader(true/false)
     */
    public void showGlobalLoader(boolean show) {
        if (lottieLoading == null) return;
        if (show) {
            lottieLoading.setVisibility(View.VISIBLE);
            lottieLoading.playAnimation();
        } else {
            lottieLoading.cancelAnimation();
            lottieLoading.setVisibility(View.GONE);
            lottieLoading.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }
}