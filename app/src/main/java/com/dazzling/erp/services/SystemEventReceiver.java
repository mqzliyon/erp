package com.dazzling.erp.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * System event receiver to handle boot completion and package replacement events
 * Helps prevent crashes related to system events
 */
public class SystemEventReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SystemEventReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            Log.d(TAG, "Received system event: " + action);
            
            if (action != null) {
                switch (action) {
                    case Intent.ACTION_BOOT_COMPLETED:
                        handleBootCompleted(context);
                        break;
                    case Intent.ACTION_MY_PACKAGE_REPLACED:
                        handlePackageReplaced(context);
                        break;
                    default:
                        Log.d(TAG, "Unhandled system event: " + action);
                        break;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling system event", e);
        }
    }
    
    /**
     * Handle boot completion event
     */
    private void handleBootCompleted(Context context) {
        try {
            Log.d(TAG, "Boot completed - initializing app components");
            
            // Initialize any necessary components after boot
            // This can include starting background services if needed
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling boot completed", e);
        }
    }
    
    /**
     * Handle package replacement event
     */
    private void handlePackageReplaced(Context context) {
        try {
            Log.d(TAG, "Package replaced - updating app components");
            
            // Handle any necessary updates after package replacement
            // This can include updating configurations or restarting services
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling package replaced", e);
        }
    }
} 