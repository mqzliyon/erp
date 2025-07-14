package com.dazzling.erp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dazzling.erp.R;

/**
 * Background service for handling background operations
 * Prevents the system from killing the app process unexpectedly
 */
public class BackgroundService extends Service {
    
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "ERP_Background_Service";
    private static final int NOTIFICATION_ID = 1001;
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.d(TAG, "BackgroundService created");
            createNotificationChannel();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.d(TAG, "BackgroundService started");
            // Create notification for foreground service
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("ERP System")
                    .setContentText("Running in background")
                    .setSmallIcon(R.drawable.ic_dashboard)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build();
            // Start foreground service
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
            stopSelf();
        }
        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BackgroundService destroyed");
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ERP Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for ERP operations");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 