package com.meetmyartist.miner.mining

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.meetmyartist.miner.data.model.CryptoAlgorithm
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.MiningStats
import com.meetmyartist.miner.data.model.ResourceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MiningEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val poolConnectionManager: com.meetmyartist.miner.network.PoolConnectionManager,
    private val realMiningWorker: RealMiningWorker
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _miningState = MutableStateFlow(MiningState.STOPPED)
    val miningState: StateFlow<MiningState> = _miningState.asStateFlow()
    
    private val _miningStats = MutableStateFlow(MiningStats())
    val miningStats: StateFlow<MiningStats> = _miningStats.asStateFlow()
    
    private var miningJobs = mutableListOf<Job>()
    private var resourceMonitorJob: Job? = null
    private var statsUpdateJob: Job? = null
    private var startTime: Long = 0
    
    private var currentConfig: MiningConfig? = null
    private var currentResourceConfig: ResourceConfig? = null
    
    // Mode selection: false = simulated (for testing), true = real mining
    private var useRealMining: Boolean = false
    
    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    enum class MiningState {
        STOPPED,
        STARTING,
        MINING, // Changed from RUNNING
        PAUSED,
        THROTTLED,
        ERROR
    }
    
    fun startMining(config: MiningConfig, resourceConfig: ResourceConfig, useRealMining: Boolean = false) {
        if (_miningState.value == MiningState.MINING) {
            return
        }
        
        currentConfig = config
        currentResourceConfig = resourceConfig
        this.useRealMining = useRealMining
        
        _miningState.value = MiningState.STARTING
        startTime = System.currentTimeMillis()
        
        scope.launch {
            try {
                if (useRealMining) {
                    // Connect to mining pool
                    poolConnectionManager.connectToPool(config)
                    
                    // Start real mining workers
                    realMiningWorker.startWorkers(resourceConfig.selectedThreads)
                    
                    // Monitor real hashrate
                    statsUpdateJob = scope.launch {
                        realMiningWorker.hashrate.collect { hashrate ->
                            val currentStats = _miningStats.value
                            _miningStats.value = currentStats.copy(
                                hashrate = hashrate,
                                totalHashes = realMiningWorker.getTotalHashes()
                            )
                        }
                    }
                } else {
                    // Start simulated mining threads
                    val threadCount = resourceConfig.selectedThreads
                    for (i in 0 until threadCount) {
                        val job = scope.launch {
                            mineOnThread(i, config, resourceConfig)
                        }
                        miningJobs.add(job)
                    }
                }
                
                // Start resource monitoring
                resourceMonitorJob = scope.launch {
                    monitorResources()
                }
                
                _miningState.value = MiningState.MINING
            } catch (e: Exception) {
                _miningState.value = MiningState.ERROR
                e.printStackTrace()
            }
        }
    }
    
    fun stopMining() {
        _miningState.value = MiningState.STOPPED
        
        // Stop real mining if active
        if (useRealMining) {
            realMiningWorker.stopWorkers()
            scope.launch {
                poolConnectionManager.disconnect()
            }
            statsUpdateJob?.cancel()
            statsUpdateJob = null
        }
        
        // Cancel all mining jobs
        miningJobs.forEach { it.cancel() }
        miningJobs.clear()
        
        resourceMonitorJob?.cancel()
        resourceMonitorJob = null
        
        // Reset stats
        _miningStats.value = MiningStats()
    }
    
    fun pauseMining() {
        if (_miningState.value == MiningState.MINING) {
            _miningState.value = MiningState.PAUSED
            miningJobs.forEach { it.cancel() }
            miningJobs.clear()
        }
    }
    
    fun resumeMining() {
        if (_miningState.value == MiningState.PAUSED) {
            currentConfig?.let { config ->
                currentResourceConfig?.let { resourceConfig ->
                    val threadCount = resourceConfig.selectedThreads
                    for (i in 0 until threadCount) {
                        val job = scope.launch {
                            mineOnThread(i, config, resourceConfig)
                        }
                        miningJobs.add(job)
                    }
                    _miningState.value = MiningState.MINING
                }
            }
        }
    }
    
    private suspend fun mineOnThread(threadId: Int, config: MiningConfig, resourceConfig: ResourceConfig) {
        while (currentCoroutineContext().isActive && _miningState.value == MiningState.MINING) {
            try {
                // Simulate mining based on algorithm
                val hashes = when (config.algorithm) {
                    "RANDOMX" -> performRandomXMining()
                    "SHA256" -> performSHA256Mining()
                    "ETHASH" -> performEthashMining()
                    "SCRYPT" -> performScryptMining()
                    "EQUIHASH" -> performEquihashMining()
                    "KAWPOW" -> performKawPowMining()
                    "CRYPTONIGHT" -> performCryptoNightMining()
                    else -> 0L
                }
                
                // Update stats
                updateStats(hashes)
                
                // Apply CPU throttling based on usage limit
                val throttleDelay = calculateThrottleDelay(resourceConfig.cpuUsageLimit)
                if (throttleDelay > 0) {
                    delay(throttleDelay)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun monitorResources() {
        while (currentCoroutineContext().isActive) {
            try {
                val cpuTemp = getCpuTemperature()
                val cpuUsage = getCpuUsage()
                val batteryLevel = getBatteryLevel()
                val batteryTemp = getBatteryTemperature()
                
                val currentStats = _miningStats.value
                val uptime = (System.currentTimeMillis() - startTime) / 1000
                
                _miningStats.value = currentStats.copy(
                    cpuTemp = cpuTemp,
                    cpuUsage = cpuUsage,
                    uptime = uptime,
                    powerUsage = estimatePowerUsage(cpuUsage)
                )
                
                // Check thermal throttling
                currentResourceConfig?.let { config ->
                    if (config.enableThermalThrottle && cpuTemp > config.maxTemperature) {
                        if (_miningState.value == MiningState.MINING) {
                            _miningState.value = MiningState.THROTTLED
                            pauseMining()
                            delay(30000) // Wait 30 seconds
                            resumeMining()
                        }
                    }
                    
                    // Check battery level
                    if (config.pauseOnLowBattery && batteryLevel < config.lowBatteryThreshold) {
                        if (_miningState.value == MiningState.MINING) {
                            pauseMining()
                        }
                    } else if (_miningState.value == MiningState.PAUSED && 
                               batteryLevel >= config.lowBatteryThreshold + 10) {
                        resumeMining()
                    }
                }
                
                delay(2000) // Update every 2 seconds
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Mining algorithm implementations using native library
    private suspend fun performRandomXMining(): Long = withContext(Dispatchers.Default) {
        if (!NativeMiner.isNativeAvailable()) {
            delay(10)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        val key = ByteArray(32) { it.toByte() } // Should be blockchain seed
        
        val startTime = System.nanoTime()
        repeat(100) { // Batch 100 hashes
            NativeMiner.randomxLight(input, key)
        }
        val elapsed = System.nanoTime() - startTime
        
        // Return hashes performed
        (100L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performSHA256Mining(): Long = withContext(Dispatchers.Default) {
        if (!NativeMiner.isNativeAvailable()) {
            delay(5)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        
        val startTime = System.nanoTime()
        repeat(10000) { // SHA256 is fast, batch 10000
            NativeMiner.sha256d(input)
        }
        val elapsed = System.nanoTime() - startTime
        
        (10000L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performEthashMining(): Long = withContext(Dispatchers.Default) {
        // Ethash is memory-intensive, use Blake3 as approximation
        if (!NativeMiner.isNativeAvailable()) {
            delay(15)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        
        val startTime = System.nanoTime()
        repeat(500) {
            NativeMiner.blake3(input)
        }
        val elapsed = System.nanoTime() - startTime
        
        (500L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performScryptMining(): Long = withContext(Dispatchers.Default) {
        if (!NativeMiner.isNativeAvailable()) {
            delay(8)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        
        val startTime = System.nanoTime()
        repeat(10) { // Scrypt is slow, batch 10
            NativeMiner.scrypt(input, 1024, 1, 1) // Litecoin params
        }
        val elapsed = System.nanoTime() - startTime
        
        (10L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performEquihashMining(): Long = withContext(Dispatchers.Default) {
        // Use Blake3 as approximation for Equihash
        if (!NativeMiner.isNativeAvailable()) {
            delay(12)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        
        val startTime = System.nanoTime()
        repeat(100) {
            NativeMiner.blake3(input)
        }
        val elapsed = System.nanoTime() - startTime
        
        (100L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performKawPowMining(): Long = withContext(Dispatchers.Default) {
        // KawPow uses modified Ethash, approximate with Blake3
        if (!NativeMiner.isNativeAvailable()) {
            delay(11)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        
        val startTime = System.nanoTime()
        repeat(200) {
            NativeMiner.blake3(input)
        }
        val elapsed = System.nanoTime() - startTime
        
        (200L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private suspend fun performCryptoNightMining(): Long = withContext(Dispatchers.Default) {
        // CryptoNight is similar to RandomX, use that
        if (!NativeMiner.isNativeAvailable()) {
            delay(10)
            return@withContext 0L
        }
        
        val input = generateMiningInput()
        val key = ByteArray(32) { (it * 7).toByte() }
        
        val startTime = System.nanoTime()
        repeat(50) {
            NativeMiner.randomxLight(input, key)
        }
        val elapsed = System.nanoTime() - startTime
        
        (50L * 1_000_000_000L / elapsed.coerceAtLeast(1))
    }
    
    private fun generateMiningInput(): ByteArray {
        val input = ByteArray(80)
        System.currentTimeMillis().let { time ->
            for (i in 0..7) {
                input[i] = ((time shr (i * 8)) and 0xFF).toByte()
            }
        }
        // Add random nonce
        kotlin.random.Random.nextBytes(input, 8, 80)
        return input
    }
    
    private fun updateStats(hashes: Long) {
        val currentStats = _miningStats.value
        val newTotalHashes = currentStats.totalHashes + hashes
        val uptime = (System.currentTimeMillis() - startTime) / 1000.0
        val hashrate = if (uptime > 0) newTotalHashes / uptime else 0.0
        
        _miningStats.value = currentStats.copy(
            totalHashes = newTotalHashes,
            hashrate = hashrate
        )
    }
    
    private fun calculateThrottleDelay(cpuUsageLimit: Int): Long {
        val currentUsage = _miningStats.value.cpuUsage
        return if (currentUsage > cpuUsageLimit) {
            // Throttle more aggressively as we exceed the limit
            val excess = currentUsage - cpuUsageLimit
            (excess * 10).toLong() // ms delay
        } else {
            0L
        }
    }
    
    private fun getCpuTemperature(): Float {
        return try {
            // Try to read CPU temperature from thermal zone
            val thermalFiles = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )
            
            for (path in thermalFiles) {
                val file = File(path)
                if (file.exists()) {
                    val temp = file.readText().trim().toFloatOrNull()
                    if (temp != null) {
                        return temp / 1000f // Convert from millidegrees to degrees
                    }
                }
            }
            
            // Fallback to battery temperature if CPU temp not available
            getBatteryTemperature()
        } catch (e: Exception) {
            40f // Default value
        }
    }
    
    private fun getBatteryTemperature(): Float {
        return try {
            val temp = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            temp / 10f
        } catch (e: Exception) {
            30f
        }
    }
    
    private fun getCpuUsage(): Float {
        return try {
            val stat = RandomAccessFile("/proc/stat", "r")
            val cpuLine = stat.readLine()
            stat.close()
            
            val tokens = cpuLine.split("\\s+".toRegex())
            val idle = tokens[4].toLong()
            val total = tokens.slice(1..7).sumOf { it.toLong() }
            
            // This is simplified; proper implementation would track deltas
            val usage = (1.0f - (idle.toFloat() / total.toFloat())) * 100f
            min(usage, 100f)
        } catch (e: Exception) {
            50f // Default value
        }
    }
    
    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun estimatePowerUsage(cpuUsage: Float): Float {
        // Rough estimation: assume max CPU power draw is around 2-5W on mobile
        val maxPower = 3.5f // watts
        return (cpuUsage / 100f) * maxPower
    }
}
