package com.dazzling.erp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dazzling.erp.MainActivity;
import com.dazzling.erp.R;
import com.dazzling.erp.models.User;
import com.dazzling.erp.services.FirebaseAuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity implements FirebaseAuthService.AuthCallback {
    
    private static final String TAG = "SignUpActivity";
    
    private TextInputEditText emailInput, passwordInput, confirmPasswordInput, fullNameInput;
    private AutoCompleteTextView roleDropdown;
    private MaterialButton signUpButton;
    private FirebaseAuthService authService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        // Initialize Firebase Auth Service
        authService = new FirebaseAuthService();
        authService.setAuthCallback(this);
        
        // Initialize views
        initViews();
        setupRoleDropdown();
        setupClickListeners();
    }
    
    private void initViews() {
        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        roleDropdown = findViewById(R.id.role_dropdown);
        signUpButton = findViewById(R.id.signup_button);
    }
    
    private void setupRoleDropdown() {
        String[] roles = {"CEO", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, roles);
        roleDropdown.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> handleSignUp());
        
        findViewById(R.id.login_link).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void handleSignUp() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String selectedRole = roleDropdown.getText().toString().trim();

        // Validation
        if (!validateInputs(fullName, email, password, confirmPassword, selectedRole)) {
            return;
        }

        // Show loading
        signUpButton.setEnabled(false);
        signUpButton.setText("Creating Account...");

        // Create user with role and display name
        authService.createUserWithEmailAndPassword(email, password, fullName, selectedRole, "General");
    }
    
    private boolean validateInputs(String fullName, String email, String password, String confirmPassword, String role) {
        // Full name validation
        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full name is required");
            return false;
        }
        // Email validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            return false;
        }
        // Password validation
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }
        // Confirm password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm your password");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }
        // Role validation
        if (TextUtils.isEmpty(role) || (!role.equals("CEO") && !role.equals("Manager"))) {
            roleDropdown.setError("Please select a valid role");
            return false;
        }
        return true;
    }
    
    @Override
    public void onAuthSuccess(User user) {
        Log.d(TAG, "User created successfully: " + user.getEmail());
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        
        // Navigate to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onAuthFailure(String error) {
        Log.e(TAG, "Auth failure: " + error);
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        // Reset button
        signUpButton.setEnabled(true);
        signUpButton.setText("Create Account");
        // Clear password fields for security
        passwordInput.setText("");
        confirmPasswordInput.setText("");
    }
    
    @Override
    public void onUserCreated(User user) {
        // This will be called after user is created in Firestore
        Log.d(TAG, "User created in Firestore: " + user.getEmail());
    }
    
    @Override
    public void onUserFetched(User user) {
        // Not used in signup
    }
} 