package com.meetmyartist.miner.mining

import android.util.Log

/**
 * Native mining library providing optimized cryptocurrency hashing algorithms.
 * Uses JNI to call C++ implementations for better performance.
 */
object NativeMiner {
    
    private const val TAG = "NativeMiner"
    private var isLoaded = false
    
    init {
        try {
            System.loadLibrary("miner_native")
            isLoaded = true
            Log.i(TAG, "Native mining library loaded: ${getVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
            isLoaded = false
        }
    }
    
    /**
     * Check if native library is loaded
     */
    fun isNativeAvailable(): Boolean = isLoaded
    
    /**
     * Get native library version
     */
    external fun getVersion(): String
    
    /**
     * SHA256 hash (single round)
     */
    external fun sha256(input: ByteArray): ByteArray
    
    /**
     * Double SHA256 hash (used by Bitcoin)
     */
    external fun sha256d(input: ByteArray): ByteArray
    
    /**
     * Mine SHA256d with nonce range
     * @param blockHeader 80-byte block header
     * @param target 32-byte target (difficulty)
     * @param startNonce starting nonce value
     * @param endNonce ending nonce value
     * @param hashCountOut array to receive hash count performed
     * @return found nonce or -1 if not found
     */
    external fun mineSha256d(
        blockHeader: ByteArray,
        target: ByteArray,
        startNonce: Long,
        endNonce: Long,
        hashCountOut: LongArray
    ): Long
    
    /**
     * Blake3 hash (fast, modern algorithm)
     */
    external fun blake3(input: ByteArray): ByteArray
    
    /**
     * Mine Blake3 with nonce range
     * @param blockData block data to hash
     * @param difficulty difficulty in bits (leading zeros required)
     * @param startNonce starting nonce
     * @param endNonce ending nonce
     * @param hashCountOut array to receive hash count
     * @return found nonce or -1
     */
    external fun mineBlake3(
        blockData: ByteArray,
        difficulty: Int,
        startNonce: Long,
        endNonce: Long,
        hashCountOut: LongArray
    ): Long
    
    /**
     * Scrypt hash (memory-hard, used by Litecoin)
     * @param input data to hash
     * @param n CPU/memory cost parameter (e.g., 1024 for Litecoin)
     * @param r block size parameter (e.g., 1)
     * @param p parallelization parameter (e.g., 1)
     */
    external fun scrypt(input: ByteArray, n: Int, r: Int, p: Int): ByteArray
    
    /**
     * RandomX light mode hash (used by Monero)
     * Light mode uses significantly less memory but is slower.
     * @param input data to hash
     * @param key key derived from blockchain data
     */
    external fun randomxLight(input: ByteArray, key: ByteArray): ByteArray
    
    /**
     * Benchmark SHA256d hashrate
     * @param durationMs duration to run benchmark in milliseconds
     * @return hashes per second
     */
    external fun benchmarkSha256d(durationMs: Int): Double
    
    // Fallback Kotlin implementations for when native is unavailable
    
    /**
     * Kotlin fallback for SHA256 using MessageDigest
     */
    fun sha256Fallback(input: ByteArray): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }
    
    /**
     * Kotlin fallback for double SHA256
     */
    fun sha256dFallback(input: ByteArray): ByteArray {
        return sha256Fallback(sha256Fallback(input))
    }
    
    /**
     * Smart hash function that uses native if available, fallback otherwise
     */
    fun hash(algorithm: String, input: ByteArray): ByteArray {
        return when {
            algorithm.equals("SHA256", ignoreCase = true) -> {
                if (isLoaded) sha256(input) else sha256Fallback(input)
            }
            algorithm.equals("SHA256D", ignoreCase = true) -> {
                if (isLoaded) sha256d(input) else sha256dFallback(input)
            }
            algorithm.equals("BLAKE3", ignoreCase = true) -> {
                if (isLoaded) blake3(input) 
                else throw UnsupportedOperationException("Blake3 requires native library")
            }
            algorithm.equals("SCRYPT", ignoreCase = true) -> {
                if (isLoaded) scrypt(input, 1024, 1, 1)
                else throw UnsupportedOperationException("Scrypt requires native library")
            }
            algorithm.equals("RANDOMX", ignoreCase = true) -> {
                if (isLoaded) randomxLight(input, input) // Use input as key for simplicity
                else throw UnsupportedOperationException("RandomX requires native library")
            }
            else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
        }
    }
    
    /**
     * Get benchmark results for all supported algorithms
     */
    fun runBenchmarks(durationMs: Int = 2000): Map<String, Double> {
        if (!isLoaded) {
            Log.w(TAG, "Native library not loaded, cannot run benchmarks")
            return emptyMap()
        }
        
        val results = mutableMapOf<String, Double>()
        
        try {
            results["SHA256D"] = benchmarkSha256d(durationMs)
            Log.i(TAG, "SHA256D benchmark: ${results["SHA256D"]} H/s")
        } catch (e: Exception) {
            Log.e(TAG, "SHA256D benchmark failed: ${e.message}")
        }
        
        // Add more benchmarks as needed
        
        return results
    }
}
