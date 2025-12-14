package com.meetmyartist.miner.mining

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.meetmyartist.miner.data.model.MiningStats
import com.meetmyartist.miner.data.model.ResourceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent Performance Optimizer
 * 
 * Automatically adjusts mining parameters based on:
 * - Device temperature and thermal state
 * - Battery level and charging status
 * - CPU usage patterns
 * - Historical performance data
 * - Power efficiency calculations
 */
@Singleton
class PerformanceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val _optimizationState = MutableStateFlow(OptimizationState())
    val optimizationState: StateFlow<OptimizationState> = _optimizationState.asStateFlow()
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Historical data for trend analysis
    private val temperatureHistory = ArrayDeque<Float>(60) // Last 60 readings
    private val hashrateHistory = ArrayDeque<Double>(60)
    private val powerEfficiencyHistory = ArrayDeque<Float>(60)
    
    data class OptimizationState(
        val mode: OptimizationMode = OptimizationMode.BALANCED,
        val boostEnabled: Boolean = false,
        val thermalThrottleActive: Boolean = false,
        val batteryThrottleActive: Boolean = false,
        val recommendedThreads: Int = 0,
        val recommendedCpuLimit: Int = 70,
        val optimizationScore: Float = 0f, // 0-100 score
        val suggestions: List<String> = emptyList()
    )
    
    data class PerformanceMetrics(
        val averageHashrate: Double = 0.0,
        val peakHashrate: Double = 0.0,
        val hashesPerWatt: Double = 0.0, // Efficiency metric
        val averageTemperature: Float = 0f,
        val temperatureTrend: TrendDirection = TrendDirection.STABLE,
        val uptimeSeconds: Long = 0,
        val totalHashesLifetime: Long = 0,
        val estimatedEarningsPerHour: Double = 0.0
    )
    
    enum class OptimizationMode {
        ECO,          // Maximum efficiency, lower performance
        BALANCED,     // Balance between performance and efficiency
        PERFORMANCE,  // Maximum hashrate, higher power consumption
        AGGRESSIVE,   // Push limits, requires active cooling
        CUSTOM        // User-defined settings
    }
    
    enum class TrendDirection {
        RISING,
        STABLE,
        FALLING
    }
    
    /**
     * Analyze current conditions and provide optimized resource configuration
     */
    fun optimizeConfiguration(
        currentConfig: ResourceConfig,
        currentStats: MiningStats,
        maxThreads: Int
    ): ResourceConfig {
        updateHistoricalData(currentStats)
        
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        val thermalState = getThermalState()
        val cpuTemp = currentStats.cpuTemp
        
        // Calculate optimal thread count
        val optimalThreads = calculateOptimalThreads(
            currentThreads = currentConfig.selectedThreads,
            maxThreads = maxThreads,
            temperature = cpuTemp,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            thermalState = thermalState
        )
        
        // Calculate optimal CPU limit
        val optimalCpuLimit = calculateOptimalCpuLimit(
            currentLimit = currentConfig.cpuUsageLimit,
            currentUsage = currentStats.cpuUsage,
            temperature = cpuTemp,
            isCharging = isCharging
        )
        
        // Generate suggestions
        val suggestions = generateSuggestions(
            currentConfig = currentConfig,
            stats = currentStats,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            thermalState = thermalState
        )
        
        // Calculate optimization score
        val score = calculateOptimizationScore(
            currentStats = currentStats,
            config = currentConfig,
            batteryLevel = batteryLevel,
            isCharging = isCharging
        )
        
        // Update optimization state
        _optimizationState.value = OptimizationState(
            mode = determineOptimizationMode(currentConfig, maxThreads),
            boostEnabled = canBoostPerformance(cpuTemp, batteryLevel, isCharging),
            thermalThrottleActive = shouldThermalThrottle(cpuTemp, currentConfig.maxTemperature),
            batteryThrottleActive = shouldBatteryThrottle(batteryLevel, isCharging),
            recommendedThreads = optimalThreads,
            recommendedCpuLimit = optimalCpuLimit,
            optimizationScore = score,
            suggestions = suggestions
        )
        
        // Update performance metrics
        updatePerformanceMetrics(currentStats)
        
        // Return optimized configuration
        return currentConfig.copy(
            selectedThreads = optimalThreads,
            cpuUsageLimit = optimalCpuLimit
        )
    }
    
    /**
     * Apply performance boost when conditions are favorable
     */
    fun applyPerformanceBoost(
        currentConfig: ResourceConfig,
        maxThreads: Int,
        currentTemp: Float
    ): ResourceConfig {
        if (!canBoostPerformance(currentTemp, getBatteryLevel(), isDeviceCharging())) {
            return currentConfig
        }
        
        return currentConfig.copy(
            selectedThreads = min(currentConfig.selectedThreads + 2, maxThreads),
            cpuUsageLimit = min(currentConfig.cpuUsageLimit + 10, 100),
            enableThermalThrottle = true // Safety measure
        )
    }
    
    /**
     * Apply power-saving mode
     */
    fun applyPowerSavingMode(currentConfig: ResourceConfig): ResourceConfig {
        return currentConfig.copy(
            selectedThreads = max(currentConfig.selectedThreads - 2, 1),
            cpuUsageLimit = min(currentConfig.cpuUsageLimit, 50),
            enableBatteryOptimization = true,
            pauseOnLowBattery = true
        )
    }
    
    private fun calculateOptimalThreads(
        currentThreads: Int,
        maxThreads: Int,
        temperature: Float,
        batteryLevel: Int,
        isCharging: Boolean,
        thermalState: Int
    ): Int {
        var optimal = currentThreads
        
        // Thermal considerations
        when {
            temperature > 80f -> optimal = max(1, optimal - 2)
            temperature > 70f -> optimal = max(1, optimal - 1)
            temperature < 60f && isCharging -> optimal = min(maxThreads, optimal + 1)
        }
        
        // Battery considerations
        when {
            !isCharging && batteryLevel < 30 -> optimal = max(1, optimal / 2)
            !isCharging && batteryLevel < 50 -> optimal = max(1, (optimal * 0.75).toInt())
            isCharging && batteryLevel > 80 -> optimal = min(maxThreads, (optimal * 1.2).toInt())
        }
        
        // Thermal state considerations (Android thermal API)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (thermalState) {
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> optimal = 1
                PowerManager.THERMAL_STATUS_MODERATE -> optimal = max(1, optimal / 2)
            }
        }
        
        return optimal.coerceIn(1, maxThreads)
    }
    
    private fun calculateOptimalCpuLimit(
        currentLimit: Int,
        currentUsage: Float,
        temperature: Float,
        isCharging: Boolean
    ): Int {
        var optimal = currentLimit
        
        // If current usage is close to limit, check if we can increase
        if (currentUsage >= currentLimit * 0.9 && temperature < 65f && isCharging) {
            optimal = min(100, optimal + 10)
        }
        
        // If temperature is high, reduce limit
        when {
            temperature > 80f -> optimal = min(optimal, 40)
            temperature > 70f -> optimal = min(optimal, 60)
            temperature > 60f && !isCharging -> optimal = min(optimal, 70)
        }
        
        return optimal.coerceIn(10, 100)
    }
    
    private fun canBoostPerformance(
        temperature: Float,
        batteryLevel: Int,
        isCharging: Boolean
    ): Boolean {
        return temperature < 65f && 
               (isCharging || batteryLevel > 50) &&
               getThermalState() < PowerManager.THERMAL_STATUS_MODERATE
    }
    
    private fun shouldThermalThrottle(
        currentTemp: Float,
        maxTemp: Float
    ): Boolean {
        return currentTemp >= maxTemp * 0.9f // Start throttling at 90% of max
    }
    
    private fun shouldBatteryThrottle(
        batteryLevel: Int,
        isCharging: Boolean
    ): Boolean {
        return !isCharging && batteryLevel < 30
    }
    
    private fun generateSuggestions(
        currentConfig: ResourceConfig,
        stats: MiningStats,
        batteryLevel: Int,
        isCharging: Boolean,
        thermalState: Int
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Temperature suggestions
        when {
            stats.cpuTemp > 80f -> suggestions.add("⚠️ CRITICAL: Temperature very high! Reduce threads immediately")
            stats.cpuTemp > 70f -> suggestions.add("⚠️ High temperature detected. Consider reducing thread count")
            stats.cpuTemp < 55f && isCharging -> suggestions.add("✓ Temperature optimal. You can safely increase threads")
        }
        
        // Battery suggestions
        when {
            !isCharging && batteryLevel < 20 -> suggestions.add("🔋 Critical battery! Enable battery optimization")
            !isCharging && batteryLevel < 50 -> suggestions.add("🔋 Running on battery. Consider reducing performance")
            isCharging && batteryLevel > 80 -> suggestions.add("⚡ Fully charged and plugged in. Boost mode available!")
        }
        
        // Performance suggestions
        if (stats.cpuUsage < currentConfig.cpuUsageLimit * 0.5) {
            suggestions.add("📊 CPU underutilized. Increase thread count for better hashrate")
        }
        
        if (stats.hashrate < 100 && currentConfig.selectedThreads < 4) {
            suggestions.add("⚡ Low hashrate. Try increasing threads to 4 or more")
        }
        
        // Efficiency suggestions
        val efficiency = if (stats.powerUsage > 0) stats.hashrate / stats.powerUsage else 0.0
        if (efficiency < 100 && !isCharging) {
            suggestions.add("💡 Low efficiency on battery. Switch to ECO mode")
        }
        
        // Thermal state warnings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (thermalState) {
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL -> 
                    suggestions.add("🔥 Device overheating! Stop mining and let it cool down")
                PowerManager.THERMAL_STATUS_MODERATE ->
                    suggestions.add("🌡️ Thermal throttling active. Performance may be reduced")
            }
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("✓ All systems optimal. Mining efficiently!")
        }
        
        return suggestions
    }
    
    private fun calculateOptimizationScore(
        currentStats: MiningStats,
        config: ResourceConfig,
        batteryLevel: Int,
        isCharging: Boolean
    ): Float {
        var score = 100f
        
        // Temperature penalty
        score -= when {
            currentStats.cpuTemp > 80f -> 40f
            currentStats.cpuTemp > 70f -> 20f
            currentStats.cpuTemp > 60f -> 10f
            else -> 0f
        }
        
        // Battery penalty (if not charging)
        if (!isCharging) {
            score -= when {
                batteryLevel < 20 -> 30f
                batteryLevel < 50 -> 15f
                else -> 0f
            }
        }
        
        // CPU usage efficiency
        val usageEfficiency = currentStats.cpuUsage / config.cpuUsageLimit.toFloat()
        if (usageEfficiency < 0.5f) {
            score -= 15f // Underutilized
        }
        
        // Power efficiency bonus
        val hashesPerWatt = if (currentStats.powerUsage > 0) 
            currentStats.hashrate / currentStats.powerUsage 
        else 0.0
        
        if (hashesPerWatt > 200) {
            score += 10f // Excellent efficiency
        }
        
        return score.coerceIn(0f, 100f)
    }
    
    private fun determineOptimizationMode(
        config: ResourceConfig,
        maxThreads: Int
    ): OptimizationMode {
        val threadPercentage = config.selectedThreads.toFloat() / maxThreads
        val cpuLimitPercentage = config.cpuUsageLimit / 100f
        
        return when {
            threadPercentage <= 0.25f && cpuLimitPercentage <= 0.5f -> OptimizationMode.ECO
            threadPercentage >= 0.9f && cpuLimitPercentage >= 0.9f -> OptimizationMode.AGGRESSIVE
            threadPercentage >= 0.75f && cpuLimitPercentage >= 0.8f -> OptimizationMode.PERFORMANCE
            else -> OptimizationMode.BALANCED
        }
    }
    
    private fun updateHistoricalData(stats: MiningStats) {
        temperatureHistory.addLast(stats.cpuTemp)
        if (temperatureHistory.size > 60) temperatureHistory.removeFirst()
        
        hashrateHistory.addLast(stats.hashrate)
        if (hashrateHistory.size > 60) hashrateHistory.removeFirst()
        
        val efficiency = if (stats.powerUsage > 0) 
            (stats.hashrate / stats.powerUsage).toFloat() 
        else 0f
        powerEfficiencyHistory.addLast(efficiency)
        if (powerEfficiencyHistory.size > 60) powerEfficiencyHistory.removeFirst()
    }
    
    private fun updatePerformanceMetrics(stats: MiningStats) {
        val avgHashrate = if (hashrateHistory.isNotEmpty()) 
            hashrateHistory.average() 
        else 0.0
        
        val peakHashrate = hashrateHistory.maxOrNull() ?: 0.0
        
        val avgTemp = if (temperatureHistory.isNotEmpty()) 
            temperatureHistory.average().toFloat() 
        else 0f
        
        val tempTrend = calculateTrend(temperatureHistory)
        
        val hashesPerWatt = if (stats.powerUsage > 0) 
            stats.hashrate / stats.powerUsage 
        else 0.0
        
        // Estimate earnings (example: $0.00001 per 1000 hashes for XMR)
        val earningsPerHour = (avgHashrate * 3600 * 0.00001) / 1000
        
        _performanceMetrics.value = PerformanceMetrics(
            averageHashrate = avgHashrate,
            peakHashrate = peakHashrate,
            hashesPerWatt = hashesPerWatt,
            averageTemperature = avgTemp,
            temperatureTrend = tempTrend,
            uptimeSeconds = stats.uptime,
            totalHashesLifetime = stats.totalHashes,
            estimatedEarningsPerHour = earningsPerHour
        )
    }
    
    private fun calculateTrend(history: ArrayDeque<Float>): TrendDirection {
        if (history.size < 10) return TrendDirection.STABLE
        
        val recent = history.takeLast(10).average()
        val previous = history.take(10).average()
        val diff = recent - previous
        
        return when {
            diff > 2f -> TrendDirection.RISING
            diff < -2f -> TrendDirection.FALLING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            100
        }
    }
    
    private fun isDeviceCharging(): Boolean {
        return try {
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || 
            status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getThermalState(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                powerManager.currentThermalStatus
            } catch (e: Exception) {
                PowerManager.THERMAL_STATUS_NONE
            }
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }
    }
}
