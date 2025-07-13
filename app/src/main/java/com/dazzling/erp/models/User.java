package com.dazzling.erp.models;

import com.google.firebase.firestore.DocumentId;

/**
 * User model for Firebase Authentication and role-based access control
 */
public class User {
    @DocumentId
    private String uid;
    private String email;
    private String displayName;
    private String role; // CEO, Manager
    private String department;
    private long createdAt;
    private boolean isActive;
    private boolean manager; // Added for Firestore compatibility
    private boolean admin;   // Added for Firestore compatibility

    // Required empty constructor for Firestore
    public User() {}

    public User(String uid, String email, String displayName, String role, String department) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.department = department;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.admin = "CEO".equals(role);
        this.manager = "Manager".equals(role) || "CEO".equals(role);
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { 
        this.role = role; 
        this.admin = "CEO".equals(role);
        this.manager = "Manager".equals(role) || "CEO".equals(role);
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Added getters and setters for Firestore compatibility
    public boolean isManager() { return manager; }
    public void setManager(boolean manager) { this.manager = manager; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    // Legacy methods for backward compatibility
    public boolean isAdminRole() {
        return "CEO".equals(role);
    }

    public boolean isManagerRole() {
        return "Manager".equals(role) || "CEO".equals(role);
    }
} 