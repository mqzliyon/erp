package com.dazzling.erp.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dazzling.erp.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firebase Authentication Service for handling user authentication and management
 */
public class FirebaseAuthService {
    private static final String TAG = "FirebaseAuthService";
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private AuthCallback mAuthCallback;
    
    public interface AuthCallback {
        void onAuthSuccess(User user);
        void onAuthFailure(String error);
        void onUserCreated(User user);
        void onUserFetched(User user);
    }
    
    public FirebaseAuthService() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
    }
    
    public void setAuthCallback(AuthCallback callback) {
        this.mAuthCallback = callback;
    }
    
    /**
     * Sign in with email and password
     */
    public void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                fetchUserData(firebaseUser.getUid());
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            if (mAuthCallback != null) {
                                mAuthCallback.onAuthFailure("Authentication failed: " + 
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            }
                        }
                    }
                });
    }
    
    /**
     * Create new user account
     */
    public void createUserWithEmailAndPassword(String email, String password, String displayName, 
                                              String role, String department) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                User user = new User(firebaseUser.getUid(), email, displayName, role, department);
                                saveUserToFirestore(user);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            if (mAuthCallback != null) {
                                mAuthCallback.onAuthFailure("Account creation failed: " + 
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            }
                        }
                    }
                });
    }
    
    /**
     * Save user data to Firestore
     */
    private void saveUserToFirestore(User user) {
        Log.d(TAG, "Saving user to Firestore: " + user.getEmail());
        
        mFirestore.collection("users").document(user.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User document created successfully");
                        if (mAuthCallback != null) {
                            mAuthCallback.onUserCreated(user);
                            mAuthCallback.onAuthSuccess(user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error creating user document", e);
                        Log.e(TAG, "Save user error details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        
                        // If it's a permission error, we can still proceed with auth success
                        // since the user is authenticated, just not saved to Firestore
                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            Log.w(TAG, "Permission denied for saving user, but authentication successful");
                            if (mAuthCallback != null) {
                                mAuthCallback.onAuthSuccess(user);
                            }
                        } else {
                            if (mAuthCallback != null) {
                                mAuthCallback.onAuthFailure("Failed to save user data: " + e.getMessage());
                            }
                        }
                    }
                });
    }
    
    /**
     * Fetch user data from Firestore
     */
    public void fetchUserData(String uid) {
        Log.d(TAG, "Fetching user data for UID: " + uid);
        
        mFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.d(TAG, "Document exists: " + documentSnapshot.exists());
                        
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                Log.d(TAG, "User data fetched successfully: " + user.getEmail());
                                if (mAuthCallback != null) {
                                    mAuthCallback.onUserFetched(user);
                                    mAuthCallback.onAuthSuccess(user);
                                }
                            } else {
                                Log.w(TAG, "Failed to convert document to User object");
                                // Create a basic user object from Firebase Auth data
                                createBasicUserFromAuth(uid);
                            }
                        } else {
                            Log.w(TAG, "User document does not exist, creating new user document");
                            // Create a basic user object from Firebase Auth data
                            createBasicUserFromAuth(uid);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error fetching user data", e);
                        Log.e(TAG, "Error details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        
                        // If it's a permission error, try to create the user document
                        if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                            Log.d(TAG, "Permission denied, attempting to create user document");
                            createBasicUserFromAuth(uid);
                        } else {
                            if (mAuthCallback != null) {
                                mAuthCallback.onAuthFailure("Failed to fetch user data: " + e.getMessage());
                            }
                        }
                    }
                });
    }
    
    /**
     * Create a basic user object from Firebase Auth data when Firestore document doesn't exist
     */
    private void createBasicUserFromAuth(String uid) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            String displayName = firebaseUser.getDisplayName();
            
            // Create a basic user with default values
            User user = new User(
                uid,
                email != null ? email : "unknown@email.com",
                displayName != null ? displayName : "User",
                "operator", // default role
                "General"   // default department
            );
            
            Log.d(TAG, "Creating basic user document for: " + user.getEmail());
            saveUserToFirestore(user);
        } else {
            Log.e(TAG, "FirebaseUser is null when trying to create basic user");
            if (mAuthCallback != null) {
                mAuthCallback.onAuthFailure("Unable to create user profile");
            }
        }
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        mAuth.signOut();
        Log.d(TAG, "User signed out");
    }
    
    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Get current user email
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * Reset password
     */
    public void resetPassword(String email, OnCompleteListener<Void> listener) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }
    
    /**
     * Test Firebase connection and permissions
     */
    public void testFirebaseConnection() {
        Log.d(TAG, "Testing Firebase connection...");
        
        // Test if we can access Firestore
        mFirestore.collection("test").document("test")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firebase connection test successful");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase connection test failed: " + e.getMessage());
                });
    }
} 