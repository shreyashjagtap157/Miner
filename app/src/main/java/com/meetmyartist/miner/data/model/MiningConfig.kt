package com.meetmyartist.miner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mining_configs")
data class MiningConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cryptocurrency: String,
    val algorithm: String,
    val poolUrl: String,
    val poolPort: Int,
    val walletAddress: String,
    val workerName: String,
    val threadCount: Int,
    val cpuUsagePercent: Int,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Failover pools
    val failoverPoolUrl: String? = null,
    val failoverPoolPort: Int? = null,
    val failoverEnabled: Boolean = false
)

data class MiningStats(
    val hashrate: Double = 0.0, // Hashes per second
    val acceptedShares: Long = 0,
    val rejectedShares: Long = 0,
    val totalHashes: Long = 0,
    val uptime: Long = 0, // in seconds
    val cpuTemp: Float = 0f,
    val cpuUsage: Float = 0f,
    val powerUsage: Float = 0f, // in watts (estimated)
    val earnings: Double = 0.0 // in cryptocurrency units
)

data class ResourceConfig(
    val maxThreads: Int = Runtime.getRuntime().availableProcessors(),
    val selectedThreads: Int = Runtime.getRuntime().availableProcessors() / 2,
    val cpuUsageLimit: Int = 70, // percentage
    val maxTemperature: Float = 80f, // Celsius
    val enableThermalThrottle: Boolean = true,
    val enableBatteryOptimization: Boolean = true,
    val pauseOnLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    // Advanced per-core control
    val enablePerCoreControl: Boolean = false,
    val coreAffinityMap: Map<Int, CoreConfig> = emptyMap(),
    val maxHashrate: Double? = null, // Maximum hashrate limit in H/s
    val enableHashrateLimit: Boolean = false
)

data class CoreConfig(
    val coreId: Int,
    val enabled: Boolean = true,
    val usagePercent: Int = 100, // Individual core usage limit
    val priority: CorePriority = CorePriority.NORMAL
)

enum class CorePriority(val niceValue: Int) {
    LOWEST(19),
    LOW(10),
    NORMAL(0),
    HIGH(-5),
    HIGHEST(-10)
}

enum class EnergyMode(
    val threads: (max: Int) -> Int,
    val cpuLimit: Int,
    val maxTemp: Float,
    val displayName: String
) {
    ECO(
        threads = { max -> (max * 0.25).toInt().coerceAtLeast(1) },
        cpuLimit = 50,
        maxTemp = 70f,
        displayName = "Eco Mode"
    ),
    BALANCED(
        threads = { max -> (max * 0.5).toInt().coerceAtLeast(1) },
        cpuLimit = 70,
        maxTemp = 80f,
        displayName = "Balanced"
    ),
    PERFORMANCE(
        threads = { max -> max },
        cpuLimit = 90,
        maxTemp = 85f,
        displayName = "Performance"
    )
}

data class ExportData(
    val timestamp: Long,
    val hashrate: Double,
    val cpuTemp: Float,
    val cpuUsage: Float,
    val acceptedShares: Long,
    val rejectedShares: Long,
    val uptime: Long,
    val powerUsage: Float,
    val earnings: Double
)
