/**
 * Native Mining Benchmark
 * Measures hashrate performance for supported algorithms
 */

#include "benchmark.h"
#include "sha256.h"
#include "scrypt.h"
#include "blake3.h"
#include <chrono>
#include <vector>
#include <string>

namespace mining {

BenchmarkResult Benchmark::Run(Algorithm algo, int duration_ms) {
    BenchmarkResult result;
    result.algorithm = algo;
    result.duration_ms = duration_ms;
    result.hashes = 0;
    
    // Test data
    std::vector<uint8_t> input(80, 0); // 80 byte block header
    std::vector<uint8_t> output(32);
    
    auto start = std::chrono::high_resolution_clock::now();
    auto end_time = start + std::chrono::milliseconds(duration_ms);
    
    while (std::chrono::high_resolution_clock::now() < end_time) {
        // Run batch of hashes (unrolled loop for speed)
        for (int i = 0; i < 1000; i++) {
            input[0]++; // Change nonce
            
            switch (algo) {
                case Algorithm::SHA256:
                    SHA256::Hash(input.data(), input.size(), output.data());
                    break;
                case Algorithm::SCRYPT:
                    // Simplified Scrypt parameters for benchmark
                    Scrypt::Hash(input.data(), input.size(), output.data(), 1024, 1, 1);
                    break;
                case Algorithm::BLAKE3:
                    Blake3::Hash(input.data(), input.size(), output.data());
                    break;
            }
        }
        result.hashes += 1000;
    }
    
    auto actual_end = std::chrono::high_resolution_clock::now();
    auto actual_duration = std::chrono::duration_cast<std::chrono::milliseconds>(actual_end - start).count();
    
    result.hashrate = (double)result.hashes / ((double)actual_duration / 1000.0);
    
    return result;
}

} // namespace mining
