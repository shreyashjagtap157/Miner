#ifndef BLAKE3_H
#define BLAKE3_H

#include <cstdint>
#include <cstddef>

// Simplified Blake3 hash function
void blake3_hash(const uint8_t* data, size_t len, uint8_t* hash);

#endif // BLAKE3_H
