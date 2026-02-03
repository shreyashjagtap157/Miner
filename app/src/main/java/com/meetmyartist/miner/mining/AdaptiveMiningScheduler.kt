package com.meetmyartist.miner.mining

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Adaptive Mining Scheduler - ML-based optimal mining time prediction
 * 
 * Learns from historical patterns to optimize mining profitability:
 * - Battery charge/discharge patterns
 * - Thermal behavior under different conditions
 * - Electricity cost patterns (time-of-use)
 * - Crypto profitability fluctuations
 * - Device usage patterns
 */

private val Context.schedulerDataStore by preferencesDataStore(name = "adaptive_scheduler")

data class SchedulerState(
    val isOptimalTime: Boolean,
    val confidenceScore: Float,
    val recommendedIntensity: Float,
    val nextOptimalWindow: LocalDateTime?,
    val reason: String
)

data class EnvironmentSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Float = 0f,
    val isCharging: Boolean = false,
    val temperature: Float = 0f,
    val hourOfDay: Int = 0,
    val dayOfWeek: Int = 0,
    val isWeekend: Boolean = false,
    val electricityPrice: Float = 1f, // Normalized 0-1
    val cryptoPrice: Float = 0f,
    val hashrate: Float = 0f,
    val powerConsumption: Float = 0f,
    val profitability: Float = 0f
)

data class TimeSlotScore(
    val hour: Int,
    val dayOfWeek: Int,
    val avgProfitability: Float,
    val avgBatteryDrain: Float,
    val avgTemperature: Float,
    val sampleCount: Int,
    val score: Float // Composite score for ranking
)

@Singleton
class AdaptiveMiningScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _schedulerState = MutableStateFlow(SchedulerState(
        isOptimalTime = true,
        confidenceScore = 0f,
        recommendedIntensity = 0.5f,
        nextOptimalWindow = null,
        reason = "Initializing..."
    ))
    val schedulerState: StateFlow<SchedulerState> = _schedulerState.asStateFlow()
    
    private val historyBuffer = ConcurrentLinkedQueue<EnvironmentSnapshot>()
    private val maxHistorySize = 10000
    
    // Time slot statistics: [dayOfWeek][hour]
    private val timeSlotStats = Array(7) { Array(24) { MutableTimeSlotStats() } }
    
    // Feature weights for scoring (learned over time)
    private var weightBattery = 0.25f
    private var weightTemperature = 0.20f
    private var weightElectricity = 0.20f
    private var weightProfitability = 0.25f
    private var weightCharging = 0.10f
    
    // Thresholds
    private var minBatteryLevel = 30f
    private var maxTemperature = 45f
    private var preferChargingOnly = false
    
    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    init {
        scope.launch {
            loadPersistedWeights()
        }
    }
    
    /**
     * Record current environment snapshot for learning
     */
    suspend fun recordSnapshot(
        hashrate: Float,
        powerConsumption: Float,
        cryptoPrice: Float = 0f,
        electricityPrice: Float = 1f
    ) {
        val now = LocalDateTime.now()
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        val temperature = getDeviceTemperature()
        
        val profitability = calculateProfitability(hashrate, powerConsumption, cryptoPrice, electricityPrice)
        
        val snapshot = EnvironmentSnapshot(
            timestamp = System.currentTimeMillis(),
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            temperature = temperature,
            hourOfDay = now.hour,
            dayOfWeek = now.dayOfWeek.value - 1, // 0-6
            isWeekend = now.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            electricityPrice = electricityPrice,
            cryptoPrice = cryptoPrice,
            hashrate = hashrate,
            powerConsumption = powerConsumption,
            profitability = profitability
        )
        
        // Update buffer
        historyBuffer.offer(snapshot)
        while (historyBuffer.size > maxHistorySize) {
            historyBuffer.poll()
        }
        
        // Update time slot statistics
        updateTimeSlotStats(snapshot)
        
        // Update scheduler state
        updateSchedulerState()
    }
    
    /**
     * Get recommended mining intensity for current conditions
     */
    fun getRecommendedIntensity(): Float {
        return _schedulerState.value.recommendedIntensity
    }
    
    /**
     * Check if current time is optimal for mining
     */
    fun isOptimalMiningTime(): Boolean {
        return _schedulerState.value.isOptimalTime
    }
    
    /**
     * Get next predicted optimal mining window
     */
    fun getNextOptimalWindow(): LocalDateTime? {
        return _schedulerState.value.nextOptimalWindow
    }
    
    /**
     * Get ranked time slots by profitability
     */
    fun getBestTimeSlots(topN: Int = 10): List<TimeSlotScore> {
        val scores = mutableListOf<TimeSlotScore>()
        
        for (day in 0 until 7) {
            for (hour in 0 until 24) {
                val stats = timeSlotStats[day][hour]
                if (stats.sampleCount >= 3) { // Need minimum samples
                    val score = calculateTimeSlotScore(stats, day, hour)
                    scores.add(TimeSlotScore(
                        hour = hour,
                        dayOfWeek = day,
                        avgProfitability = stats.avgProfitability,
                        avgBatteryDrain = stats.avgBatteryDrain,
                        avgTemperature = stats.avgTemperature,
                        sampleCount = stats.sampleCount,
                        score = score
                    ))
                }
            }
        }
        
        return scores.sortedByDescending { it.score }.take(topN)
    }
    
    /**
     * Configure scheduler preferences
     */
    fun configure(
        minBattery: Float = 30f,
        maxTemp: Float = 45f,
        chargingOnly: Boolean = false
    ) {
        minBatteryLevel = minBattery
        maxTemperature = maxTemp
        preferChargingOnly = chargingOnly
        
        scope.launch {
            updateSchedulerState()
        }
    }
    
    /**
     * Train the scheduler with accumulated data
     */
    suspend fun trainModel() {
        if (historyBuffer.size < 100) {
            return // Not enough data
        }
        
        // Calculate feature importance from historical correlations
        val snapshots = historyBuffer.toList()
        
        // Simple correlation-based weight learning
        val profitValues = snapshots.map { it.profitability }
        val profitMean = profitValues.average().toFloat()
        
        if (profitMean > 0) {
            // Correlation with battery level
            val batteryCorr = calculateCorrelation(
                snapshots.map { it.batteryLevel },
                profitValues
            )
            
            // Correlation with temperature (inverse - lower is better)
            val tempCorr = -calculateCorrelation(
                snapshots.map { it.temperature },
                profitValues
            )
            
            // Correlation with charging state
            val chargingCorr = calculateCorrelation(
                snapshots.map { if (it.isCharging) 1f else 0f },
                profitValues
            )
            
            // Update weights with exponential smoothing
            val alpha = 0.1f
            weightBattery = weightBattery * (1 - alpha) + max(0f, batteryCorr) * alpha
            weightTemperature = weightTemperature * (1 - alpha) + max(0f, tempCorr) * alpha
            weightCharging = weightCharging * (1 - alpha) + max(0f, chargingCorr) * alpha
            
            // Normalize weights
            val totalWeight = weightBattery + weightTemperature + weightCharging + 
                              weightElectricity + weightProfitability
            if (totalWeight > 0) {
                weightBattery /= totalWeight
                weightTemperature /= totalWeight
                weightCharging /= totalWeight
                weightElectricity /= totalWeight
                weightProfitability /= totalWeight
            }
            
            persistWeights()
        }
    }
    
    // ============ Private Methods ============
    
    private fun getBatteryLevel(): Float {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }
    
    private fun isDeviceCharging(): Boolean {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    private fun getDeviceTemperature(): Float {
        // Approximate using battery temperature (not ideal but widely available)
        // In production, use thermal API or native sensors
        return 35f // Placeholder - would read from actual sensors
    }
    
    private fun calculateProfitability(
        hashrate: Float,
        powerConsumption: Float,
        cryptoPrice: Float,
        electricityPrice: Float
    ): Float {
        if (hashrate <= 0 || powerConsumption <= 0) return 0f
        
        // Simple profitability estimate: (hashrate * crypto_value) - (power * electricity_cost)
        // Normalized to 0-1 range
        val revenue = hashrate * cryptoPrice * 0.00001f // Scaling factor
        val cost = powerConsumption * electricityPrice * 0.001f
        
        return max(0f, (revenue - cost) / max(revenue, 1f))
    }
    
    private fun updateTimeSlotStats(snapshot: EnvironmentSnapshot) {
        val day = snapshot.dayOfWeek
        val hour = snapshot.hourOfDay
        val stats = timeSlotStats[day][hour]
        
        // Exponential moving average update
        val alpha = 0.05f
        stats.avgProfitability = stats.avgProfitability * (1 - alpha) + snapshot.profitability * alpha
        stats.avgTemperature = stats.avgTemperature * (1 - alpha) + snapshot.temperature * alpha
        stats.avgBatteryDrain = stats.avgBatteryDrain * (1 - alpha) + 
            (if (snapshot.isCharging) 0f else (100f - snapshot.batteryLevel) / 100f) * alpha
        stats.avgChargingRate = stats.avgChargingRate * (1 - alpha) + 
            (if (snapshot.isCharging) 1f else 0f) * alpha
        stats.sampleCount++
    }
    
    private fun calculateTimeSlotScore(stats: MutableTimeSlotStats, day: Int, hour: Int): Float {
        // Weighted composite score
        val profitScore = stats.avgProfitability
        val tempScore = 1f - (stats.avgTemperature / 60f).coerceIn(0f, 1f) // Lower is better
        val batteryScore = 1f - stats.avgBatteryDrain // Less drain is better
        val chargingScore = stats.avgChargingRate // Charging is better
        
        return (profitScore * weightProfitability +
                tempScore * weightTemperature +
                batteryScore * weightBattery +
                chargingScore * weightCharging)
    }
    
    private suspend fun updateSchedulerState() {
        val now = LocalDateTime.now()
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        val temperature = getDeviceTemperature()
        
        // Check hard constraints
        val batteryOk = batteryLevel >= minBatteryLevel
        val tempOk = temperature <= maxTemperature
        val chargingOk = !preferChargingOnly || isCharging
        
        val constraintsPassed = batteryOk && tempOk && chargingOk
        
        // Get current time slot score
        val currentStats = timeSlotStats[now.dayOfWeek.value - 1][now.hour]
        val currentScore = if (currentStats.sampleCount >= 3) {
            calculateTimeSlotScore(currentStats, now.dayOfWeek.value - 1, now.hour)
        } else {
            0.5f // Default neutral score
        }
        
        // Find next optimal window
        var nextOptimal: LocalDateTime? = null
        var bestFutureScore = currentScore
        
        for (hoursAhead in 1..48) {
            val futureTime = now.plusHours(hoursAhead.toLong())
            val futureDay = futureTime.dayOfWeek.value - 1
            val futureHour = futureTime.hour
            val futureStats = timeSlotStats[futureDay][futureHour]
            
            if (futureStats.sampleCount >= 3) {
                val score = calculateTimeSlotScore(futureStats, futureDay, futureHour)
                if (score > bestFutureScore + 0.1f) { // Significantly better
                    bestFutureScore = score
                    nextOptimal = futureTime
                    break
                }
            }
        }
        
        // Determine recommended intensity
        val intensity = when {
            !constraintsPassed -> 0f
            currentScore > 0.7f -> 1.0f
            currentScore > 0.5f -> 0.7f
            currentScore > 0.3f -> 0.4f
            else -> 0.2f
        }
        
        // Build reason string
        val reason = buildString {
            if (!batteryOk) append("Low battery (${batteryLevel.toInt()}%). ")
            if (!tempOk) append("High temperature (${temperature.toInt()}°C). ")
            if (!chargingOk) append("Not charging. ")
            if (constraintsPassed) {
                if (currentScore > 0.7f) append("Excellent conditions. ")
                else if (currentScore > 0.5f) append("Good conditions. ")
                else append("Moderate conditions. ")
            }
        }.trim()
        
        _schedulerState.value = SchedulerState(
            isOptimalTime = constraintsPassed && currentScore > 0.4f,
            confidenceScore = min(1f, currentStats.sampleCount / 50f),
            recommendedIntensity = intensity,
            nextOptimalWindow = nextOptimal,
            reason = reason.ifEmpty { "Ready to mine" }
        )
    }
    
    private fun calculateCorrelation(x: List<Float>, y: List<Float>): Float {
        if (x.size != y.size || x.size < 2) return 0f
        
        val n = x.size
        val meanX = x.average().toFloat()
        val meanY = y.average().toFloat()
        
        var numerator = 0f
        var denomX = 0f
        var denomY = 0f
        
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }
        
        val denom = kotlin.math.sqrt(denomX * denomY)
        return if (denom > 0) (numerator / denom).coerceIn(-1f, 1f) else 0f
    }
    
    private suspend fun persistWeights() {
        context.schedulerDataStore.edit { prefs ->
            prefs[WEIGHT_BATTERY] = weightBattery
            prefs[WEIGHT_TEMPERATURE] = weightTemperature
            prefs[WEIGHT_ELECTRICITY] = weightElectricity
            prefs[WEIGHT_PROFITABILITY] = weightProfitability
            prefs[WEIGHT_CHARGING] = weightCharging
        }
    }
    
    private suspend fun loadPersistedWeights() {
        context.schedulerDataStore.data.first().let { prefs ->
            weightBattery = prefs[WEIGHT_BATTERY] ?: 0.25f
            weightTemperature = prefs[WEIGHT_TEMPERATURE] ?: 0.20f
            weightElectricity = prefs[WEIGHT_ELECTRICITY] ?: 0.20f
            weightProfitability = prefs[WEIGHT_PROFITABILITY] ?: 0.25f
            weightCharging = prefs[WEIGHT_CHARGING] ?: 0.10f
        }
    }
    
    companion object {
        private val WEIGHT_BATTERY = floatPreferencesKey("weight_battery")
        private val WEIGHT_TEMPERATURE = floatPreferencesKey("weight_temperature")
        private val WEIGHT_ELECTRICITY = floatPreferencesKey("weight_electricity")
        private val WEIGHT_PROFITABILITY = floatPreferencesKey("weight_profitability")
        private val WEIGHT_CHARGING = floatPreferencesKey("weight_charging")
    }
}

private class MutableTimeSlotStats {
    var avgProfitability: Float = 0f
    var avgTemperature: Float = 30f
    var avgBatteryDrain: Float = 0f
    var avgChargingRate: Float = 0f
    var sampleCount: Int = 0
}
