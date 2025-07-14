package com.dazzling.erp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.dazzling.erp.MainActivity;
import com.dazzling.erp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "erp_notifications";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String type = null;
        if (remoteMessage.getData() != null) {
            type = remoteMessage.getData().get("type");
        }
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "ERP Notification";
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "You have a new notification.";
        String navigateTo = null;
        if ("rongdhonu_office".equals(type)) {
            navigateTo = "rongdhonu_office";
        } else if ("uttara_office".equals(type)) {
            navigateTo = "uttara_office";
        } else if ("payment_request".equals(type)) {
            navigateTo = "payment_request";
        }
        sendNotification(title, body, navigateTo);
    }

    private void sendNotification(String title, String messageBody, String navigateTo) {
        Intent intent = new Intent(this, MainActivity.class);
        if (navigateTo != null) {
            intent.putExtra("navigate_to", navigateTo);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "ERP Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
} 