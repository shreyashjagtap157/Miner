#ifndef RANDOMX_LIGHT_H
#define RANDOMX_LIGHT_H

#include <cstdint>
#include <cstddef>

// RandomX light mode hash (used by Monero)
// Light mode doesn't require the 2GB dataset, suitable for mobile
void randomx_light_hash(const uint8_t* input, size_t inputLen,
                        const uint8_t* key, size_t keyLen,
                        uint8_t* hash);

#endif // RANDOMX_LIGHT_H
