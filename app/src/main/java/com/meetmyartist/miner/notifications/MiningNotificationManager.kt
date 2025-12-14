package com.meetmyartist.miner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.meetmyartist.miner.MainActivity
import com.meetmyartist.miner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiningNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ALERTS_ID = "mining_alerts"
        private const val CHANNEL_STATUS_ID = "mining_status"
        private const val NOTIFICATION_ID_OVERHEAT = 1001
        private const val NOTIFICATION_ID_PAYOUT = 1002
        private const val NOTIFICATION_ID_ERROR = 1003
        private const val NOTIFICATION_ID_MILESTONE = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Mining Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important mining alerts (overheating, errors, payouts)"
                enableVibration(true)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "Mining Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mining milestones and achievements"
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(statusChannel)
        }
    }

    fun notifyOverheating(currentTemp: Float, maxTemp: Float) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Device Overheating")
            .setContentText("Temperature: ${currentTemp}°C (Max: ${maxTemp}°C). Mining paused.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OVERHEAT, notification)
    }

    fun notifyPayout(amount: Double, cryptocurrency: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Mining Payout Received")
            .setContentText("${"%.6f".format(amount)} $cryptocurrency deposited to your wallet")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PAYOUT, notification)
    }

    fun notifyMiningError(errorMessage: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("❌ Mining Error")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    fun notifyMilestone(title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MILESTONE, notification)
    }

    fun notifyHashrateMilestone(hashrate: Double) {
        notifyMilestone(
            "🚀 Hashrate Milestone",
            "Achieved ${hashrate.toInt()} H/s!"
        )
    }

    fun notifyUptimeMilestone(hours: Int) {
        notifyMilestone(
            "⏱️ Mining Marathon",
            "Mining continuously for $hours hours!"
        )
    }
}
