package com.meetmyartist.miner.mining

import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicResourceController @Inject constructor() {
    
    // Dynamically detect CPU core count
    val availableCores: Int = Runtime.getRuntime().availableProcessors()
    
    private val _activeThreadCount = MutableStateFlow(0)
    val activeThreadCount: StateFlow<Int> = _activeThreadCount.asStateFlow()
    
    private val _currentHashrate = MutableStateFlow(0.0)
    val currentHashrate: StateFlow<Double> = _currentHashrate.asStateFlow()
    
    private val _perCoreStats = MutableStateFlow<Map<Int, CoreStats>>(emptyMap())
    val perCoreStats: StateFlow<Map<Int, CoreStats>> = _perCoreStats.asStateFlow()
    
    private var miningJobs = mutableMapOf<Int, Job>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    init {
        // Initialize per-core stats map for all available cores
        val initialStats = (0 until availableCores).associateWith { coreId ->
            CoreStats(
                coreId = coreId,
                hashrate = 0.0,
                usage = 0
            )
        }
        _perCoreStats.value = initialStats
    }
    
    fun updateThreadCount(newCount: Int) {
        val currentCount = miningJobs.size
        
        when {
            newCount > currentCount -> {
                // Add more threads
                repeat(newCount - currentCount) { index ->
                    val threadId = currentCount + index
                    startMiningThread(threadId)
                }
            }
            newCount < currentCount -> {
                // Remove threads
                val threadsToRemove = miningJobs.keys.sortedDescending().take(currentCount - newCount)
                threadsToRemove.forEach { threadId ->
                    stopMiningThread(threadId)
                }
            }
        }
        _activeThreadCount.value = newCount
    }
    
    fun setCoreAffinity(threadId: Int, coreId: Int, usagePercent: Int) {
        // Android doesn't expose direct CPU affinity APIs, but we can adjust thread priority
        // and control execution time to simulate per-core usage limits
        miningJobs[threadId]?.let { job ->
            // Restart thread with new parameters
            stopMiningThread(threadId)
            startMiningThread(threadId, coreId, usagePercent)
        }
    }
    
    fun setHashrateLimit(maxHashrate: Double?) {
        if (maxHashrate != null && maxHashrate > 0) {
            // Implement hashrate throttling by adding delays between hashing operations
            val targetDelayMs = calculateDelayForHashrate(maxHashrate, _activeThreadCount.value)
            applyHashrateThrottling(targetDelayMs)
        }
    }
    
    private fun calculateDelayForHashrate(targetHashrate: Double, threadCount: Int): Long {
        if (threadCount == 0) return 0L
        val currentHashrate = _currentHashrate.value
        if (currentHashrate <= targetHashrate) return 0L
        
        // Calculate how much to slow down
        val ratio = targetHashrate / currentHashrate
        val activeTime = 100L // Active time in ms
        val delayTime = ((activeTime / ratio) - activeTime).toLong().coerceAtLeast(0)
        return delayTime
    }
    
    private fun applyHashrateThrottling(delayMs: Long) {
        // Apply delay to all active mining threads
        miningJobs.forEach { (threadId, _) ->
            updateThreadDelay(threadId, delayMs)
        }
    }
    
    private fun startMiningThread(threadId: Int, targetCore: Int = -1, usagePercent: Int = 100) {
        val job = coroutineScope.launch {
            // Set thread priority based on target usage
            val priority = when {
                usagePercent >= 90 -> Process.THREAD_PRIORITY_URGENT_AUDIO
                usagePercent >= 70 -> Process.THREAD_PRIORITY_DEFAULT
                usagePercent >= 50 -> Process.THREAD_PRIORITY_BACKGROUND
                else -> Process.THREAD_PRIORITY_LOWEST
            }
            
            try {
                Process.setThreadPriority(priority)
            } catch (e: Exception) {
                // Ignore if setting priority fails
            }
            
            var hashCount = 0L
            val startTime = System.currentTimeMillis()
            
            while (isActive) {
                // Simulate hashing work
                val workCycles = (usagePercent * 100).toLong()
                repeat(workCycles.toInt()) {
                    // Actual hashing would happen here
                    hashCount++
                }
                
                // Apply usage limit by adding delay
                val sleepTime = ((100 - usagePercent) * 10).toLong()
                if (sleepTime > 0) {
                    delay(sleepTime)
                }
                
                // Update stats every second
                if (hashCount % 10000 == 0L) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val hashrate = hashCount / elapsed
                    updateCoreStats(threadId, hashrate, usagePercent)
                }
            }
        }
        
        miningJobs[threadId] = job
    }
    
    private fun stopMiningThread(threadId: Int) {
        miningJobs[threadId]?.cancel()
        miningJobs.remove(threadId)
        removeCoreStats(threadId)
    }
    
    private var threadDelays = mutableMapOf<Int, Long>()
    
    private fun updateThreadDelay(threadId: Int, delayMs: Long) {
        threadDelays[threadId] = delayMs
    }
    
    private fun updateCoreStats(threadId: Int, hashrate: Double, usage: Int) {
        val currentStats = _perCoreStats.value.toMutableMap()
        currentStats[threadId] = CoreStats(threadId, hashrate, usage)
        _perCoreStats.value = currentStats
        
        // Update total hashrate
        _currentHashrate.value = currentStats.values.sumOf { it.hashrate }
    }
    
    private fun removeCoreStats(threadId: Int) {
        val currentStats = _perCoreStats.value.toMutableMap()
        currentStats.remove(threadId)
        _perCoreStats.value = currentStats
        _currentHashrate.value = currentStats.values.sumOf { it.hashrate }
    }
    
    fun stopAll() {
        miningJobs.values.forEach { it.cancel() }
        miningJobs.clear()
        _activeThreadCount.value = 0
        _currentHashrate.value = 0.0
        _perCoreStats.value = emptyMap()
    }
    
    fun stopResourceManagement() {
        stopAll()
    }
}

data class CoreStats(
    val coreId: Int,
    val hashrate: Double,
    val usage: Int
)
