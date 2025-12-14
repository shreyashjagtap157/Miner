#ifndef SHA256_H
#define SHA256_H

#include <cstdint>
#include <cstddef>
#include <cstring>

// SHA256 implementation optimized for ARM
void sha256_hash(const uint8_t* data, size_t len, uint8_t* hash);

#endif // SHA256_H
