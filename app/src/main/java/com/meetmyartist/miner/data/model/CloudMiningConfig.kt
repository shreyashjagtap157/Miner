package com.meetmyartist.miner.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CloudMiningConfig(
    val id: String = "",
    val cryptocurrency: String = "",
    val algorithm: String = "",
    val poolUrl: String = "",
    val poolPort: Int = 0,
    val walletAddress: String = "",
    val workerName: String = "",
    val threadCount: Int = 0,
    val cpuUsagePercent: Int = 0,
    val isActive: Boolean = false,
    val createdAt: Long = 0L
)
