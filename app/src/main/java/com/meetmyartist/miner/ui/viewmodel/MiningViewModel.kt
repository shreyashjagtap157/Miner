package com.meetmyartist.miner.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.local.MiningConfigDao
import com.meetmyartist.miner.data.model.EnergyMode
import com.meetmyartist.miner.data.model.ExportData
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.ResourceConfig
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.export.StatisticsExporter
import com.meetmyartist.miner.gamification.Achievement
import com.meetmyartist.miner.gamification.AchievementManager
import com.meetmyartist.miner.mining.DynamicResourceController
import com.meetmyartist.miner.mining.MiningEngine
import com.meetmyartist.miner.notifications.MiningNotificationManager
import com.meetmyartist.miner.widget.MiningWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val miningEngine: MiningEngine,
    private val preferencesManager: PreferencesManager,
    private val miningConfigDao: MiningConfigDao,
    private val achievementManager: AchievementManager,
    private val notificationManager: MiningNotificationManager,
    private val statisticsExporter: StatisticsExporter,
    private val resourceController: DynamicResourceController
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val exportHistory = mutableListOf<ExportData>()
    private var lastHashrateMilestone = 0.0
    private var lastUptimeMilestone = 0L

    init {
        viewModelScope.launch {
            miningEngine.miningStats.collect { stats ->
                // Achievements
                if (stats.uptime >= 3600) {
                    achievementManager.unlockAchievement(Achievement.HOUR_OF_POWER)
                }
                if (stats.uptime >= 8 * 3600) {
                    achievementManager.unlockAchievement(Achievement.NIGHT_OWL)
                }
                
                // Notifications
                val notificationsEnabled = preferencesManager.notificationEnabled.first()
                if (notificationsEnabled) {
                    // Overheating alert
                    val maxTemp = preferencesManager.maxTemperature.first()
                    if (stats.cpuTemp > maxTemp) {
                        notificationManager.notifyOverheating(stats.cpuTemp, maxTemp)
                    }
                    
                    // Hashrate milestones (every 1000 H/s)
                    val hashrateThreshold = (stats.hashrate / 1000).toInt() * 1000.0
                    if (hashrateThreshold > lastHashrateMilestone && hashrateThreshold >= 1000) {
                        notificationManager.notifyHashrateMilestone(hashrateThreshold)
                        lastHashrateMilestone = hashrateThreshold
                    }
                    
                    // Uptime milestones (every hour)
                    val uptimeHours = stats.uptime / 3600
                    if (uptimeHours > lastUptimeMilestone && uptimeHours > 0) {
                        notificationManager.notifyUptimeMilestone(uptimeHours.toInt())
                        lastUptimeMilestone = uptimeHours
                    }
                }
                
                // Update widget
                MiningWidgetProvider.sendUpdateBroadcast(
                    context,
                    stats.hashrate,
                    stats.cpuTemp,
                    miningEngine.miningState.value == MiningEngine.MiningState.MINING,
                    stats.uptime
                )
                
                // Collect export data (keep last 1000 records)
                exportHistory.add(statisticsExporter.createExportData(stats))
                if (exportHistory.size > 1000) {
                    exportHistory.removeAt(0)
                }
            }
        }
    }
    
    val miningState = miningEngine.miningState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MiningEngine.MiningState.STOPPED
    )
    
    val miningStats = miningEngine.miningStats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.meetmyartist.miner.data.model.MiningStats()
    )
    
    val activeConfig: StateFlow<MiningConfig?> = miningConfigDao.getActiveConfig().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
    
    val selectedThreads = preferencesManager.selectedThreads.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Runtime.getRuntime().availableProcessors() / 2
    )
    
    val cpuUsageLimit = preferencesManager.cpuUsageLimit.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        70
    )
    
    val maxTemperature = preferencesManager.maxTemperature.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        80f
    )
    
    val thermalThrottleEnabled = preferencesManager.thermalThrottleEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    
    val batteryOptimizationEnabled = preferencesManager.batteryOptimizationEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    
    val biometricEnabled = preferencesManager.biometricEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    
    val themeMode = preferencesManager.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "system"
    )
    
    fun updateSelectedThreads(threads: Int) {
        viewModelScope.launch {
            preferencesManager.updateSelectedThreads(threads)
        }
    }
    
    fun updateCpuUsageLimit(limit: Int) {
        viewModelScope.launch {
            preferencesManager.updateCpuUsageLimit(limit)
        }
    }
    
    fun updateMaxTemperature(temp: Float) {
        viewModelScope.launch {
            preferencesManager.updateMaxTemperature(temp)
        }
    }
    
    fun updateThermalThrottle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateThermalThrottle(enabled)
        }
    }
    
    fun updateBatteryOptimization(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateBatteryOptimization(enabled)
        }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateBiometricEnabled(enabled)
        }
    }
    
    fun applyEnergyMode(mode: EnergyMode) {
        viewModelScope.launch {
            preferencesManager.applyEnergyModePreset(mode)
        }
    }
    
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(mode)
        }
    }
    
    suspend fun exportStatisticsCSV(): Result<Uri> {
        return statisticsExporter.exportToCSV(exportHistory)
    }
    
    suspend fun exportStatisticsJSON(): Result<Uri> {
        return statisticsExporter.exportToJSON(exportHistory)
    }
    
    fun notifyError(message: String) {
        viewModelScope.launch {
            val enabled = preferencesManager.notificationEnabled.first()
            if (enabled) {
                notificationManager.notifyMiningError(message)
            }
        }
    }
    
    fun startMining(config: MiningConfig) {
        viewModelScope.launch {
            val resourceConfig = ResourceConfig(
                selectedThreads = selectedThreads.value,
                cpuUsageLimit = cpuUsageLimit.value,
                maxTemperature = maxTemperature.value,
                enableThermalThrottle = thermalThrottleEnabled.value,
                enableBatteryOptimization = batteryOptimizationEnabled.value
            )
            
            // Set this config as active
            miningConfigDao.deactivateAllConfigs()
            miningConfigDao.updateConfig(config.copy(isActive = true))
            
            // Apply resource controller settings
            resourceController.updateThreadCount(selectedThreads.value)
            if (resourceConfig.maxHashrate != null && resourceConfig.maxHashrate > 0) {
                resourceController.setHashrateLimit(resourceConfig.maxHashrate)
            }
            
            miningEngine.startMining(config, resourceConfig)
            achievementManager.unlockAchievement(Achievement.FIRST_SESSION)
            
            // Reset milestones for new session
            lastHashrateMilestone = 0.0
            lastUptimeMilestone = 0L
        }
    }
    
    fun stopMining() {
        miningEngine.stopMining()
        // Stop resource controller
        resourceController.stopResourceManagement()
    }
    
    fun pauseMining() {
        miningEngine.pauseMining()
    }
    
    fun resumeMining() {
        miningEngine.resumeMining()
    }
    
    fun startMining(useRealMining: Boolean = false) {
        viewModelScope.launch {
            val config = activeConfig.value
            if (config != null) {
                val resourceConfig = ResourceConfig(
                    selectedThreads = selectedThreads.value,
                    cpuUsageLimit = cpuUsageLimit.value,
                    maxTemperature = maxTemperature.value,
                    enableThermalThrottle = thermalThrottleEnabled.value,
                    enableBatteryOptimization = batteryOptimizationEnabled.value
                )
                
                // Apply resource controller settings
                resourceController.updateThreadCount(selectedThreads.value)
                if (resourceConfig.maxHashrate != null && resourceConfig.maxHashrate > 0) {
                    resourceController.setHashrateLimit(resourceConfig.maxHashrate)
                }
                
                miningEngine.startMining(config, resourceConfig, useRealMining)
                achievementManager.unlockAchievement(Achievement.FIRST_SESSION)
            }
        }
    }
    
    fun refreshStats() {
        // Trigger a refresh of mining stats
        // In a real implementation, this might update charts data from database
        viewModelScope.launch {
            _isRefreshing.value = true
            // Simulate a network delay
            kotlinx.coroutines.delay(1500)
            // Here you would typically re-fetch data from your repository
            // For now, we just turn off the refreshing indicator
            _isRefreshing.value = false
        }
    }
}
