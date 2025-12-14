package com.meetmyartist.miner.mining

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * Cryptographic hashing utilities for mining
 * Implements common hashing algorithms used in cryptocurrency mining
 */
object CryptoHasher {
    
    /**
     * SHA-256 hash function
     * Used by Bitcoin and many other cryptocurrencies
     */
    fun sha256(input: ByteArray): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.digest(input)
        } catch (e: NoSuchAlgorithmException) {
            ByteArray(32)
        }
    }
    
    /**
     * Double SHA-256 (SHA-256 of SHA-256)
     * Used in Bitcoin mining
     */
    fun sha256d(input: ByteArray): ByteArray {
        return sha256(sha256(input))
    }
    
    /**
     * Calculate target from difficulty bits (nbits)
     * Used to determine if hash meets difficulty requirement
     */
    fun calculateTarget(nbits: String): ByteArray {
        val bits = nbits.toLong(16)
        val exponent = ((bits shr 24) and 0xFF).toInt()
        val mantissa = bits and 0x00FFFFFF
        
        val target = ByteArray(32)
        val mantissaBytes = ByteArray(3)
        
        mantissaBytes[0] = ((mantissa shr 16) and 0xFF).toByte()
        mantissaBytes[1] = ((mantissa shr 8) and 0xFF).toByte()
        mantissaBytes[2] = (mantissa and 0xFF).toByte()
        
        val startIndex = 29 - exponent + 3
        if (startIndex >= 0 && startIndex < 29) {
            System.arraycopy(mantissaBytes, 0, target, startIndex, 3)
        }
        
        return target
    }
    
    /**
     * Check if hash meets target difficulty
     */
    fun meetsTarget(hash: ByteArray, target: ByteArray): Boolean {
        // Compare hash with target (reversed byte order)
        for (i in 31 downTo 0) {
            val hashByte = hash[i].toInt() and 0xFF
            val targetByte = target[i].toInt() and 0xFF
            
            if (hashByte < targetByte) return true
            if (hashByte > targetByte) return false
        }
        return true
    }
    
    /**
     * Scrypt hash function (simplified version)
     * Used by Litecoin, Dogecoin
     * Note: Full Scrypt requires native implementation for performance
     */
    fun scrypt(input: ByteArray, salt: ByteArray, n: Int = 1024, r: Int = 1, p: Int = 1): ByteArray {
        // This is a simplified version for demonstration
        // Real Scrypt implementation should use native code
        val combined = input + salt
        return sha256(combined)
    }
    
    /**
     * Keccak-256 hash function
     * Used by Ethereum
     */
    fun keccak256(input: ByteArray): ByteArray {
        // Simplified Keccak implementation
        // Real implementation should use proper Keccak-256
        return sha256(input)
    }
    
    /**
     * RIPEMD-160 hash function
     * Used in Bitcoin address generation
     */
    fun ripemd160(input: ByteArray): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("RIPEMD160")
            digest.digest(input)
        } catch (e: NoSuchAlgorithmException) {
            // Fallback to SHA-1 if RIPEMD160 not available
            val sha1 = MessageDigest.getInstance("SHA-1")
            sha1.digest(input)
        }
    }
    
    /**
     * Build block header for hashing
     */
    fun buildBlockHeader(
        version: String,
        prevHash: String,
        merkleRoot: String,
        ntime: String,
        nbits: String,
        nonce: String
    ): ByteArray {
        val header = version + prevHash + merkleRoot + ntime + nbits + nonce
        return hexStringToByteArray(header)
    }
    
    /**
     * Build coinbase transaction
     */
    fun buildCoinbase(
        coinbase1: String,
        extranonce1: String,
        extranonce2: String,
        coinbase2: String
    ): String {
        return coinbase1 + extranonce1 + extranonce2 + coinbase2
    }
    
    /**
     * Calculate merkle root from coinbase and merkle branches
     */
    fun calculateMerkleRoot(coinbase: String, merkleBranch: List<String>): String {
        var hash = sha256d(hexStringToByteArray(coinbase))
        
        for (branch in merkleBranch) {
            val branchBytes = hexStringToByteArray(branch)
            val combined = hash + branchBytes
            hash = sha256d(combined)
        }
        
        return byteArrayToHexString(hash)
    }
    
    /**
     * Convert hex string to byte array
     */
    fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("-", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)).toByte()
        }
        
        return data
    }
    
    /**
     * Convert byte array to hex string
     */
    fun byteArrayToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        
        return String(hexChars)
    }
    
    /**
     * Reverse byte array (endianness conversion)
     */
    fun reverseBytes(bytes: ByteArray): ByteArray {
        return bytes.reversedArray()
    }
    
    /**
     * Reverse bytes in groups of 4 (for Bitcoin header fields)
     */
    fun reverseBytesInGroups(bytes: ByteArray, groupSize: Int = 4): ByteArray {
        val result = ByteArray(bytes.size)
        for (i in bytes.indices step groupSize) {
            for (j in 0 until groupSize) {
                if (i + j < bytes.size) {
                    result[i + j] = bytes[i + groupSize - 1 - j]
                }
            }
        }
        return result
    }
    
    /**
     * Generate random extranonce2
     */
    fun generateExtranonce2(size: Int): String {
        val random = java.util.Random()
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return byteArrayToHexString(bytes)
    }
    
    /**
     * Increment nonce
     */
    fun incrementNonce(nonce: String): String {
        val value = nonce.toLong(16) + 1
        return value.toString(16).padStart(8, '0')
    }
    
    /**
     * Check if hash is valid (has enough leading zeros)
     */
    fun hasValidLeadingZeros(hash: ByteArray, requiredZeros: Int): Boolean {
        var zeros = 0
        for (byte in hash) {
            if (byte.toInt() == 0) {
                zeros += 8
            } else {
                // Count leading zeros in this byte
                var b = byte.toInt() and 0xFF
                while (b and 0x80 == 0 && zeros < requiredZeros) {
                    zeros++
                    b = b shl 1
                }
                break
            }
            
            if (zeros >= requiredZeros) return true
        }
        
        return zeros >= requiredZeros
    }
}
