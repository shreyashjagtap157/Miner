#ifndef SCRYPT_H
#define SCRYPT_H

#include <cstdint>
#include <cstddef>

// Scrypt hash function (memory-hard, used by Litecoin)
void scrypt_hash(const uint8_t* password, size_t passwordLen,
                 const uint8_t* salt, size_t saltLen,
                 int N, int r, int p,
                 uint8_t* output, size_t outputLen);

#endif // SCRYPT_H
