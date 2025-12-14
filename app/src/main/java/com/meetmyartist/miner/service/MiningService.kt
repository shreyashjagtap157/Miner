package com.meetmyartist.miner.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meetmyartist.miner.MainActivity
import com.meetmyartist.miner.R
import com.meetmyartist.miner.data.local.MiningConfigDao
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.mining.MiningEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MiningService : Service() {
    
    @Inject
    lateinit var miningEngine: MiningEngine
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var miningConfigDao: MiningConfigDao
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var statsUpdateJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mining_service_channel"
        private const val CHANNEL_NAME = "Mining Service"
        
        const val ACTION_START_MINING = "com.meetmyartist.miner.START_MINING"
        const val ACTION_STOP_MINING = "com.meetmyartist.miner.STOP_MINING"
        const val ACTION_PAUSE_MINING = "com.meetmyartist.miner.PAUSE_MINING"
        const val ACTION_RESUME_MINING = "com.meetmyartist.miner.RESUME_MINING"
        
        fun startMiningService(context: Context) {
            val intent = Intent(context, MiningService::class.java).apply {
                action = ACTION_START_MINING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopMiningService(context: Context) {
            val intent = Intent(context, MiningService::class.java).apply {
                action = ACTION_STOP_MINING
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MINING -> {
                startForeground(NOTIFICATION_ID, createNotification("Starting mining..."))
                scope.launch {
                    startMining()
                }
            }
            ACTION_STOP_MINING -> {
                stopMining()
                stopSelf()
            }
            ACTION_PAUSE_MINING -> {
                miningEngine.pauseMining()
                updateNotification("Mining paused")
            }
            ACTION_RESUME_MINING -> {
                miningEngine.resumeMining()
                updateNotification("Mining resumed")
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopMining()
        scope.cancel()
    }
    
    private suspend fun startMining() {
        try {
            val activeConfig = miningConfigDao.getActiveConfig().first()
            if (activeConfig == null) {
                updateNotification("No active mining configuration")
                stopSelf()
                return
            }
            
            val selectedThreads = preferencesManager.selectedThreads.first()
            val cpuUsageLimit = preferencesManager.cpuUsageLimit.first()
            val maxTemp = preferencesManager.maxTemperature.first()
            val thermalThrottle = preferencesManager.thermalThrottleEnabled.first()
            val batteryOpt = preferencesManager.batteryOptimizationEnabled.first()
            val pauseOnLowBatt = preferencesManager.pauseOnLowBattery.first()
            val lowBattThreshold = preferencesManager.lowBatteryThreshold.first()
            
            val resourceConfig = com.meetmyartist.miner.data.model.ResourceConfig(
                selectedThreads = selectedThreads,
                cpuUsageLimit = cpuUsageLimit,
                maxTemperature = maxTemp,
                enableThermalThrottle = thermalThrottle,
                enableBatteryOptimization = batteryOpt,
                pauseOnLowBattery = pauseOnLowBatt,
                lowBatteryThreshold = lowBattThreshold
            )
            
            miningEngine.startMining(activeConfig, resourceConfig)
            
            // Start updating notification with stats
            statsUpdateJob = scope.launch {
                while (isActive) {
                    val stats = miningEngine.miningStats.first()
                    val state = miningEngine.miningState.first()
                    updateNotification(
                        "State: ${state.name}\n" +
                        "Hashrate: ${"%.2f".format(stats.hashrate)} H/s\n" +
                        "CPU: ${"%.1f".format(stats.cpuUsage)}% @ ${"%.1f".format(stats.cpuTemp)}°C"
                    )
                    delay(5000) // Update every 5 seconds
                }
            }
            
            preferencesManager.updateMiningActive(true)
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("Error: ${e.message}")
        }
    }
    
    private fun stopMining() {
        statsUpdateJob?.cancel()
        miningEngine.stopMining()
        scope.launch {
            preferencesManager.updateMiningActive(false)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Shows mining status and statistics"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, MiningService::class.java).apply {
            action = ACTION_STOP_MINING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crypto Miner")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
