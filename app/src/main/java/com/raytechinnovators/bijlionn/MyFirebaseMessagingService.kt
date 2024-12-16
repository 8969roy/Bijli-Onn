package com.raytechinnovators.bijlionn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "your_channel_id"
        private const val TAG = "com.raytechinnovators.bijlionn.MyFirebaseMessagingService"
        private val displayedNotificationIds = mutableSetOf<Int>()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract data from the message
        val senderName = remoteMessage.data["senderName"]
        val messageContent = remoteMessage.data["messageContent"]
        val senderId = remoteMessage.data["senderId"]
        val profileImageUrl = remoteMessage.data["profileImageUrl"]
        val location = remoteMessage.data["senderLocation"]

        // Determine the message type based on the content of the message
        val messageType = if (messageContent == "You have new News") "news" else "chat"

        // Generate a unique notification ID
        val uniqueNotificationId = if (messageType == "news") {
            (senderId + System.currentTimeMillis().toString()).hashCode()
        } else {
            (senderId + messageContent).hashCode()
        }

        // Show notification only if it hasn't been displayed already for chats, always show for news
        if (senderId != null && (messageType == "news" || !displayedNotificationIds.contains(uniqueNotificationId))) {
            if (messageType == "chat") {
                displayedNotificationIds.add(uniqueNotificationId)
            }
            showNotification(senderName, messageContent, senderId, location, profileImageUrl, messageType, uniqueNotificationId)
        }
    }

    private fun showNotification(
        senderName: String?,
        messageContent: String?,
        senderId: String,
        location: String?,
        profileImageUrl: String?,
        messageType: String?,
        uniqueNotificationId: Int
    ) {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Determine the activity to open based on message type
        val intent = if (messageType == "chat") {
            Intent(this, Chat::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SelectedUserId", senderId)
            putExtra("senderName", senderName)
            putExtra("senderLocation", location)
            putExtra("profileImageUrl", profileImageUrl)
        }

        // Set the activity to be launched when the notification is clicked
        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueNotificationId, // Use the uniqueNotificationId
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Channel Name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification with a unique ID
        notificationManager.notify(uniqueNotificationId, notificationBuilder.build())
    }
}
