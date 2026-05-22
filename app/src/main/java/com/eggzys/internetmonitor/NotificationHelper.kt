package com.eggzys.internetmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val TAG = "NotificationHelper"
        const val CHANNEL_SERVICE = "monitor_service"
        const val CHANNEL_ALERTS = "state_alerts"
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_ALERT = 1002
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Network Monitor",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Persistent monitoring service notification"
            setShowBadge(true)
            enableVibration(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "State Change Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when network state changes"
            enableVibration(true)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
        Log.d(TAG, "Notification channels created")
    }

    fun buildServiceNotification(state: InternetState): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "Building notification for state: ${state.displayName}")

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle("${state.emoji} ${state.displayName}")
            .setContentText(state.notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(state.notificationText))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun sendStateAlert(newState: InternetState) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertText = when (newState) {
            InternetState.FULL_ACCESS -> "Network restored. All nodes operational."
            InternetState.RUSSIA_ONLY -> "Global nodes unreachable. RU network only."
            InternetState.WHITELIST_ONLY -> "RKN lockdown active. Heavy restrictions."
            InternetState.NO_INTERNET -> "Signal lost. No connectivity."
            InternetState.UNKNOWN -> "State changed to unknown."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle("${newState.emoji} ${newState.displayName}")
            .setContentText(alertText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_ALERT, notification)
        Log.d(TAG, "Alert notification sent: ${newState.displayName}")
    }
}
