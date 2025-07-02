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

import android.os.StrictMode;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;

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
        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
            
            // Setup MaterialToolbar
            MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
            setSupportActionBar(toolbar);
            
            // Setup BottomNavigationView
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    loadFragment(new DashboardFragment(), "Dashboard");
                    return true;
                } else if (id == R.id.nav_fabrics) {
                    loadFragment(new FabricsFragment(), "Fabrics Management");
                    return true;
                } else if (id == R.id.nav_cutting) {
                    loadFragment(new CuttingFragment(), "Cutting Operations");
                    return true;
                } else if (id == R.id.nav_lots) {
                    loadFragment(new LotsFragment(), "Lot Management");
                    return true;
                }
                return false;
            });
            
            isInitialized = true;
            
            // Check authentication status after UI is ready
            binding.getRoot().post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    checkAuthenticationStatus();
                }
            });
            
            // Load default fragment
            loadFragment(new DashboardFragment(), "Dashboard");
            
            // Initialize global loader
            lottieLoading = findViewById(R.id.lottie_loading);
            showGlobalLoader(false); // Hide by default
            
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            
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
     * Load fragment with title
     */
    private void loadFragment(Fragment fragment, String title) {
        if (isFinishing() || isDestroyed()) return;
        // Avoid reloading the same fragment
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            if (title != null && !title.isEmpty()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
            return;
        }
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
            currentFragment = fragment;
            if (title != null && !title.isEmpty()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
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
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof com.dazzling.erp.fragments.DashboardFragment) {
            ((com.dazzling.erp.fragments.DashboardFragment) current).reloadDashboardData();
        }
        // Hide loading indicator
        showGlobalLoader(false);
        updateNavigationHeader(user);
        
        // Start background service to prevent process killing
        startBackgroundService();
        
        // Set default navigation item
        loadFragment(new DashboardFragment(), "Dashboard");
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
        loadFragment(new DashboardFragment(), "Dashboard");
        // Load default dashboard fragment only after successful authentication
        loadFragment(new DashboardFragment(), "Dashboard");
    }
    
    @Override
    public void onUserFetched(User user) {
        Log.d(TAG, "onUserFetched: User data fetched successfully");
        this.currentUser = user;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof com.dazzling.erp.fragments.DashboardFragment) {
            ((com.dazzling.erp.fragments.DashboardFragment) current).reloadDashboardData();
        }
        // Hide loading indicator
        showGlobalLoader(false);
        updateNavigationHeader(user);
        // Set default navigation item
        loadFragment(new DashboardFragment(), "Dashboard");
        // Load default dashboard fragment only after successful authentication
        loadFragment(new DashboardFragment(), "Dashboard");
    }
    
    /**
     * Update navigation header with user info
     */
    private void updateNavigationHeader(User user) {
        if (user != null && getSupportActionBar() != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                getSupportActionBar().setTitle(displayName + " - ERP");
            } else {
                getSupportActionBar().setTitle("ERP System");
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