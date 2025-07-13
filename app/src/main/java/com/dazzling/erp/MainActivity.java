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
import com.dazzling.erp.fragments.ManagerRongdhonuFragment;
import com.dazzling.erp.fragments.ManagerUttaraFragment;
import com.dazzling.erp.fragments.ManagerPaymentFragment;
import com.dazzling.erp.models.User;
import com.dazzling.erp.services.FirebaseAuthService;
import com.dazzling.erp.ui.auth.LoginActivity;

import com.google.android.material.snackbar.Snackbar;
import com.airbnb.lottie.LottieAnimationView;

import android.os.StrictMode;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.google.firebase.auth.FirebaseUser;

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
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        
        try {
            // Suppress verbose logging first
            suppressVerboseLogging();
            
            // Set content view
            setContentView(R.layout.activity_main);
            
            // Initialize binding
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
            
            // Setup Navigation Drawer
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            
            // Create ActionBarDrawerToggle
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, 
                R.string.navigation_drawer_open, 
                R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            
            // Setup NavigationView item selection listener (CEO only)
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_settings) {
                    // Open ProfileFragment for both CEO and Manager
                    loadFragment(new com.dazzling.erp.fragments.ProfileFragment(), "Profile");
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                // Check if user has CEO role
                if (currentUser != null && "CEO".equals(currentUser.getRole())) {
                    if (id == R.id.nav_rongdhonu_office) {
                        // Handle Rongdhonu Office selection
                        showSnackbar("Rongdhonu Office selected");
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    } else if (id == R.id.nav_uttara_office) {
                        // Handle Uttara Office selection
                        showSnackbar("Uttara Office selected");
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    } else if (id == R.id.nav_payment_request) {
                        // Handle Payment Request selection
                        showSnackbar("Payment Request selected");
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                } else {
                    // User is not CEO - show access denied message
                    showSnackbar("Access denied. CEO role required.");
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                
                return false;
            });
            
            // Check authentication status first
            checkAuthenticationStatus();
            
            // Handle navigation from other activities
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if (navigateTo != null) {
                // Store the navigation intent to be processed after authentication
                // The navigation will be handled in the authentication callbacks
                Log.d(TAG, "Navigation intent received: " + navigateTo);
            }
            // Don't navigate immediately - wait for authentication to complete
            
            // Initialize global loader
            try {
                lottieLoading = findViewById(R.id.lottie_loading);
                showGlobalLoader(false); // Hide by default
            } catch (Exception e) {
                Log.e(TAG, "Error initializing global loader", e);
            }
            
            // Configure StrictMode with better error handling
            configureStrictMode();
            
            // Initialize Search Bar
            initializeSearchBar();
            
            // Mark as initialized
            isInitialized = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Fallback to login if initialization fails
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    /**
     * Suppress verbose logging from system components
     */
    private void suppressVerboseLogging() {
        try {
            // Set system properties to reduce verbose logging
            System.setProperty("log.tag.SurfaceFlinger", "ERROR");
            System.setProperty("log.tag.BufferQueueDebug", "ERROR");
            System.setProperty("log.tag.OpenGLRenderer", "ERROR");
            System.setProperty("log.tag.TranClassInfo", "ERROR");
            System.setProperty("log.tag.QT", "ERROR");
            
            // Suppress IME callback warnings
            System.setProperty("log.tag.InputMethodManager", "ERROR");
            
            // Suppress system-level scroll and behavior warnings
            System.setProperty("log.tag.ScrollIdentify", "ERROR");
            System.setProperty("log.tag.SBE", "ERROR");
            System.setProperty("log.tag.BoostFwk", "ERROR");
            System.setProperty("log.tag.TasksUtil", "ERROR");
            System.setProperty("log.tag.ActivityInfo", "ERROR");
            
            // Suppress StrictMode warnings for system components
            System.setProperty("log.tag.StrictMode", "ERROR");
            
        } catch (Exception e) {
            Log.e(TAG, "Error suppressing verbose logging", e);
        }
    }
    
    /**
     * Configure StrictMode with better error handling
     */
    private void configureStrictMode() {
        try {
            // Use a more lenient StrictMode policy to avoid system-level violations
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .penaltyLog()
                .permitNetwork() // Allow network operations
                .permitDiskReads() // Allow disk reads to prevent system violations
                .permitDiskWrites() // Allow disk writes
                .build());
            
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build());
        } catch (Exception e) {
            Log.e(TAG, "Error configuring StrictMode", e);
        }
    }
    
    /**
     * Setup bottom navigation based on user role
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        
        if (currentUser != null && "Manager".equals(currentUser.getRole())) {
            // Manager role - show manager menu
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.manager_bottom_navigation_menu);
            
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_manager_rongdhonu) {
                    loadFragment(new ManagerRongdhonuFragment(), "Rongdhonu Office");
                    return true;
                } else if (id == R.id.nav_manager_uttara) {
                    loadFragment(new ManagerUttaraFragment(), "Uttara Office");
                    return true;
                } else if (id == R.id.nav_manager_payment) {
                    loadFragment(new ManagerPaymentFragment(), "Payment Request");
                    return true;
                }
                return false;
            });
            
            // Set default fragment for Manager
            loadFragment(new ManagerRongdhonuFragment(), "Rongdhonu Office");
            bottomNav.setSelectedItemId(R.id.nav_manager_rongdhonu);
            
        } else {
            // CEO role - show full menu
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.bottom_navigation_menu);
            
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
            
            // Set default fragment for CEO
            loadFragment(new DashboardFragment(), "Dashboard");
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    /**
     * Initialize search bar with error handling
     */
    private void initializeSearchBar() {
        try {
            View searchBar = findViewById(R.id.search_bar);
            if (searchBar != null) {
                final View searchCard = searchBar;
                final ImageView searchIcon = searchBar.findViewById(R.id.search_icon);
                final ImageView clearIcon = searchBar.findViewById(R.id.clear_icon);
                final android.widget.EditText editText = searchBar.findViewById(R.id.edit_text_search);

                // Animate search bar: slide in from top and fade in
                searchCard.setTranslationY(-100f);
                searchCard.setAlpha(0f);
                searchCard.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(500)
                        .setStartDelay(200)
                        .start();

                // Focus EditText and show keyboard
                editText.requestFocus();
                editText.postDelayed(() -> {
                    if (!isDestroyed && !isFinishing()) {
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                }, 400);

                // Show clear icon only when text is not empty
                editText.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (clearIcon != null) {
                            clearIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });

                // Clear text when clear icon is pressed
                if (clearIcon != null) {
                    clearIcon.setOnClickListener(v -> editText.setText(""));
                }

                // Optional: handle search icon click (e.g., trigger search)
                if (searchIcon != null) {
                    searchIcon.setOnClickListener(v -> {
                        // You can trigger search here if needed
                        // For now, just focus the EditText
                        editText.requestFocus();
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing search bar", e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        isDestroyed = false;
        
        try {
            // Check if user is still authenticated
            if (isInitialized && authService != null) {
                // Use the correct method to check current user
                if (authService.isUserSignedIn()) {
                    FirebaseUser currentUser = authService.getCurrentUser();
                    if (currentUser != null) {
                        // User is signed in, fetch their data
                        authService.fetchUserData(currentUser.getUid());
                    }
                } else {
                    // User is not signed in, redirect to login
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
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
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        isDestroyed = true;
        
        try {
            // Clean up resources
            if (authService != null) {
                authService.setAuthCallback(null);
                authService = null;
            }
            
            if (binding != null) {
                binding = null;
            }
            
            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        } finally {
            super.onDestroy();
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
    
    private boolean doubleBackToExitPressedOnce = false;
    
    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        // Check if we're on the first fragment (Dashboard)
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        
        if (currentFragment instanceof DashboardFragment) {
            // If on dashboard, show exit confirmation
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            
            new android.os.Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        } else {
            // If not on dashboard, go back to dashboard
            loadFragment(new DashboardFragment(), "Dashboard");
            
            // Update bottom navigation to reflect dashboard selection
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_dashboard);
            }
        }
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
        
        try {
            // Hide loading indicator
            showGlobalLoader(false);
            updateNavigationHeader(user);
            
            // Setup navigation based on user role
            setupBottomNavigation();
            
            // Start background service to prevent process killing
            startBackgroundService();
            
            // Check if we have a navigation intent, if not, load appropriate default
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if (navigateTo != null) {
                // Navigate to the requested fragment
                navigateToFragment(navigateTo);
                // Clear the intent extra to prevent reprocessing
                getIntent().removeExtra("navigate_to");
            } else {
                // Load appropriate default fragment based on user role
                if (user != null && "Manager".equals(user.getRole())) {
                    loadFragment(new com.dazzling.erp.fragments.ManagerRongdhonuFragment(), "Rongdhonu Office");
                } else {
                    loadFragment(new DashboardFragment(), "Dashboard");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onAuthSuccess", e);
            // Fallback: just hide loader and show appropriate default
            showGlobalLoader(false);
            if (user != null && "Manager".equals(user.getRole())) {
                loadFragment(new com.dazzling.erp.fragments.ManagerRongdhonuFragment(), "Rongdhonu Office");
            } else {
                loadFragment(new DashboardFragment(), "Dashboard");
            }
        }
    }
    
    @Override
    public void onAuthFailure(String error) {
        Log.e(TAG, "onAuthFailure: " + error);
        
        try {
            // Hide loading indicator
            showGlobalLoader(false);
            Toast.makeText(this, "Authentication error: " + error, Toast.LENGTH_LONG).show();
            // Redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error in onAuthFailure", e);
            // Fallback: just redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    @Override
    public void onUserCreated(User user) {
        Log.d(TAG, "onUserCreated: New user created");
        this.currentUser = user;
        
        try {
            // Hide loading indicator
            showGlobalLoader(false);
            updateNavigationHeader(user);
            
            // Setup navigation based on user role
            setupBottomNavigation();
            
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            
            // Check if we have a navigation intent, if not, load appropriate default
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if (navigateTo != null) {
                // Navigate to the requested fragment
                navigateToFragment(navigateTo);
                // Clear the intent extra to prevent reprocessing
                getIntent().removeExtra("navigate_to");
            } else {
                // Load appropriate default fragment based on user role
                if (user != null && "Manager".equals(user.getRole())) {
                    loadFragment(new com.dazzling.erp.fragments.ManagerRongdhonuFragment(), "Rongdhonu Office");
                } else {
                    loadFragment(new DashboardFragment(), "Dashboard");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onUserCreated", e);
            // Fallback: just hide loader and show appropriate default
            showGlobalLoader(false);
            if (user != null && "Manager".equals(user.getRole())) {
                loadFragment(new com.dazzling.erp.fragments.ManagerRongdhonuFragment(), "Rongdhonu Office");
            } else {
                loadFragment(new DashboardFragment(), "Dashboard");
            }
        }
    }
    
    @Override
    public void onUserFetched(User user) {
        Log.d(TAG, "onUserFetched: User data fetched successfully");
        this.currentUser = user;
        
        try {
            // Hide loading indicator
            showGlobalLoader(false);
            updateNavigationHeader(user);
            
            // Setup navigation based on user role
            setupBottomNavigation();
            
            // Check if we have a navigation intent, if not, load appropriate default
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if (navigateTo != null) {
                // Navigate to the requested fragment
                navigateToFragment(navigateTo);
                // Clear the intent extra to prevent reprocessing
                getIntent().removeExtra("navigate_to");
            } else {
                // Load appropriate default fragment based on user role
                if (user != null && "Manager".equals(user.getRole())) {
                    loadFragment(new ManagerRongdhonuFragment(), "Rongdhonu Office");
                } else {
                    loadFragment(new DashboardFragment(), "Dashboard");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onUserFetched", e);
            // Fallback: just hide loader and show appropriate default
            showGlobalLoader(false);
            if (user != null && "Manager".equals(user.getRole())) {
                loadFragment(new ManagerRongdhonuFragment(), "Rongdhonu Office");
            } else {
                loadFragment(new DashboardFragment(), "Dashboard");
            }
        }
    }
    
    /**
     * Update navigation header with user info
     */
    private void updateNavigationHeader(User user) {
        try {
            // Update toolbar title
            if (user != null && getSupportActionBar() != null) {
                String displayName = user.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    getSupportActionBar().setTitle(displayName + " - ERP");
                } else {
                    getSupportActionBar().setTitle("ERP System");
                }
            }
            
            // Update navigation drawer header
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                if (headerView != null) {
                    TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
                    TextView emailTextView = headerView.findViewById(R.id.nav_header_email);
                    
                    if (user != null) {
                        // Get user name - use role if display name is generic or empty
                        String displayName = user.getDisplayName();
                        String userRole = user.getRole();
                        
                        // If display name is generic ("User") or empty, use the role instead
                        String userName = displayName;
                        if (displayName == null || displayName.isEmpty() || "User".equals(displayName)) {
                            userName = userRole != null ? userRole : "CEO";
                        }
                        
                        // Update user name
                        nameTextView.setText("User: " + userName);
                        
                        // Update email
                        String email = user.getEmail();
                        if (email != null && !email.isEmpty()) {
                            emailTextView.setText("Email: " + email);
                        } else {
                            emailTextView.setText("Email: No email available");
                        }
                        
                        // Update role to show user name as role (same as user name)
                        TextView roleTextView = headerView.findViewById(R.id.nav_header_role);
                        if (roleTextView != null) {
                            roleTextView.setText("Role: " + userName);
                        }
                    } else {
                        // Default values if no user
                        nameTextView.setText("User: Guest");
                        emailTextView.setText("Email: No email available");
                        
                        // Default role
                        TextView roleTextView = headerView.findViewById(R.id.nav_header_role);
                        if (roleTextView != null) {
                            roleTextView.setText("Role: CEO");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating navigation header", e);
            // Fallback: set default title
            try {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("ERP System");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error setting fallback title", ex);
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
        try {
            if (lottieLoading == null) return;
            if (show) {
                lottieLoading.setVisibility(View.VISIBLE);
                lottieLoading.playAnimation();
            } else {
                lottieLoading.cancelAnimation();
                lottieLoading.setVisibility(View.GONE);
                lottieLoading.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing/hiding global loader", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        // Handle menu item selections here if needed
        // Removed test crash functionality for safety
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * Navigate to a specific fragment and update bottom navigation
     */
    public void navigateToFragment(String fragmentType) {
        try {
            Log.d(TAG, "navigateToFragment: Navigating to " + fragmentType);
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
            
            switch (fragmentType) {
                case "lots":
                    Log.d(TAG, "Loading LotsFragment");
                    loadFragment(new LotsFragment(), "Lot Management");
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_lots);
                        Log.d(TAG, "Set bottom navigation to lots");
                    }
                    break;
                case "dashboard":
                    Log.d(TAG, "Loading DashboardFragment");
                    loadFragment(new DashboardFragment(), "Dashboard");
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_dashboard);
                        Log.d(TAG, "Set bottom navigation to dashboard");
                    }
                    break;
                case "fabrics":
                    Log.d(TAG, "Loading FabricsFragment");
                    loadFragment(new FabricsFragment(), "Fabrics Management");
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_fabrics);
                        Log.d(TAG, "Set bottom navigation to fabrics");
                    }
                    break;
                case "cutting":
                    Log.d(TAG, "Loading CuttingFragment");
                    loadFragment(new CuttingFragment(), "Cutting Operations");
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_cutting);
                        Log.d(TAG, "Set bottom navigation to cutting");
                    }
                    break;
                default:
                    Log.d(TAG, "Unknown fragment type: " + fragmentType + ", loading dashboard");
                    // Load default fragment
                    loadFragment(new DashboardFragment(), "Dashboard");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to fragment: " + fragmentType, e);
            // Fallback to dashboard
            loadFragment(new DashboardFragment(), "Dashboard");
        }
    }
}