package com.dazzling.erp;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.dazzling.erp.utils.DeviceUtils;

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
            // Apply device-specific fixes first
            DeviceUtils.applyDeviceSpecificFixes(this);
            
            // Initialize Firebase
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
            
            // Initialize other app components here
            initializeAppComponents();
            
            // Initialize Firebase Crashlytics
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        
        // Set up global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
                
                // Log the exception details for debugging
                Log.e(TAG, "Exception type: " + throwable.getClass().getSimpleName());
                Log.e(TAG, "Exception message: " + throwable.getMessage());
                
                // Print stack trace for debugging
                throwable.printStackTrace();
                
                // Report to Firebase Crashlytics
                try {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to report to Crashlytics", e);
                }
                
                // Exit the app gracefully
                System.exit(1);
            }
        });
        
        Log.d(TAG, "ERP Application initialized with crash protection");
        Log.d(TAG, DeviceUtils.getDeviceInfo());
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