package com.dazzling.erp.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Utility class to handle device-specific issues and configurations
 */
public class DeviceUtils {
    
    private static final String TAG = "DeviceUtils";
    
    /**
     * Check if the device is a Transsion device (Tecno, Infinix, etc.)
     */
    public static boolean isTranssionDevice() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("transsion") || 
               manufacturer.contains("tecno") || 
               manufacturer.contains("infinix") ||
               manufacturer.contains("itel");
    }
    
    /**
     * Apply device-specific fixes
     */
    public static void applyDeviceSpecificFixes(Context context) {
        try {
            Log.d(TAG, "Applying device-specific fixes for: " + Build.MANUFACTURER + " " + Build.MODEL);
            
            if (isTranssionDevice()) {
                applyTranssionFixes();
            }
            
            // Apply general fixes for all devices
            applyGeneralFixes();
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying device-specific fixes", e);
        }
    }
    
    /**
     * Apply fixes specific to Transsion devices
     */
    private static void applyTranssionFixes() {
        try {
            Log.d(TAG, "Applying Transsion-specific fixes");
            
            // Disable problematic Transsion components
            System.setProperty("transsion.multidisplay.enabled", "false");
            System.setProperty("transsion.hubcore.enabled", "false");
            System.setProperty("transsion.display.enabled", "false");
            
            // Suppress Transsion-related logging
            System.setProperty("log.tag.TranClassInfo", "ERROR");
            System.setProperty("log.tag.TranActivityImpl", "ERROR");
            System.setProperty("log.tag.ITranMultiDisplayCoreComponent", "ERROR");
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying Transsion fixes", e);
        }
    }
    
    /**
     * Apply general fixes for all devices
     */
    private static void applyGeneralFixes() {
        try {
            // Suppress verbose system logging
            System.setProperty("log.tag.SurfaceFlinger", "ERROR");
            System.setProperty("log.tag.BufferQueueDebug", "ERROR");
            System.setProperty("log.tag.OpenGLRenderer", "ERROR");
            System.setProperty("log.tag.QT", "ERROR");
            System.setProperty("log.tag.InputMethodManager", "ERROR");
            
            // Handle QT file issues
            handleQTFileIssues();
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying general fixes", e);
        }
    }
    
    /**
     * Handle QT file related issues
     */
    private static void handleQTFileIssues() {
        try {
            // Create QT configuration directory if it doesn't exist
            String qtDir = "/data/data/com.dazzling.erp/qt";
            java.io.File qtDirectory = new java.io.File(qtDir);
            if (!qtDirectory.exists()) {
                boolean created = qtDirectory.mkdirs();
                Log.d(TAG, "Created QT directory: " + created);
            }
            
            // Create QT configuration file
            String qtConfigFile = qtDir + "/qt.conf";
            java.io.File qtConfig = new java.io.File(qtConfigFile);
            if (!qtConfig.exists()) {
                try (java.io.FileWriter writer = new java.io.FileWriter(qtConfig)) {
                    writer.write("# QT Configuration\n");
                    writer.write("[Paths]\n");
                    writer.write("Plugins=plugins\n");
                    writer.write("Imports=imports\n");
                    writer.write("Qml2Imports=qml\n");
                }
                Log.d(TAG, "Created QT configuration file");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling QT file issues", e);
        }
    }
    
    /**
     * Get device information for debugging
     */
    public static String getDeviceInfo() {
        return String.format("Device: %s %s, Android: %s (API %d)", 
            Build.MANUFACTURER, 
            Build.MODEL, 
            Build.VERSION.RELEASE, 
            Build.VERSION.SDK_INT);
    }
    
    /**
     * Check if device supports specific features
     */
    public static boolean supportsFeature(String feature) {
        try {
            return Build.VERSION.SDK_INT >= getFeatureMinSdk(feature);
        } catch (Exception e) {
            Log.e(TAG, "Error checking feature support", e);
            return false;
        }
    }
    
    /**
     * Get minimum SDK for a feature
     */
    private static int getFeatureMinSdk(String feature) {
        switch (feature) {
            case "multidisplay":
                return 26; // Android 8.0
            case "foreground_service":
                return 26; // Android 8.0
            default:
                return 24; // Android 7.0
        }
    }
} 