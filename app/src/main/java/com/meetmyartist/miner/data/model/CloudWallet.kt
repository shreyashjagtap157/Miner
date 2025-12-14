package com.meetmyartist.miner.data.model

data class CloudWallet(
    val address: String = "",
    val cryptocurrency: String = "",
    val label: String = "",
    val walletService: String = "",
    val linkedGoogleId: String? = null,
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)
