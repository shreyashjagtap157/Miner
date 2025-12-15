/**
 * Native Mining Benchmark Header
 */

#ifndef MINER_BENCHMARK_H
#define MINER_BENCHMARK_H

#include <cstdint>

namespace mining {

enum class Algorithm {
    SHA256,
    SCRYPT,
    BLAKE3
};

struct BenchmarkResult {
    Algorithm algorithm;
    uint64_t hashes;
    uint64_t duration_ms;
    double hashrate; // Hashes per second
};

class Benchmark {
public:
    static BenchmarkResult Run(Algorithm algo, int duration_ms = 5000);
};

} // namespace mining

#endif // MINER_BENCHMARK_H
