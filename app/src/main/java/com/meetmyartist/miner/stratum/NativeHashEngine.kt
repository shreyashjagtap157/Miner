package com.meetmyartist.miner.stratum

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Hash Engine for Real Mining
 * 
 * Implements SHA-256 based mining with:
 * - Multi-threaded hash computation
 * - Hardware-optimized batch processing
 * - Dynamic intensity adjustment
 * - Integration with StratumClient
 */
@Singleton
class NativeHashEngine @Inject constructor(
    private val stratumClient: StratumClient
) {
    companion object {
        private const val TAG = "NativeHashEngine"
        private const val HASH_BATCH_SIZE = 1024
        private const val NONCE_RANGE_PER_THREAD = 0x10000L
    }

    // Mining state
    private val isMining = AtomicBoolean(false)
    private val totalHashes = AtomicLong(0)
    private val validShares = AtomicLong(0)
    private val invalidShares = AtomicLong(0)
    
    // Statistics
    private val _hashRate = MutableStateFlow(0.0)
    val hashRate: StateFlow<Double> = _hashRate.asStateFlow()
    
    private val _miningStats = MutableStateFlow(MiningStats())
    val miningStats: StateFlow<MiningStats> = _miningStats.asStateFlow()
    
    // Current job state
    private var currentJob: MiningJob? = null
    private var currentExtranonce2: Long = 0
    
    // Coroutine management
    private val miningScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var miningJobs = mutableListOf<Job>()
    
    // Mining intensity (0.0 to 1.0)
    private var intensity: Float = 0.5f
    
    // SHA-256 instance per thread (thread-safe)
    private val sha256 = ThreadLocal.withInitial {
        MessageDigest.getInstance("SHA-256")
    }

    /**
     * Start mining with specified number of threads
     */
    fun startMining(threadCount: Int = getOptimalThreadCount()) {
        if (isMining.getAndSet(true)) {
            Log.w(TAG, "Mining already in progress")
            return
        }
        
        Log.i(TAG, "Starting mining with $threadCount threads")
        
        // Subscribe to jobs
        miningScope.launch {
            stratumClient.currentJob.collect { job ->
                job?.let { handleNewJob(it, threadCount) }
            }
        }
        
        // Collect share results
        miningScope.launch {
            stratumClient.shareResults.collect { result ->
                if (result.accepted) {
                    validShares.incrementAndGet()
                } else {
                    invalidShares.incrementAndGet()
                }
                updateStats()
            }
        }
        
        // Start hashrate calculator
        startHashRateCalculator()
    }
    
    /**
     * Stop mining
     */
    fun stopMining() {
        if (!isMining.getAndSet(false)) return
        
        Log.i(TAG, "Stopping mining")
        
        miningJobs.forEach { it.cancel() }
        miningJobs.clear()
        
        _hashRate.value = 0.0
    }
    
    /**
     * Set mining intensity (0.0 to 1.0)
     */
    fun setIntensity(value: Float) {
        intensity = value.coerceIn(0.0f, 1.0f)
        Log.i(TAG, "Intensity set to $intensity")
    }
    
    /**
     * Handle new job from pool
     */
    private fun handleNewJob(job: MiningJob, threadCount: Int) {
        // Cancel current mining if clean jobs
        if (job.cleanJobs) {
            miningJobs.forEach { it.cancel() }
            miningJobs.clear()
        }
        
        currentJob = job
        currentExtranonce2++
        
        Log.d(TAG, "Starting work on job ${job.jobId}")
        
        // Divide nonce space among threads
        val nonceRangePerThread = 0xFFFFFFFFL / threadCount
        
        repeat(threadCount) { threadIndex ->
            val startNonce = threadIndex * nonceRangePerThread
            val endNonce = if (threadIndex == threadCount - 1) {
                0xFFFFFFFFL
            } else {
                (threadIndex + 1) * nonceRangePerThread - 1
            }
            
            val miningJob = miningScope.launch {
                mineRange(job, currentExtranonce2, startNonce, endNonce)
            }
            miningJobs.add(miningJob)
        }
    }
    
    /**
     * Mine a specific nonce range
     */
    private suspend fun mineRange(
        job: MiningJob,
        extranonce2: Long,
        startNonce: Long,
        endNonce: Long
    ) = withContext(Dispatchers.Default) {
        val extranonce2Hex = extranonce2.toString(16).padStart(job.extranonce2Size * 2, '0')
        
        // Build coinbase and get merkle root
        val coinbase = job.buildCoinbase(extranonce2Hex)
        val coinbaseHash = doubleSha256(coinbase)
        val merkleRoot = job.calculateMerkleRoot(coinbaseHash)
        
        // Get target
        val target = job.getTarget()
        
        var nonce = startNonce
        var batchCounter = 0
        
        while (nonce <= endNonce && isMining.get() && currentJob?.jobId == job.jobId) {
            // Apply intensity throttling
            if (intensity < 1.0f) {
                if (batchCounter >= HASH_BATCH_SIZE) {
                    val sleepTime = ((1.0f - intensity) * 10).toLong()
                    delay(sleepTime)
                    batchCounter = 0
                }
            }
            
            // Build and hash block header
            val header = job.buildBlockHeader(merkleRoot, nonce)
            val hash = doubleSha256(header)
            
            totalHashes.incrementAndGet()
            batchCounter++
            
            // Check if hash meets target
            if (meetsTarget(hash, target)) {
                Log.i(TAG, "Found valid share at nonce $nonce")
                
                // Submit share
                submitShare(job, extranonce2Hex, nonce)
            }
            
            nonce++
        }
    }
    
    /**
     * Submit valid share to pool
     */
    private fun submitShare(job: MiningJob, extranonce2: String, nonce: Long) {
        val nonceHex = nonce.toString(16).padStart(8, '0')
        
        miningScope.launch {
            stratumClient.submitShare(
                jobId = job.jobId,
                extranonce2 = extranonce2,
                ntime = job.ntime,
                nonce = nonceHex
            )
        }
    }
    
    /**
     * Check if hash meets target difficulty
     */
    private fun meetsTarget(hash: ByteArray, target: ByteArray): Boolean {
        // Compare hash to target (hash must be <= target)
        // Both are in big-endian byte order
        for (i in hash.indices) {
            val hashByte = hash[i].toInt() and 0xFF
            val targetByte = target[i].toInt() and 0xFF
            
            if (hashByte < targetByte) return true
            if (hashByte > targetByte) return false
        }
        return true
    }
    
    /**
     * Double SHA-256 hash
     */
    private fun doubleSha256(data: ByteArray): ByteArray {
        val sha = sha256.get()!!
        sha.reset()
        val firstHash = sha.digest(data)
        sha.reset()
        return sha.digest(firstHash)
    }
    
    /**
     * Start hashrate calculation
     */
    private fun startHashRateCalculator() {
        miningScope.launch {
            var lastHashes = 0L
            var lastTime = System.currentTimeMillis()
            
            while (isMining.get()) {
                delay(1000)
                
                val currentHashes = totalHashes.get()
                val currentTime = System.currentTimeMillis()
                
                val hashDiff = currentHashes - lastHashes
                val timeDiff = (currentTime - lastTime) / 1000.0
                
                _hashRate.value = if (timeDiff > 0) hashDiff / timeDiff else 0.0
                
                lastHashes = currentHashes
                lastTime = currentTime
                
                updateStats()
            }
        }
    }
    
    /**
     * Update mining statistics
     */
    private fun updateStats() {
        _miningStats.value = MiningStats(
            totalHashes = totalHashes.get(),
            hashRate = _hashRate.value,
            validShares = validShares.get(),
            invalidShares = invalidShares.get(),
            currentDifficulty = currentJob?.difficulty ?: 0.0,
            currentJobId = currentJob?.jobId,
            intensity = intensity,
            isActive = isMining.get()
        )
    }
    
    /**
     * Get optimal thread count for device
     */
    private fun getOptimalThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        // Use 75% of available cores for mining
        return (cores * 0.75).toInt().coerceAtLeast(1)
    }
    
    /**
     * Reset statistics
     */
    fun resetStats() {
        totalHashes.set(0)
        validShares.set(0)
        invalidShares.set(0)
        updateStats()
    }
}

/**
 * Mining statistics
 */
data class MiningStats(
    val totalHashes: Long = 0,
    val hashRate: Double = 0.0,
    val validShares: Long = 0,
    val invalidShares: Long = 0,
    val currentDifficulty: Double = 0.0,
    val currentJobId: String? = null,
    val intensity: Float = 0.5f,
    val isActive: Boolean = false
) {
    val shareAcceptRate: Double
        get() = if (validShares + invalidShares > 0) {
            validShares.toDouble() / (validShares + invalidShares) * 100
        } else 0.0
    
    val formattedHashRate: String
        get() = when {
            hashRate >= 1_000_000_000 -> "%.2f GH/s".format(hashRate / 1_000_000_000)
            hashRate >= 1_000_000 -> "%.2f MH/s".format(hashRate / 1_000_000)
            hashRate >= 1_000 -> "%.2f KH/s".format(hashRate / 1_000)
            else -> "%.2f H/s".format(hashRate)
        }
}
