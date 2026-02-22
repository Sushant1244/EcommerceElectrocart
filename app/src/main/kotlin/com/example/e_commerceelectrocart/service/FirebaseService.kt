package com.example.e_commerceelectrocart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.e_commerceelectrocart.DashboardActivity
import com.example.e_commerceelectrocart.R
import com.example.e_commerceelectrocart.firebase.FirebaseManager
import com.example.e_commerceelectrocart.firestore.AppNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service for handling push notifications
 */
@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseService"
        
        // Notification channel IDs
        const val CHANNEL_ORDER = "order_notifications"
        const val CHANNEL_PROMO = "promo_notifications"
        const val CHANNEL_GENERAL = "general_notifications"
    }

    @Inject
    lateinit var firebaseManager: FirebaseManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")

        // Store token for the current user
        firebaseManager.currentUserId?.let { userId ->
            serviceScope.launch {
                firebaseManager.db.collection("users")
                    .document(userId)
                    .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                    .addOnSuccessListener { Log.d(TAG, "FCM token stored successfully") }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to store FCM token", e) }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message notification body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Electrocart",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // Check if message contains notification payload with image
        remoteMessage.notification?.let { notification ->
            val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            if (imageUrl != null) {
                showNotificationWithImage(
                    title = notification.title ?: "Electrocart",
                    body = notification.body ?: "",
                    imageUrl = imageUrl,
                    data = remoteMessage.data
                )
            }
        }
    }

    /**
     * Handle data-only messages
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: "general"
        
        when (type) {
            "order_status" -> {
                val orderId = data["orderId"] ?: ""
                val status = data["status"] ?: ""
                val title = "Order Update"
                val body = "Your order status has been updated to: $status"
                showNotification(title, body, data, CHANNEL_ORDER)
                
                // Log analytics
                firebaseManager.logEvent("notification_received", mapOf("type" to "order_status"))
            }
            
            "promo" -> {
                val title = data["title"] ?: "Special Offer"
                val body = data["body"] ?: "Check out our latest deals!"
                showNotification(title, body, data, CHANNEL_PROMO)
                
                firebaseManager.logEvent("notification_received", mapOf("type" to "promo"))
            }
            
            "product" -> {
                val title = data["title"] ?: "Product Update"
                val body = data["body"] ?: "A product you're interested in is now available!"
                showNotification(title, body, data, CHANNEL_GENERAL)
            }
            
            "chat" -> {
                val senderName = data["senderName"] ?: "Someone"
                val message = data["message"] ?: "You have a new message"
                showNotification(senderName, message, data, CHANNEL_GENERAL)
            }
            
            else -> {
                val title = data["title"] ?: "Electrocart"
                val body = data["body"] ?: ""
                showNotification(title, body, data, CHANNEL_GENERAL)
            }
        }
    }

    /**
     * Show basic notification
     */
    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        channelId: String = CHANNEL_GENERAL
    ) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Show notification with image
     */
    private fun showNotificationWithImage(
        title: String,
        body: String,
        imageUrl: String,
        data: Map<String, String> = emptyMap(),
        channelId: String = CHANNEL_PROMO
    ) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Load image using coil
        val style = NotificationCompat.BigPictureStyle()
            .bigPicture(null as android.graphics.Bitmap?) // Would load from URL in production
            .setBigContentTitle(title)
            .setSummaryText(body)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(style)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Order notifications channel
            val orderChannel = NotificationChannel(
                CHANNEL_ORDER,
                "Order Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about order status updates"
                enableVibration(true)
            }

            // Promo notifications channel
            val promoChannel = NotificationChannel(
                CHANNEL_PROMO,
                "Promotions & Offers",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Special offers and promotional notifications"
            }

            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(orderChannel, promoChannel, generalChannel)
            )
        }
    }
}
