package com.dazzling.erp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dazzling.erp.MainActivity;
import com.dazzling.erp.R;
import com.dazzling.erp.databinding.ActivityLoginBinding;
import com.dazzling.erp.models.User;
import com.dazzling.erp.services.FirebaseAuthService;
import com.google.android.material.snackbar.Snackbar;

/**
 * Login Activity for user authentication
 */
public class LoginActivity extends AppCompatActivity implements FirebaseAuthService.AuthCallback {
    
    private ActivityLoginBinding binding;
    private FirebaseAuthService authService;
    private boolean isPasswordVisible = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("LoginActivity", "onCreate: Initializing LoginActivity");
        
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Log.d("LoginActivity", "onCreate: Layout inflated and set");
        
        // Initialize Firebase Auth Service
        authService = new FirebaseAuthService();
        authService.setAuthCallback(this);
        
        // Check if user is already signed in
        if (authService.isUserSignedIn()) {
            Log.d("LoginActivity", "onCreate: User already signed in, navigating to MainActivity");
            navigateToMain();
            return;
        }
        
        Log.d("LoginActivity", "onCreate: Setting up views");
        setupViews();
        Log.d("LoginActivity", "onCreate: LoginActivity setup complete");
    }
    
    /**
     * Setup UI components
     */
    private void setupViews() {
        // Login button click listener
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        
        // Register button click listener
        binding.btnRegister.setOnClickListener(v -> showRegisterDialog());
        
        // Forgot password click listener
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        
        // Toggle password visibility
        binding.tilPassword.setEndIconOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.etPassword.setTransformationMethod(null);
                binding.tilPassword.setEndIconDrawable(R.drawable.ic_lock);
            } else {
                binding.etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.tilPassword.setEndIconDrawable(R.drawable.ic_lock);
            }
        });
    }
    
    /**
     * Attempt to login with email and password
     */
    private void attemptLogin() {
        // Reset errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        
        // Get values from input fields
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        boolean cancel = false;
        View focusView = null;
        
        // Check for valid email
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            focusView = binding.etEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            binding.tilEmail.setError("Invalid email format");
            focusView = binding.etEmail;
            cancel = true;
        }
        
        // Check for valid password
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            focusView = binding.etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            focusView = binding.etPassword;
            cancel = true;
        }
        
        if (cancel) {
            // There was an error; focus the first form field with an error
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // Show progress and attempt login
            showProgress(true);
            authService.signInWithEmailAndPassword(email, password);
        }
    }
    
    /**
     * Validate email format
     */
    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Show/hide progress indicator
     */
    private void showProgress(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.btnRegister.setEnabled(!show);
    }
    
    /**
     * Navigate to sign up activity
     */
    private void showRegisterDialog() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }
    
    /**
     * Show forgot password dialog
     */
    private void showForgotPasswordDialog() {
        // TODO: Implement forgot password dialog
        Snackbar.make(binding.getRoot(), "Forgot password functionality coming soon!", Snackbar.LENGTH_SHORT).show();
    }
    
    /**
     * Navigate to main activity
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    // ==================== AUTH CALLBACKS ====================
    
    @Override
    public void onAuthSuccess(User user) {
        showProgress(false);
        Toast.makeText(this, "Welcome back, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }
    
    @Override
    public void onAuthFailure(String error) {
        showProgress(false);
        Snackbar.make(binding.getRoot(), "Login failed: " + error, Snackbar.LENGTH_LONG).show();
    }
    
    @Override
    public void onUserCreated(User user) {
        // This won't be called from login activity
    }
    
    @Override
    public void onUserFetched(User user) {
        // This won't be called from login activity
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 