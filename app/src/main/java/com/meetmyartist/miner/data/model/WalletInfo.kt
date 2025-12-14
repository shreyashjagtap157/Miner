package com.meetmyartist.miner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cryptocurrency: String,
    val address: String,
    val encryptedPrivateKey: String? = null,
    val label: String,
    val balance: Double = 0.0,
    val linkedGoogleId: String? = null,
    val walletService: WalletService = WalletService.CUSTOM,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0
)

fun CloudWallet.toWalletInfo(): WalletInfo {
    return WalletInfo(
        address = this.address,
        cryptocurrency = this.cryptocurrency,
        label = this.label ?: "",
        walletService = WalletService.valueOf(this.walletService),
        linkedGoogleId = this.linkedGoogleId,
        balance = this.balance,
        createdAt = this.createdAt,
        lastSyncedAt = this.lastSyncedAt ?: 0
    )
}

enum class WalletService(val serviceName: String) {
    COINBASE("Coinbase"),
    BINANCE("Binance"),
    TRUST_WALLET("Trust Wallet"),
    METAMASK("MetaMask"),
    EXODUS("Exodus"),
    ATOMIC_WALLET("Atomic Wallet"),
    CUSTOM("Custom Address")
}

data class MiningPool(
    val name: String,
    val url: String,
    val port: Int,
    val fee: Double, // percentage
    val minPayout: Double,
    val algorithm: CryptoAlgorithm,
    val isActive: Boolean = false
)
