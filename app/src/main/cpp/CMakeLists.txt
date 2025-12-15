cmake_minimum_required(VERSION 3.22.1)

project("miner_native" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable optimizations for release builds
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_C_FLAGS_RELEASE "-O3 -DNDEBUG")

# Add ARM NEON support for optimization
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+crypto")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
endif()

# Source files
add_library(miner_native SHARED
    mining/native_miner.cpp
    mining/sha256.cpp
    mining/randomx_light.cpp
    mining/blake3.cpp
    mining/scrypt.cpp
    mining/benchmark.cpp
)

# Find Android log library
find_library(log-lib log)

# Link libraries
target_link_libraries(miner_native
    ${log-lib}
)

# Include directories
target_include_directories(miner_native PRIVATE
    ${CMAKE_SOURCE_DIR}/mining
)
