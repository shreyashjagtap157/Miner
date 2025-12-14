/**
 * Blake3 implementation for cryptocurrency mining
 * Simplified version focusing on single-threaded hashing
 */

#include "blake3.h"
#include <cstring>

// Blake3 constants
static const uint32_t IV[8] = {
    0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A,
    0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19
};

static const uint8_t MSG_SCHEDULE[7][16] = {
    {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
    {2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8},
    {3, 4, 10, 12, 13, 2, 7, 14, 6, 5, 9, 0, 11, 15, 8, 1},
    {10, 7, 12, 9, 14, 3, 13, 15, 4, 0, 11, 2, 5, 8, 1, 6},
    {12, 13, 9, 11, 15, 10, 14, 8, 7, 2, 5, 3, 0, 1, 6, 4},
    {9, 14, 11, 5, 8, 12, 15, 1, 13, 3, 0, 10, 2, 6, 4, 7},
    {11, 15, 5, 0, 1, 9, 8, 6, 14, 10, 2, 12, 3, 4, 7, 13}
};

#define ROTR32(x, n) (((x) >> (n)) | ((x) << (32 - (n))))

static inline void g(uint32_t state[16], int a, int b, int c, int d, uint32_t mx, uint32_t my) {
    state[a] = state[a] + state[b] + mx;
    state[d] = ROTR32(state[d] ^ state[a], 16);
    state[c] = state[c] + state[d];
    state[b] = ROTR32(state[b] ^ state[c], 12);
    state[a] = state[a] + state[b] + my;
    state[d] = ROTR32(state[d] ^ state[a], 8);
    state[c] = state[c] + state[d];
    state[b] = ROTR32(state[b] ^ state[c], 7);
}

static void blake3_compress(const uint32_t chaining[8], const uint8_t block[64],
                            uint64_t counter, uint32_t blockLen, uint32_t flags,
                            uint32_t out[16]) {
    uint32_t state[16];
    uint32_t m[16];
    
    // Load message block as little-endian uint32s
    for (int i = 0; i < 16; i++) {
        m[i] = ((uint32_t)block[i*4]) |
               ((uint32_t)block[i*4+1] << 8) |
               ((uint32_t)block[i*4+2] << 16) |
               ((uint32_t)block[i*4+3] << 24);
    }
    
    // Initialize state
    for (int i = 0; i < 8; i++) {
        state[i] = chaining[i];
    }
    state[8] = IV[0];
    state[9] = IV[1];
    state[10] = IV[2];
    state[11] = IV[3];
    state[12] = (uint32_t)counter;
    state[13] = (uint32_t)(counter >> 32);
    state[14] = blockLen;
    state[15] = flags;
    
    // 7 rounds
    for (int round = 0; round < 7; round++) {
        const uint8_t* schedule = MSG_SCHEDULE[round];
        
        // Column step
        g(state, 0, 4, 8, 12, m[schedule[0]], m[schedule[1]]);
        g(state, 1, 5, 9, 13, m[schedule[2]], m[schedule[3]]);
        g(state, 2, 6, 10, 14, m[schedule[4]], m[schedule[5]]);
        g(state, 3, 7, 11, 15, m[schedule[6]], m[schedule[7]]);
        
        // Diagonal step
        g(state, 0, 5, 10, 15, m[schedule[8]], m[schedule[9]]);
        g(state, 1, 6, 11, 12, m[schedule[10]], m[schedule[11]]);
        g(state, 2, 7, 8, 13, m[schedule[12]], m[schedule[13]]);
        g(state, 3, 4, 9, 14, m[schedule[14]], m[schedule[15]]);
    }
    
    // XOR with chaining value
    for (int i = 0; i < 8; i++) {
        state[i] ^= state[i + 8];
        state[i + 8] ^= chaining[i];
    }
    
    memcpy(out, state, 64);
}

void blake3_hash(const uint8_t* data, size_t len, uint8_t* hash) {
    uint32_t chaining[8];
    uint8_t block[64];
    uint32_t out[16];
    
    memcpy(chaining, IV, sizeof(IV));
    
    // Flags
    const uint32_t CHUNK_START = 1;
    const uint32_t CHUNK_END = 2;
    const uint32_t ROOT = 8;
    
    if (len == 0) {
        // Empty input
        memset(block, 0, 64);
        blake3_compress(chaining, block, 0, 0, CHUNK_START | CHUNK_END | ROOT, out);
    } else if (len <= 64) {
        // Single block
        memset(block, 0, 64);
        memcpy(block, data, len);
        blake3_compress(chaining, block, 0, (uint32_t)len, CHUNK_START | CHUNK_END | ROOT, out);
    } else {
        // Multiple blocks - simplified: just process first and last
        // For real mining, implement full tree structure
        
        // First block
        memcpy(block, data, 64);
        blake3_compress(chaining, block, 0, 64, CHUNK_START, out);
        memcpy(chaining, out, 32);
        
        // Remaining data
        data += 64;
        len -= 64;
        
        while (len > 64) {
            memcpy(block, data, 64);
            blake3_compress(chaining, block, 0, 64, 0, out);
            memcpy(chaining, out, 32);
            data += 64;
            len -= 64;
        }
        
        // Final block
        memset(block, 0, 64);
        memcpy(block, data, len);
        blake3_compress(chaining, block, 0, (uint32_t)len, CHUNK_END | ROOT, out);
    }
    
    // Output hash (first 32 bytes, little-endian)
    for (int i = 0; i < 8; i++) {
        hash[i*4]     = (uint8_t)(out[i]);
        hash[i*4 + 1] = (uint8_t)(out[i] >> 8);
        hash[i*4 + 2] = (uint8_t)(out[i] >> 16);
        hash[i*4 + 3] = (uint8_t)(out[i] >> 24);
    }
}
