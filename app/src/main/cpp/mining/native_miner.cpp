#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>
#include "sha256.h"
#include "randomx_light.h"
#include "blake3.h"
#include "scrypt.h"

#define LOG_TAG "NativeMiner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// SHA256 double hash (used by Bitcoin)
JNIEXPORT jbyteArray JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_sha256d(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input) {
    
    jsize inputLen = env->GetArrayLength(input);
    jbyte *inputBytes = env->GetByteArrayElements(input, nullptr);
    
    uint8_t hash1[32];
    uint8_t hash2[32];
    
    // First SHA256
    sha256_hash((const uint8_t*)inputBytes, inputLen, hash1);
    // Second SHA256
    sha256_hash(hash1, 32, hash2);
    
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (jbyte*)hash2);
    
    return result;
}

// Single SHA256 hash
JNIEXPORT jbyteArray JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_sha256(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input) {
    
    jsize inputLen = env->GetArrayLength(input);
    jbyte *inputBytes = env->GetByteArrayElements(input, nullptr);
    
    uint8_t hash[32];
    sha256_hash((const uint8_t*)inputBytes, inputLen, hash);
    
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (jbyte*)hash);
    
    return result;
}

// Mine SHA256d with nonce range
JNIEXPORT jlong JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_mineSha256d(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray blockHeader,
        jbyteArray target,
        jlong startNonce,
        jlong endNonce,
        jlongArray hashCountOut) {
    
    jsize headerLen = env->GetArrayLength(blockHeader);
    jbyte *headerBytes = env->GetByteArrayElements(blockHeader, nullptr);
    
    jbyte *targetBytes = env->GetByteArrayElements(target, nullptr);
    
    uint8_t header[80];
    memcpy(header, headerBytes, headerLen < 80 ? headerLen : 80);
    
    uint8_t hash1[32];
    uint8_t hash2[32];
    long hashCount = 0;
    
    for (uint32_t nonce = (uint32_t)startNonce; nonce <= (uint32_t)endNonce; nonce++) {
        // Insert nonce at bytes 76-79 (little endian)
        header[76] = (uint8_t)(nonce & 0xFF);
        header[77] = (uint8_t)((nonce >> 8) & 0xFF);
        header[78] = (uint8_t)((nonce >> 16) & 0xFF);
        header[79] = (uint8_t)((nonce >> 24) & 0xFF);
        
        // Double SHA256
        sha256_hash(header, 80, hash1);
        sha256_hash(hash1, 32, hash2);
        hashCount++;
        
        // Check if hash meets target (compare from end, big-endian style)
        bool meetsTarget = true;
        for (int i = 31; i >= 0; i--) {
            if (hash2[i] < (uint8_t)targetBytes[i]) {
                // Found valid nonce!
                env->ReleaseByteArrayElements(blockHeader, headerBytes, 0);
                env->ReleaseByteArrayElements(target, targetBytes, 0);
                
                jlong count = hashCount;
                env->SetLongArrayRegion(hashCountOut, 0, 1, &count);
                return (jlong)nonce;
            } else if (hash2[i] > (uint8_t)targetBytes[i]) {
                meetsTarget = false;
                break;
            }
        }
    }
    
    env->ReleaseByteArrayElements(blockHeader, headerBytes, 0);
    env->ReleaseByteArrayElements(target, targetBytes, 0);
    
    jlong count = hashCount;
    env->SetLongArrayRegion(hashCountOut, 0, 1, &count);
    
    return -1; // No valid nonce found
}

// Blake3 hash (fast, modern algorithm)
JNIEXPORT jbyteArray JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_blake3(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input) {
    
    jsize inputLen = env->GetArrayLength(input);
    jbyte *inputBytes = env->GetByteArrayElements(input, nullptr);
    
    uint8_t hash[32];
    blake3_hash((const uint8_t*)inputBytes, inputLen, hash);
    
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (jbyte*)hash);
    
    return result;
}

// Mine Blake3 with nonce range
JNIEXPORT jlong JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_mineBlake3(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray blockData,
        jint difficulty,
        jlong startNonce,
        jlong endNonce,
        jlongArray hashCountOut) {
    
    jsize dataLen = env->GetArrayLength(blockData);
    jbyte *dataBytes = env->GetByteArrayElements(blockData, nullptr);
    
    // Calculate required leading zeros from difficulty
    int requiredZeros = difficulty / 8;
    int remainingBits = difficulty % 8;
    
    uint8_t data[256];
    int actualLen = dataLen < 248 ? dataLen : 248;
    memcpy(data, dataBytes, actualLen);
    
    uint8_t hash[32];
    long hashCount = 0;
    
    for (uint64_t nonce = startNonce; nonce <= (uint64_t)endNonce; nonce++) {
        // Append nonce (8 bytes, little endian)
        for (int i = 0; i < 8; i++) {
            data[actualLen + i] = (uint8_t)((nonce >> (i * 8)) & 0xFF);
        }
        
        blake3_hash(data, actualLen + 8, hash);
        hashCount++;
        
        // Check leading zeros
        bool valid = true;
        for (int i = 0; i < requiredZeros && valid; i++) {
            if (hash[i] != 0) valid = false;
        }
        
        if (valid && remainingBits > 0) {
            uint8_t mask = 0xFF << (8 - remainingBits);
            if ((hash[requiredZeros] & mask) != 0) valid = false;
        }
        
        if (valid) {
            env->ReleaseByteArrayElements(blockData, dataBytes, 0);
            jlong count = hashCount;
            env->SetLongArrayRegion(hashCountOut, 0, 1, &count);
            return (jlong)nonce;
        }
    }
    
    env->ReleaseByteArrayElements(blockData, dataBytes, 0);
    jlong count = hashCount;
    env->SetLongArrayRegion(hashCountOut, 0, 1, &count);
    
    return -1;
}

// Scrypt hash (for Litecoin)
JNIEXPORT jbyteArray JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_scrypt(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input,
        jint n,
        jint r,
        jint p) {
    
    jsize inputLen = env->GetArrayLength(input);
    jbyte *inputBytes = env->GetByteArrayElements(input, nullptr);
    
    uint8_t hash[32];
    scrypt_hash((const uint8_t*)inputBytes, inputLen, 
                (const uint8_t*)inputBytes, inputLen,
                n, r, p, hash, 32);
    
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (jbyte*)hash);
    
    return result;
}

// RandomX light mode (CPU mining for Monero) - simplified version
JNIEXPORT jbyteArray JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_randomxLight(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input,
        jbyteArray key) {
    
    jsize inputLen = env->GetArrayLength(input);
    jbyte *inputBytes = env->GetByteArrayElements(input, nullptr);
    
    jsize keyLen = env->GetArrayLength(key);
    jbyte *keyBytes = env->GetByteArrayElements(key, nullptr);
    
    uint8_t hash[32];
    randomx_light_hash((const uint8_t*)inputBytes, inputLen,
                       (const uint8_t*)keyBytes, keyLen, hash);
    
    env->ReleaseByteArrayElements(input, inputBytes, 0);
    env->ReleaseByteArrayElements(key, keyBytes, 0);
    
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (jbyte*)hash);
    
    return result;
}

// Get native library version
JNIEXPORT jstring JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_getVersion(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF("1.0.0-native");
}

// Benchmark: measure hash rate for algorithm
JNIEXPORT jdouble JNICALL
Java_com_meetmyartist_miner_mining_NativeMiner_benchmarkSha256d(
        JNIEnv *env,
        jobject /* this */,
        jint durationMs) {
    
    uint8_t data[80];
    uint8_t hash1[32];
    uint8_t hash2[32];
    
    // Fill with random-ish data
    for (int i = 0; i < 80; i++) {
        data[i] = (uint8_t)(i * 7 + 13);
    }
    
    auto start = std::chrono::high_resolution_clock::now();
    long hashes = 0;
    
    while (true) {
        // Increment nonce
        data[76]++;
        if (data[76] == 0) data[77]++;
        
        sha256_hash(data, 80, hash1);
        sha256_hash(hash1, 32, hash2);
        hashes++;
        
        // Check elapsed time every 10000 hashes
        if (hashes % 10000 == 0) {
            auto now = std::chrono::high_resolution_clock::now();
            auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - start).count();
            if (elapsed >= durationMs) break;
        }
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    
    return (double)hashes / ((double)duration / 1000.0);
}

} // extern "C"
