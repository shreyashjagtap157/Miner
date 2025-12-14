package com.meetmyartist.miner.mining

import com.meetmyartist.miner.data.model.MiningStats
import com.meetmyartist.miner.network.PoolConnectionManager
import com.meetmyartist.miner.network.StratumClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Real mining worker that performs actual hashing
 * Supports pool connectivity via Stratum protocol
 */
@Singleton
class RealMiningWorker @Inject constructor(
    private val poolConnectionManager: PoolConnectionManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val workers = mutableListOf<Job>()
    
    private val _hashrate = MutableStateFlow(0.0)
    val hashrate: StateFlow<Double> = _hashrate.asStateFlow()
    
    private var totalHashes = 0L
    private var lastHashCountTime = System.currentTimeMillis()
    
    fun startWorkers(threadCount: Int) {
        stopWorkers()
        
        repeat(threadCount) { threadId ->
            val job = scope.launch {
                workerLoop(threadId)
            }
            workers.add(job)
        }
        
        // Start hashrate calculator
        scope.launch {
            calculateHashrate()
        }
    }
    
    fun stopWorkers() {
        workers.forEach { it.cancel() }
        workers.clear()
        totalHashes = 0L
    }
    
    private suspend fun workerLoop(threadId: Int) {
        while (currentCoroutineContext().isActive) {
            try {
                // Get current mining job from pool
                val job = poolConnectionManager.getCurrentJob().value
                
                if (job != null) {
                    // Mine on this job
                    mineJob(job, threadId)
                } else {
                    // Wait for job
                    delay(1000)
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    delay(5000) // Wait before retry
                }
            }
        }
    }
    
    private suspend fun mineJob(job: StratumClient.MiningJob, threadId: Int) {
        // Generate unique extranonce2 for this thread
        val extranonce2 = CryptoHasher.generateExtranonce2(
            poolConnectionManager.getConnectionState().value.let { 
                4 // Default size, should get from pool
            }
        )
        
        // Starting nonce for this thread
        var nonce = (0xFFFFFFFF / 100 * threadId).toUInt()
        val nonceEnd = (0xFFFFFFFF / 100 * (threadId + 1)).toUInt()
        
        while (currentCoroutineContext().isActive && nonce < nonceEnd) {
            // Build coinbase
            val coinbase = CryptoHasher.buildCoinbase(
                job.coinbase1,
                poolConnectionManager.getConnectionState().value.let { "" }, // extranonce1 from pool
                extranonce2,
                job.coinbase2
            )
            
            // Calculate merkle root
            val merkleRoot = CryptoHasher.calculateMerkleRoot(coinbase, job.merkleBranch)
            
            // Build block header
            val header = CryptoHasher.buildBlockHeader(
                job.version,
                job.prevHash,
                merkleRoot,
                job.ntime,
                job.nbits,
                nonce.toString(16).padStart(8, '0')
            )
            
            // Hash the header
            val hash = CryptoHasher.sha256d(header)
            
            // Check if hash meets target
            val target = CryptoHasher.calculateTarget(job.nbits)
            
            if (CryptoHasher.meetsTarget(hash, target)) {
                // Found valid share!
                submitShare(
                    job.jobId,
                    extranonce2,
                    job.ntime,
                    nonce.toString(16).padStart(8, '0')
                )
            }
            
            nonce++
            totalHashes++
            
            // Yield every 1000 hashes to allow other operations
            if (totalHashes % 1000L == 0L) {
                yield()
            }
        }
    }
    
    private suspend fun submitShare(
        jobId: String,
        extranonce2: String,
        ntime: String,
        nonce: String
    ) {
        poolConnectionManager.submitShare(jobId, extranonce2, ntime, nonce)
    }
    
    private suspend fun calculateHashrate() {
        while (currentCoroutineContext().isActive) {
            delay(2000) // Update every 2 seconds
            
            val currentTime = System.currentTimeMillis()
            val timeDiff = (currentTime - lastHashCountTime) / 1000.0
            
            if (timeDiff > 0) {
                val currentHashrate = totalHashes / timeDiff
                _hashrate.value = currentHashrate
            }
            
            lastHashCountTime = currentTime
            totalHashes = 0L
        }
    }
    
    fun getTotalHashes(): Long = totalHashes
}
