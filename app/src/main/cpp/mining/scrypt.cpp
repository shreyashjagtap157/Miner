/**
 * Scrypt implementation for Litecoin mining
 * Simplified version suitable for mobile devices
 */

#include "scrypt.h"
#include "sha256.h"
#include <cstring>
#include <cstdlib>

// HMAC-SHA256
static void hmac_sha256(const uint8_t* key, size_t keyLen,
                        const uint8_t* data, size_t dataLen,
                        uint8_t* mac) {
    uint8_t k_ipad[64];
    uint8_t k_opad[64];
    uint8_t tk[32];
    
    // If key is longer than 64 bytes, hash it
    if (keyLen > 64) {
        sha256_hash(key, keyLen, tk);
        key = tk;
        keyLen = 32;
    }
    
    // Prepare padded keys
    memset(k_ipad, 0x36, 64);
    memset(k_opad, 0x5c, 64);
    
    for (size_t i = 0; i < keyLen; i++) {
        k_ipad[i] ^= key[i];
        k_opad[i] ^= key[i];
    }
    
    // Inner hash: SHA256(k_ipad || data)
    uint8_t inner[64 + 1024]; // Assuming data < 1024
    memcpy(inner, k_ipad, 64);
    memcpy(inner + 64, data, dataLen);
    
    uint8_t innerHash[32];
    sha256_hash(inner, 64 + dataLen, innerHash);
    
    // Outer hash: SHA256(k_opad || inner_hash)
    uint8_t outer[96];
    memcpy(outer, k_opad, 64);
    memcpy(outer + 64, innerHash, 32);
    
    sha256_hash(outer, 96, mac);
}

// PBKDF2-HMAC-SHA256
static void pbkdf2_sha256(const uint8_t* password, size_t passwordLen,
                          const uint8_t* salt, size_t saltLen,
                          int iterations, uint8_t* output, size_t outputLen) {
    uint8_t U[32];
    uint8_t T[32];
    uint8_t block[256]; // salt + 4 bytes for block number
    
    size_t outputOffset = 0;
    int blockNum = 1;
    
    while (outputOffset < outputLen) {
        // Prepare block: salt || INT(blockNum)
        memcpy(block, salt, saltLen);
        block[saltLen] = (uint8_t)(blockNum >> 24);
        block[saltLen + 1] = (uint8_t)(blockNum >> 16);
        block[saltLen + 2] = (uint8_t)(blockNum >> 8);
        block[saltLen + 3] = (uint8_t)blockNum;
        
        // U_1 = HMAC(password, salt || INT(blockNum))
        hmac_sha256(password, passwordLen, block, saltLen + 4, U);
        memcpy(T, U, 32);
        
        // U_2 to U_iterations
        for (int i = 1; i < iterations; i++) {
            hmac_sha256(password, passwordLen, U, 32, U);
            for (int j = 0; j < 32; j++) {
                T[j] ^= U[j];
            }
        }
        
        // Copy to output
        size_t copyLen = (outputLen - outputOffset) < 32 ? (outputLen - outputOffset) : 32;
        memcpy(output + outputOffset, T, copyLen);
        
        outputOffset += 32;
        blockNum++;
    }
}

// Salsa20/8 core
static void salsa20_8(uint32_t B[16]) {
    uint32_t x[16];
    memcpy(x, B, 64);
    
    for (int i = 0; i < 8; i += 2) {
        #define R(a, b) (((a) << (b)) | ((a) >> (32 - (b))))
        
        x[ 4] ^= R(x[ 0] + x[12],  7);
        x[ 8] ^= R(x[ 4] + x[ 0],  9);
        x[12] ^= R(x[ 8] + x[ 4], 13);
        x[ 0] ^= R(x[12] + x[ 8], 18);
        
        x[ 9] ^= R(x[ 5] + x[ 1],  7);
        x[13] ^= R(x[ 9] + x[ 5],  9);
        x[ 1] ^= R(x[13] + x[ 9], 13);
        x[ 5] ^= R(x[ 1] + x[13], 18);
        
        x[14] ^= R(x[10] + x[ 6],  7);
        x[ 2] ^= R(x[14] + x[10],  9);
        x[ 6] ^= R(x[ 2] + x[14], 13);
        x[10] ^= R(x[ 6] + x[ 2], 18);
        
        x[ 3] ^= R(x[15] + x[11],  7);
        x[ 7] ^= R(x[ 3] + x[15],  9);
        x[11] ^= R(x[ 7] + x[ 3], 13);
        x[15] ^= R(x[11] + x[ 7], 18);
        
        x[ 1] ^= R(x[ 0] + x[ 3],  7);
        x[ 2] ^= R(x[ 1] + x[ 0],  9);
        x[ 3] ^= R(x[ 2] + x[ 1], 13);
        x[ 0] ^= R(x[ 3] + x[ 2], 18);
        
        x[ 6] ^= R(x[ 5] + x[ 4],  7);
        x[ 7] ^= R(x[ 6] + x[ 5],  9);
        x[ 4] ^= R(x[ 7] + x[ 6], 13);
        x[ 5] ^= R(x[ 4] + x[ 7], 18);
        
        x[11] ^= R(x[10] + x[ 9],  7);
        x[ 8] ^= R(x[11] + x[10],  9);
        x[ 9] ^= R(x[ 8] + x[11], 13);
        x[10] ^= R(x[ 9] + x[ 8], 18);
        
        x[12] ^= R(x[15] + x[14],  7);
        x[13] ^= R(x[12] + x[15],  9);
        x[14] ^= R(x[13] + x[12], 13);
        x[15] ^= R(x[14] + x[13], 18);
        
        #undef R
    }
    
    for (int i = 0; i < 16; i++) {
        B[i] += x[i];
    }
}

// BlockMix for Scrypt
static void scrypt_block_mix(uint32_t* B, uint32_t* Y, int r) {
    uint32_t X[16];
    memcpy(X, B + (2 * r - 1) * 16, 64);
    
    for (int i = 0; i < 2 * r; i++) {
        for (int j = 0; j < 16; j++) {
            X[j] ^= B[i * 16 + j];
        }
        salsa20_8(X);
        memcpy(Y + i * 16, X, 64);
    }
    
    // Rearrange: even blocks, then odd blocks
    for (int i = 0; i < r; i++) {
        memcpy(B + i * 16, Y + (2 * i) * 16, 64);
        memcpy(B + (r + i) * 16, Y + (2 * i + 1) * 16, 64);
    }
}

// ROMix for Scrypt
static void scrypt_romix(uint32_t* B, int r, int N, uint32_t* V, uint32_t* XY) {
    int blockSize = 32 * r;
    uint32_t* X = XY;
    uint32_t* Y = XY + blockSize;
    
    memcpy(X, B, blockSize * sizeof(uint32_t));
    
    // Step 1: fill V with copies of X
    for (int i = 0; i < N; i++) {
        memcpy(V + i * blockSize, X, blockSize * sizeof(uint32_t));
        scrypt_block_mix(X, Y, r);
    }
    
    // Step 2: mix X with random blocks from V
    for (int i = 0; i < N; i++) {
        int j = X[(2 * r - 1) * 16] % N;
        for (int k = 0; k < blockSize; k++) {
            X[k] ^= V[j * blockSize + k];
        }
        scrypt_block_mix(X, Y, r);
    }
    
    memcpy(B, X, blockSize * sizeof(uint32_t));
}

void scrypt_hash(const uint8_t* password, size_t passwordLen,
                 const uint8_t* salt, size_t saltLen,
                 int N, int r, int p,
                 uint8_t* output, size_t outputLen) {
    
    // For Litecoin: N=1024, r=1, p=1
    int blockSize = 128 * r;
    
    // Derive initial key using PBKDF2
    uint8_t* B = (uint8_t*)malloc(blockSize * p);
    pbkdf2_sha256(password, passwordLen, salt, saltLen, 1, B, blockSize * p);
    
    // Allocate memory for ROMix
    uint32_t* V = (uint32_t*)malloc(blockSize * N);
    uint32_t* XY = (uint32_t*)malloc(blockSize * 2);
    
    // Apply ROMix to each block
    for (int i = 0; i < p; i++) {
        scrypt_romix((uint32_t*)(B + i * blockSize), r, N, V, XY);
    }
    
    // Derive output using PBKDF2
    pbkdf2_sha256(password, passwordLen, B, blockSize * p, 1, output, outputLen);
    
    free(B);
    free(V);
    free(XY);
}
