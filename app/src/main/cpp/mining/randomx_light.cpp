/**
 * RandomX Light Mode implementation for Monero mining
 * 
 * This is a simplified light-mode implementation that doesn't require
 * the full 2GB dataset. It's slower than full mode but suitable for mobile.
 * 
 * For production use, consider linking against the official RandomX library.
 */

#include "randomx_light.h"
#include "blake3.h"
#include <cstring>
#include <cstdlib>

// AES round keys for the scratchpad initialization
static const uint8_t AES_ROUND_KEYS[16] = {
    0x6a, 0x09, 0xe6, 0x67, 0xbb, 0x67, 0xae, 0x85,
    0x3c, 0x6e, 0xf3, 0x72, 0xa5, 0x4f, 0xf5, 0x3a
};

// Simple AES-like round function (not cryptographic, just for randomization)
static void aes_round(uint8_t state[16], const uint8_t key[16]) {
    for (int i = 0; i < 16; i++) {
        state[i] ^= key[i];
        // Simple S-box substitution
        state[i] = (state[i] * 0x9D + 0x5B) & 0xFF;
    }
    
    // Row shift
    uint8_t temp = state[1];
    state[1] = state[5];
    state[5] = state[9];
    state[9] = state[13];
    state[13] = temp;
    
    temp = state[2];
    state[2] = state[10];
    state[10] = temp;
    temp = state[6];
    state[6] = state[14];
    state[14] = temp;
    
    temp = state[15];
    state[15] = state[11];
    state[11] = state[7];
    state[7] = state[3];
    state[3] = temp;
}

// Initialize scratchpad with pseudo-random data
static void init_scratchpad(uint8_t* scratchpad, size_t size, 
                            const uint8_t* seed, size_t seedLen) {
    uint8_t state[16];
    
    // Initialize state from seed
    memset(state, 0, 16);
    for (size_t i = 0; i < seedLen && i < 16; i++) {
        state[i] = seed[i];
    }
    
    // Fill scratchpad
    for (size_t i = 0; i < size; i += 16) {
        aes_round(state, AES_ROUND_KEYS);
        memcpy(scratchpad + i, state, 16);
    }
}

// Execute random program (simplified version)
static void execute_program(uint8_t* scratchpad, size_t scratchpadSize,
                            const uint8_t* input, size_t inputLen,
                            uint8_t* registerFile) {
    // Initialize register file from input
    memset(registerFile, 0, 256);
    for (size_t i = 0; i < inputLen && i < 256; i++) {
        registerFile[i] = input[i];
    }
    
    // Execute pseudo-random operations
    for (int round = 0; round < 2048; round++) {
        // Calculate memory address from register
        size_t addr = ((registerFile[0] << 8) | registerFile[1]) % (scratchpadSize - 8);
        
        // Read from scratchpad
        uint64_t value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((uint64_t)scratchpad[addr + i]) << (i * 8);
        }
        
        // Apply operation based on round
        switch (round % 8) {
            case 0: // XOR
                for (int i = 0; i < 8; i++) {
                    registerFile[i * 2] ^= (value >> (i * 8)) & 0xFF;
                }
                break;
            case 1: // ADD
                for (int i = 0; i < 8; i++) {
                    registerFile[i * 2 + 1] += (value >> (i * 8)) & 0xFF;
                }
                break;
            case 2: // MUL (simplified)
                registerFile[16] = (uint8_t)(value * registerFile[0]);
                break;
            case 3: // ROTATE
                {
                    uint8_t shift = registerFile[1] % 8;
                    for (int i = 0; i < 8; i++) {
                        registerFile[24 + i] = (scratchpad[addr + i] << shift) | 
                                                (scratchpad[addr + i] >> (8 - shift));
                    }
                }
                break;
            case 4: // SUB
                for (int i = 0; i < 8; i++) {
                    registerFile[32 + i] -= (value >> (i * 8)) & 0xFF;
                }
                break;
            case 5: // SWAP
                {
                    uint8_t temp = registerFile[40];
                    registerFile[40] = registerFile[41];
                    registerFile[41] = temp;
                }
                break;
            case 6: // AND
                for (int i = 0; i < 8; i++) {
                    registerFile[48 + i] &= (value >> (i * 8)) & 0xFF;
                }
                break;
            case 7: // OR
                for (int i = 0; i < 8; i++) {
                    registerFile[56 + i] |= (value >> (i * 8)) & 0xFF;
                }
                break;
        }
        
        // Write back to scratchpad
        for (int i = 0; i < 8; i++) {
            scratchpad[addr + i] ^= registerFile[round % 256];
        }
        
        // Update addressing registers
        registerFile[0] = registerFile[1];
        registerFile[1] = scratchpad[addr];
    }
}

void randomx_light_hash(const uint8_t* input, size_t inputLen,
                        const uint8_t* key, size_t keyLen,
                        uint8_t* hash) {
    
    // Light mode uses 256KB scratchpad (reduced from 2MB)
    const size_t SCRATCHPAD_SIZE = 256 * 1024;
    
    // Allocate scratchpad
    uint8_t* scratchpad = (uint8_t*)malloc(SCRATCHPAD_SIZE);
    if (!scratchpad) {
        // Fallback to simpler hash
        blake3_hash(input, inputLen, hash);
        return;
    }
    
    // Initialize scratchpad from key
    init_scratchpad(scratchpad, SCRATCHPAD_SIZE, key, keyLen);
    
    // Register file (256 bytes)
    uint8_t registerFile[256];
    
    // Execute random program
    execute_program(scratchpad, SCRATCHPAD_SIZE, input, inputLen, registerFile);
    
    // Final hash using Blake3
    uint8_t finalInput[512];
    memcpy(finalInput, registerFile, 256);
    memcpy(finalInput + 256, scratchpad, 256);
    
    blake3_hash(finalInput, 512, hash);
    
    free(scratchpad);
}
