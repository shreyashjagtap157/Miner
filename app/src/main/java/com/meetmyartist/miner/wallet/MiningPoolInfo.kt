package com.meetmyartist.miner.wallet

data class MiningPoolInfo(
    val name: String,
    val url: String,
    val port: Int,
    val fee: Double, // Mining pool fee percentage
    val isRecommended: Boolean = false
)
