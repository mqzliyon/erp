package com.dazzling.erp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

/**
 * Custom Application class for ERP app
 * Handles multidex initialization and Firebase setup
 */
public class ERPApplication extends MultiDexApplication {
    
    private static final String TAG = "ERPApplication";
    private static ERPApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
            
            // Initialize other app components here
            initializeAppComponents();
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
        }
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    
    /**
     * Initialize app components
     */
    private void initializeAppComponents() {
        try {
            // Initialize any additional components here
            Log.d(TAG, "App components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app components", e);
        }
    }
    
    /**
     * Get application instance
     */
    public static ERPApplication getInstance() {
        return instance;
    }
    
    /**
     * Get application context
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
} 