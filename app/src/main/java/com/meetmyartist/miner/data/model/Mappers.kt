package com.meetmyartist.miner.data.model

// This file contains mapping functions to convert between different data models,
// such as from cloud/network models to local database models.

/**
 * Converts a [CloudMiningConfig] object from Firestore into a local [MiningConfig] entity.
 * Note that the local `id` is not preserved from the cloud `id` because the local `id`
 * is an auto-incrementing primary key managed by Room, whereas the cloud `id` is a
 * string-based document identifier.
 */
fun CloudMiningConfig.toMiningConfig(): MiningConfig {
    return MiningConfig(
        // The local `id` is intentionally left as the default (0) so that Room can
        // auto-generate a new one upon insertion if it's a new config.
        // If updating an existing config, the caller is responsible for setting the correct `id`.
        id = 0,
        cryptocurrency = this.cryptocurrency,
        algorithm = this.algorithm,
        poolUrl = this.poolUrl,
        poolPort = this.poolPort,
        walletAddress = this.walletAddress,
        workerName = this.workerName,
        threadCount = this.threadCount,
        cpuUsagePercent = this.cpuUsagePercent,
        isActive = this.isActive,
        createdAt = this.createdAt,
        failoverPoolUrl = null, // Cloud model does not have these fields yet
        failoverPoolPort = null,
        failoverEnabled = false
    )
}
