/*
 * Multi-Algorithm Mining Support
 *
 * Provides implementations for multiple mining algorithms:
 * - SHA-256 (Bitcoin)
 * - Ethash (Ethereum Classic)
 * - RandomX (Monero)
 * - KawPow (Ravencoin)
 *
 * Features:
 * - DAG generation for Ethash
 * - Memory-hard algorithm support
 * - Algorithm auto-switching based on profitability
 * - Benchmark suite for each algorithm
 */

package com.miner.algorithms

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.experimental.xor

/**
 * Supported mining algorithms
 */
enum class MiningAlgorithm(
    val displayName: String,
    val memoryRequirementMB: Int,
    val cpuIntensive: Boolean,
    val gpuOptimized: Boolean
) {
    SHA256("SHA-256", 1, false, true),
    SHA256D("SHA-256d", 1, false, true),
    SCRYPT("Scrypt", 128, true, true),
    ETHASH("Ethash", 4096, false, true),
    ETCHASH("Etchash", 3072, false, true),
    RANDOMX("RandomX", 2048, true, false),
    KAWPOW("KawPow", 4096, false, true),
    GHOSTRIDER("GhostRider", 256, true, false)
}

/**
 * Algorithm-specific configuration
 */
data class AlgorithmConfig(
    val algorithm: MiningAlgorithm,
    val intensity: Float = 1.0f,
    val workSize: Int = 256,
    val batchSize: Int = 1024 * 1024,
    val customParams: Map<String, Any> = emptyMap()
)

/**
 * Result from algorithm benchmark
 */
data class BenchmarkResult(
    val algorithm: MiningAlgorithm,
    val hashRate: Double,
    val powerEfficiency: Double,  // H/W
    val temperature: Float,
    val durationMs: Long,
    val success: Boolean
)

/**
 * Base interface for mining algorithm implementations
 */
interface MiningAlgorithmImpl {
    val algorithm: MiningAlgorithm
    
    suspend fun initialize(): Boolean
    suspend fun compute(header: ByteArray, nonce: Long, target: ByteArray): HashResult
    suspend fun computeBatch(header: ByteArray, startNonce: Long, count: Int, target: ByteArray): BatchHashResult
    suspend fun benchmark(durationMs: Long = 10000): BenchmarkResult
    fun shutdown()
}

data class HashResult(
    val hash: ByteArray,
    val nonce: Long,
    val meetsTarget: Boolean
)

data class BatchHashResult(
    val hashes: List<HashResult>,
    val bestHash: HashResult?,
    val hashesComputed: Int,
    val computeTimeMs: Long
)

/**
 * SHA-256 Double Hash Implementation (Bitcoin style)
 */
class SHA256DAlgorithm : MiningAlgorithmImpl {
    override val algorithm = MiningAlgorithm.SHA256D
    
    private lateinit var digest1: MessageDigest
    private lateinit var digest2: MessageDigest
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            digest1 = MessageDigest.getInstance("SHA-256")
            digest2 = MessageDigest.getInstance("SHA-256")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun compute(header: ByteArray, nonce: Long, target: ByteArray): HashResult {
        val block = header.copyOf(header.size + 8)
        ByteBuffer.wrap(block, header.size, 8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(nonce)
        
        val hash1 = digest1.digest(block)
        val hash2 = digest2.digest(hash1)
        
        return HashResult(
            hash = hash2,
            nonce = nonce,
            meetsTarget = compareHashToTarget(hash2, target)
        )
    }
    
    override suspend fun computeBatch(
        header: ByteArray,
        startNonce: Long,
        count: Int,
        target: ByteArray
    ): BatchHashResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<HashResult>()
        var bestHash: HashResult? = null
        
        for (i in 0 until count) {
            val result = compute(header, startNonce + i, target)
            if (result.meetsTarget) {
                results.add(result)
            }
            
            if (bestHash == null || compareHashes(result.hash, bestHash.hash) < 0) {
                bestHash = result
            }
        }
        
        BatchHashResult(
            hashes = results,
            bestHash = bestHash,
            hashesComputed = count,
            computeTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    override suspend fun benchmark(durationMs: Long): BenchmarkResult {
        initialize()
        val startTime = System.currentTimeMillis()
        var hashCount = 0L
        val testHeader = ByteArray(80) { it.toByte() }
        val testTarget = ByteArray(32) { 0xFF.toByte() }
        
        while (System.currentTimeMillis() - startTime < durationMs) {
            compute(testHeader, hashCount, testTarget)
            hashCount++
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        val hashRate = hashCount.toDouble() / (elapsed / 1000.0)
        
        return BenchmarkResult(
            algorithm = algorithm,
            hashRate = hashRate,
            powerEfficiency = hashRate / 1.0,  // Placeholder
            temperature = 0f,
            durationMs = elapsed,
            success = true
        )
    }
    
    override fun shutdown() {}
    
    private fun compareHashToTarget(hash: ByteArray, target: ByteArray): Boolean {
        for (i in hash.indices.reversed()) {
            val h = hash[i].toInt() and 0xFF
            val t = target[i].toInt() and 0xFF
            if (h < t) return true
            if (h > t) return false
        }
        return true
    }
    
    private fun compareHashes(a: ByteArray, b: ByteArray): Int {
        for (i in a.indices.reversed()) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return 0
    }
}

/**
 * Ethash Implementation (Ethereum Classic style)
 * 
 * Memory-hard algorithm with DAG (Directed Acyclic Graph)
 */
class EthashAlgorithm : MiningAlgorithmImpl {
    override val algorithm = MiningAlgorithm.ETHASH
    
    companion object {
        const val HASH_BYTES = 64
        const val DATASET_BYTES_INIT = 1073741824L  // 1 GB
        const val DATASET_BYTES_GROWTH = 8388608L   // 8 MB
        const val CACHE_BYTES_INIT = 16777216L      // 16 MB
        const val CACHE_BYTES_GROWTH = 131072L      // 128 KB
        const val EPOCH_LENGTH = 30000
        const val MIX_BYTES = 128
        const val ACCESSES = 64
    }
    
    private var cache: ByteArray? = null
    private var dagSlice: ByteArray? = null
    private var currentEpoch: Int = -1
    
    private val _dagProgress = MutableStateFlow(0f)
    val dagProgress: StateFlow<Float> = _dagProgress
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        true
    }
    
    /**
     * Generate DAG cache for given epoch
     */
    suspend fun generateCache(epoch: Int): ByteArray = withContext(Dispatchers.Default) {
        val cacheSize = getCacheSize(epoch)
        val seed = getSeed(epoch)
        
        // Initialize cache with sequential hashing
        val cache = ByteArray(cacheSize.toInt())
        var hash = keccak512(seed)
        
        val numItems = cacheSize / HASH_BYTES
        for (i in 0 until numItems) {
            hash = keccak512(hash)
            System.arraycopy(hash, 0, cache, (i * HASH_BYTES).toInt(), HASH_BYTES)
            
            if (i % 1000 == 0L) {
                _dagProgress.value = i.toFloat() / numItems
            }
        }
        
        // Perform cache RandMemoHash passes
        for (round in 0 until 3) {
            for (i in 0 until numItems) {
                val offset = (i * HASH_BYTES).toInt()
                val srcOffset1 = (((i - 1 + numItems) % numItems) * HASH_BYTES).toInt()
                
                // XOR with random item
                val v = ByteBuffer.wrap(cache, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val srcOffset2 = ((v.toLong() and 0xFFFFFFFFL) % numItems * HASH_BYTES).toInt()
                
                val temp = ByteArray(HASH_BYTES)
                for (j in 0 until HASH_BYTES) {
                    temp[j] = (cache[srcOffset1 + j] xor cache[srcOffset2 + j])
                }
                
                val newHash = keccak512(temp)
                System.arraycopy(newHash, 0, cache, offset, HASH_BYTES)
            }
        }
        
        currentEpoch = epoch
        this@EthashAlgorithm.cache = cache
        _dagProgress.value = 1f
        
        cache
    }
    
    /**
     * Get seed hash for epoch
     */
    private fun getSeed(epoch: Int): ByteArray {
        var seed = ByteArray(32)
        for (i in 0 until epoch) {
            seed = keccak256(seed)
        }
        return seed
    }
    
    /**
     * Get cache size for epoch
     */
    private fun getCacheSize(epoch: Int): Long {
        var size = CACHE_BYTES_INIT + CACHE_BYTES_GROWTH * epoch
        size -= HASH_BYTES
        while (!isPrime(size / HASH_BYTES)) {
            size -= 2 * HASH_BYTES
        }
        return size
    }
    
    /**
     * Get full DAG size for epoch
     */
    private fun getDatasetSize(epoch: Int): Long {
        var size = DATASET_BYTES_INIT + DATASET_BYTES_GROWTH * epoch
        size -= MIX_BYTES
        while (!isPrime(size / MIX_BYTES)) {
            size -= 2 * MIX_BYTES
        }
        return size
    }
    
    private fun isPrime(n: Long): Boolean {
        if (n <= 1) return false
        if (n <= 3) return true
        if (n % 2 == 0L || n % 3 == 0L) return false
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2) == 0L) return false
            i += 6
        }
        return true
    }
    
    /**
     * Calculate DAG item from cache (light evaluation)
     */
    private fun calcDatasetItem(cache: ByteArray, index: Long): ByteArray {
        val cacheSize = cache.size / HASH_BYTES
        val mix = ByteArray(HASH_BYTES)
        
        // Initialize mix
        val offset = ((index % cacheSize) * HASH_BYTES).toInt()
        System.arraycopy(cache, offset, mix, 0, HASH_BYTES)
        
        // XOR with index
        ByteBuffer.wrap(mix, 0, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(index.toInt() xor mix[0].toInt())
        
        // Hash
        var mixHash = keccak512(mix)
        
        // Perform FNV mix
        for (j in 0 until 256) {
            val cacheIndex = fnv(index.toInt() xor j, mixHash[j % HASH_BYTES].toInt()) % cacheSize
            val cacheOffset = (cacheIndex * HASH_BYTES).toInt()
            
            for (k in 0 until HASH_BYTES) {
                mixHash[k] = (fnv(mixHash[k].toInt(), cache[cacheOffset + k].toInt()) and 0xFF).toByte()
            }
        }
        
        return keccak512(mixHash)
    }
    
    private fun fnv(v1: Int, v2: Int): Int {
        return ((v1 * 0x01000193) xor v2)
    }
    
    /**
     * Compute Ethash
     */
    override suspend fun compute(header: ByteArray, nonce: Long, target: ByteArray): HashResult {
        val cache = this.cache ?: throw IllegalStateException("Cache not initialized")
        
        // Initial hash
        val seedHash = keccak512(header + nonce.toLEBytes())
        
        // Initialize mix
        val mix = ByteArray(MIX_BYTES)
        for (i in 0 until MIX_BYTES / HASH_BYTES) {
            System.arraycopy(seedHash, 0, mix, i * HASH_BYTES, HASH_BYTES)
        }
        
        val datasetSize = getDatasetSize(currentEpoch)
        val numItems = datasetSize / HASH_BYTES
        
        // DAG accesses
        for (i in 0 until ACCESSES) {
            val p = fnv(i.toInt() xor seedHash[0].toInt(), mix[i % MIX_BYTES].toInt()) % (numItems / 2).toInt()
            
            for (j in 0 until MIX_BYTES / HASH_BYTES) {
                val dagItem = calcDatasetItem(cache, (p * 2 + j).toLong())
                for (k in 0 until HASH_BYTES) {
                    mix[j * HASH_BYTES + k] = (fnv(mix[j * HASH_BYTES + k].toInt(), dagItem[k].toInt()) and 0xFF).toByte()
                }
            }
        }
        
        // Compress mix
        val cmix = ByteArray(32)
        for (i in 0 until 32) {
            cmix[i] = fnv(
                fnv(fnv(mix[i * 4].toInt(), mix[i * 4 + 1].toInt()), mix[i * 4 + 2].toInt()),
                mix[i * 4 + 3].toInt()
            ).toByte()
        }
        
        // Final hash
        val hash = keccak256(seedHash + cmix)
        
        return HashResult(
            hash = hash,
            nonce = nonce,
            meetsTarget = compareHashToTarget(hash, target)
        )
    }
    
    override suspend fun computeBatch(
        header: ByteArray,
        startNonce: Long,
        count: Int,
        target: ByteArray
    ): BatchHashResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<HashResult>()
        var bestHash: HashResult? = null
        
        for (i in 0 until count) {
            val result = compute(header, startNonce + i, target)
            if (result.meetsTarget) {
                results.add(result)
            }
            if (bestHash == null || compareHashes(result.hash, bestHash.hash) < 0) {
                bestHash = result
            }
        }
        
        BatchHashResult(
            hashes = results,
            bestHash = bestHash,
            hashesComputed = count,
            computeTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    override suspend fun benchmark(durationMs: Long): BenchmarkResult {
        initialize()
        generateCache(0)  // Epoch 0 for benchmark
        
        val startTime = System.currentTimeMillis()
        var hashCount = 0L
        val testHeader = ByteArray(32) { it.toByte() }
        val testTarget = ByteArray(32) { 0xFF.toByte() }
        
        while (System.currentTimeMillis() - startTime < durationMs) {
            compute(testHeader, hashCount, testTarget)
            hashCount++
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        val hashRate = hashCount.toDouble() / (elapsed / 1000.0)
        
        return BenchmarkResult(
            algorithm = algorithm,
            hashRate = hashRate,
            powerEfficiency = hashRate / 5.0,  // Ethash is power intensive
            temperature = 0f,
            durationMs = elapsed,
            success = true
        )
    }
    
    override fun shutdown() {
        cache = null
        dagSlice = null
    }
    
    // Placeholder hash functions - in production use proper implementations
    private fun keccak256(input: ByteArray): ByteArray {
        // Use proper Keccak-256 implementation
        return MessageDigest.getInstance("SHA-256").digest(input)
    }
    
    private fun keccak512(input: ByteArray): ByteArray {
        // Use proper Keccak-512 implementation
        return MessageDigest.getInstance("SHA-512").digest(input)
    }
    
    private fun Long.toLEBytes(): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()
    }
    
    private fun compareHashToTarget(hash: ByteArray, target: ByteArray): Boolean {
        for (i in hash.indices.reversed()) {
            val h = hash[i].toInt() and 0xFF
            val t = target[i].toInt() and 0xFF
            if (h < t) return true
            if (h > t) return false
        }
        return true
    }
    
    private fun compareHashes(a: ByteArray, b: ByteArray): Int {
        for (i in a.indices.reversed()) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return 0
    }
}

/**
 * RandomX Implementation (Monero style)
 *
 * CPU-optimized memory-hard algorithm with random program execution
 */
class RandomXAlgorithm : MiningAlgorithmImpl {
    override val algorithm = MiningAlgorithm.RANDOMX
    
    companion object {
        init {
            System.loadLibrary("randomx_native")
        }
        
        const val SCRATCHPAD_SIZE = 2 * 1024 * 1024  // 2 MB
        const val DATASET_SIZE = 2 * 1024 * 1024 * 1024L  // 2 GB (full)
        const val DATASET_SIZE_LIGHT = 256 * 1024 * 1024L  // 256 MB (light)
    }
    
    // Native methods
    private external fun nativeInit(flags: Int): Long
    private external fun nativeCreateCache(handle: Long, seed: ByteArray): Boolean
    private external fun nativeInitDataset(handle: Long, threads: Int): Boolean
    private external fun nativeCalculateHash(handle: Long, input: ByteArray, nonce: Long): ByteArray?
    private external fun nativeDestroy(handle: Long)
    
    private var nativeHandle: Long = 0
    private var initialized = false
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            nativeHandle = nativeInit(0)  // 0 = default flags
            initialized = nativeHandle != 0L
            initialized
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun initializeWithSeed(seed: ByteArray, lightMode: Boolean = true): Boolean = withContext(Dispatchers.Default) {
        if (!initialized) {
            initialize()
        }
        
        if (!nativeCreateCache(nativeHandle, seed)) {
            return@withContext false
        }
        
        if (!lightMode) {
            // Full dataset initialization (very slow, ~1 minute)
            val threads = Runtime.getRuntime().availableProcessors()
            if (!nativeInitDataset(nativeHandle, threads)) {
                return@withContext false
            }
        }
        
        true
    }
    
    override suspend fun compute(header: ByteArray, nonce: Long, target: ByteArray): HashResult {
        if (!initialized) {
            throw IllegalStateException("RandomX not initialized")
        }
        
        val hash = nativeCalculateHash(nativeHandle, header, nonce)
            ?: throw RuntimeException("RandomX hash failed")
        
        return HashResult(
            hash = hash,
            nonce = nonce,
            meetsTarget = compareHashToTarget(hash, target)
        )
    }
    
    override suspend fun computeBatch(
        header: ByteArray,
        startNonce: Long,
        count: Int,
        target: ByteArray
    ): BatchHashResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<HashResult>()
        var bestHash: HashResult? = null
        
        for (i in 0 until count) {
            val result = compute(header, startNonce + i, target)
            if (result.meetsTarget) {
                results.add(result)
            }
            if (bestHash == null || compareHashes(result.hash, bestHash.hash) < 0) {
                bestHash = result
            }
        }
        
        BatchHashResult(
            hashes = results,
            bestHash = bestHash,
            hashesComputed = count,
            computeTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    override suspend fun benchmark(durationMs: Long): BenchmarkResult {
        val seed = ByteArray(32) { 0 }
        initializeWithSeed(seed, lightMode = true)
        
        val startTime = System.currentTimeMillis()
        var hashCount = 0L
        val testHeader = ByteArray(76) { it.toByte() }
        val testTarget = ByteArray(32) { 0xFF.toByte() }
        
        while (System.currentTimeMillis() - startTime < durationMs) {
            compute(testHeader, hashCount, testTarget)
            hashCount++
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        val hashRate = hashCount.toDouble() / (elapsed / 1000.0)
        
        return BenchmarkResult(
            algorithm = algorithm,
            hashRate = hashRate,
            powerEfficiency = hashRate / 3.0,
            temperature = 0f,
            durationMs = elapsed,
            success = true
        )
    }
    
    override fun shutdown() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
        initialized = false
    }
    
    private fun compareHashToTarget(hash: ByteArray, target: ByteArray): Boolean {
        for (i in hash.indices.reversed()) {
            val h = hash[i].toInt() and 0xFF
            val t = target[i].toInt() and 0xFF
            if (h < t) return true
            if (h > t) return false
        }
        return true
    }
    
    private fun compareHashes(a: ByteArray, b: ByteArray): Int {
        for (i in a.indices.reversed()) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return 0
    }
}

/**
 * KawPow Implementation (Ravencoin style)
 *
 * GPU-optimized Ethash variant with random program execution
 */
class KawPowAlgorithm : MiningAlgorithmImpl {
    override val algorithm = MiningAlgorithm.KAWPOW
    
    companion object {
        const val EPOCH_LENGTH = 7500
        const val PERIOD_LENGTH = 3
        const val MIX_BYTES = 256
        const val PROGPOW_LANES = 16
        const val PROGPOW_REGS = 32
        const val PROGPOW_DAG_LOADS = 4
        const val PROGPOW_CNT_DAG = 64
        const val PROGPOW_CNT_CACHE = 12
        const val PROGPOW_CNT_MATH = 20
    }
    
    private var ethash: EthashAlgorithm? = null
    
    override suspend fun initialize(): Boolean {
        ethash = EthashAlgorithm()
        return ethash?.initialize() ?: false
    }
    
    suspend fun setEpoch(epoch: Int) {
        ethash?.generateCache(epoch)
    }
    
    override suspend fun compute(header: ByteArray, nonce: Long, target: ByteArray): HashResult {
        // KawPow extends Ethash with additional ProgPoW computation
        // For this implementation, we use Ethash as base and add ProgPoW rounds
        
        val ethashResult = ethash?.compute(header, nonce, target)
            ?: throw IllegalStateException("Ethash not initialized")
        
        // ProgPoW additional mixing
        val mixHash = progpowMix(ethashResult.hash, nonce)
        val finalHash = keccak256(ethashResult.hash + mixHash)
        
        return HashResult(
            hash = finalHash,
            nonce = nonce,
            meetsTarget = compareHashToTarget(finalHash, target)
        )
    }
    
    private fun progpowMix(seedHash: ByteArray, nonce: Long): ByteArray {
        // Initialize lanes
        val laneResults = Array(PROGPOW_LANES) { ByteArray(32) }
        
        // Each lane processes independently
        for (lane in 0 until PROGPOW_LANES) {
            val laneNonce = (nonce shl 4) or lane.toLong()
            
            // Initialize with seed hash XOR lane
            val state = seedHash.copyOf()
            state[0] = (state[0].toInt() xor lane).toByte()
            
            // Perform random math operations
            var accum = 0
            for (round in 0 until PROGPOW_CNT_MATH) {
                val r = random(laneNonce + round)
                accum = when (r % 11) {
                    0 -> accum + state[r % 32].toInt()
                    1 -> accum * state[r % 32].toInt()
                    2 -> accum xor state[r % 32].toInt()
                    3 -> accum.rotateLeft(r % 31)
                    4 -> accum.rotateRight(r % 31)
                    5 -> accum and state[r % 32].toInt()
                    6 -> accum or state[r % 32].toInt()
                    7 -> accum - state[r % 32].toInt()
                    8 -> accum.inv()
                    9 -> (accum.toLong() * state[r % 32].toLong() shr 32).toInt()
                    else -> accum
                }
            }
            
            ByteBuffer.wrap(laneResults[lane]).order(ByteOrder.LITTLE_ENDIAN).putInt(accum)
        }
        
        // Merge lane results
        val result = ByteArray(32)
        for (i in 0 until 32) {
            var merged = 0
            for (lane in 0 until PROGPOW_LANES) {
                merged = merged xor laneResults[lane][i % laneResults[lane].size].toInt()
            }
            result[i] = merged.toByte()
        }
        
        return result
    }
    
    private fun random(seed: Long): Int {
        // Simple PRNG
        var x = seed xor (seed shr 12)
        x = x xor (x shl 25)
        x = x xor (x shr 27)
        return (x * 0x2545F4914F6CDD1DL).toInt() and 0x7FFFFFFF
    }
    
    private fun Int.rotateLeft(n: Int): Int = (this shl n) or (this ushr (32 - n))
    private fun Int.rotateRight(n: Int): Int = (this ushr n) or (this shl (32 - n))
    
    override suspend fun computeBatch(
        header: ByteArray,
        startNonce: Long,
        count: Int,
        target: ByteArray
    ): BatchHashResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<HashResult>()
        var bestHash: HashResult? = null
        
        for (i in 0 until count) {
            val result = compute(header, startNonce + i, target)
            if (result.meetsTarget) {
                results.add(result)
            }
            if (bestHash == null || compareHashes(result.hash, bestHash.hash) < 0) {
                bestHash = result
            }
        }
        
        BatchHashResult(
            hashes = results,
            bestHash = bestHash,
            hashesComputed = count,
            computeTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    override suspend fun benchmark(durationMs: Long): BenchmarkResult {
        initialize()
        setEpoch(0)
        
        val startTime = System.currentTimeMillis()
        var hashCount = 0L
        val testHeader = ByteArray(80) { it.toByte() }
        val testTarget = ByteArray(32) { 0xFF.toByte() }
        
        while (System.currentTimeMillis() - startTime < durationMs) {
            compute(testHeader, hashCount, testTarget)
            hashCount++
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        val hashRate = hashCount.toDouble() / (elapsed / 1000.0)
        
        return BenchmarkResult(
            algorithm = algorithm,
            hashRate = hashRate,
            powerEfficiency = hashRate / 4.0,
            temperature = 0f,
            durationMs = elapsed,
            success = true
        )
    }
    
    override fun shutdown() {
        ethash?.shutdown()
        ethash = null
    }
    
    private fun keccak256(input: ByteArray): ByteArray {
        return java.security.MessageDigest.getInstance("SHA-256").digest(input)
    }
    
    private fun compareHashToTarget(hash: ByteArray, target: ByteArray): Boolean {
        for (i in hash.indices.reversed()) {
            val h = hash[i].toInt() and 0xFF
            val t = target[i].toInt() and 0xFF
            if (h < t) return true
            if (h > t) return false
        }
        return true
    }
    
    private fun compareHashes(a: ByteArray, b: ByteArray): Int {
        for (i in a.indices.reversed()) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return 0
    }
}

/**
 * Algorithm Manager - handles algorithm selection and switching
 */
class AlgorithmManager {
    
    private val algorithms = mutableMapOf<MiningAlgorithm, MiningAlgorithmImpl>()
    private var currentAlgorithm: MiningAlgorithmImpl? = null
    
    suspend fun initialize(vararg algos: MiningAlgorithm) {
        for (algo in algos) {
            val impl = when (algo) {
                MiningAlgorithm.SHA256, MiningAlgorithm.SHA256D -> SHA256DAlgorithm()
                MiningAlgorithm.ETHASH, MiningAlgorithm.ETCHASH -> EthashAlgorithm()
                MiningAlgorithm.RANDOMX -> RandomXAlgorithm()
                MiningAlgorithm.KAWPOW -> KawPowAlgorithm()
                else -> null
            }
            
            impl?.let {
                if (it.initialize()) {
                    algorithms[algo] = it
                }
            }
        }
    }
    
    fun setAlgorithm(algo: MiningAlgorithm): Boolean {
        val impl = algorithms[algo] ?: return false
        currentAlgorithm = impl
        return true
    }
    
    fun getCurrentAlgorithm(): MiningAlgorithmImpl? = currentAlgorithm
    
    suspend fun benchmarkAll(): Map<MiningAlgorithm, BenchmarkResult> {
        return algorithms.mapValues { (_, impl) ->
            impl.benchmark()
        }
    }
    
    fun shutdown() {
        algorithms.values.forEach { it.shutdown() }
        algorithms.clear()
        currentAlgorithm = null
    }
}
