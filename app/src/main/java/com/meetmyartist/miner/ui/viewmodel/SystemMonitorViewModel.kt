package com.meetmyartist.miner.ui.viewmodel

import android.app.Application
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.model.MiningStats
import com.meetmyartist.miner.mining.MiningEngine
import com.meetmyartist.miner.monitoring.BatteryHealthMonitor
import com.meetmyartist.miner.utils.SystemInfo
import com.meetmyartist.miner.utils.SystemInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemMonitorViewModel @Inject constructor(
    private val batteryHealthMonitor: BatteryHealthMonitor,
    private val miningEngine: MiningEngine,
    application: Application
) : AndroidViewModel(application) {

    private val _batteryHealth = MutableStateFlow<BatteryHealthMonitor.BatteryHealth?>(null)
    val batteryHealth: StateFlow<BatteryHealthMonitor.BatteryHealth?> = _batteryHealth.asStateFlow()

    val miningStats: StateFlow<MiningStats> = miningEngine.miningStats

    private val _systemInfo = MutableStateFlow(SystemInfoProvider.getSystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            batteryHealthMonitor.monitorBatteryHealth()
                .collect { health ->
                    _batteryHealth.value = health
                }
        }
    }

    fun getBatteryHealthDescription(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    fun shouldReducePerformance(): Boolean {
        val health = _batteryHealth.value ?: return false
        return health.temperature > 40f || health.level < 20 && !health.isCharging
    }

    fun getPerformanceRecommendation(): String? {
        val health = _batteryHealth.value ?: return null
        val stats = miningStats.value

        return when {
            health.temperature > 45f -> "Critical: Stop mining to cool down device"
            health.temperature > 40f -> "Warning: Reduce thread count or CPU usage"
            stats.cpuTemp > 80f -> "CPU overheating - reduce mining intensity"
            !health.isCharging && health.level < 20 -> "Low battery - connect charger"
            stats.cpuUsage < 50f && stats.hashrate > 0 -> "Safe to increase performance"
            else -> null
        }
    }
}
