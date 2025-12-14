package com.meetmyartist.miner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents cryptocurrency balance in a wallet
 */
@Entity(tableName = "crypto_balances")
data class CryptoBalance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val walletId: Long,
    val cryptocurrency: String, // BTC, ETH, LTC, etc.
    val balance: Double, // Amount of crypto
    val address: String, // Wallet address
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Represents current market price of a cryptocurrency
 */
@Entity(tableName = "crypto_prices")
data class CryptoPrice(
    @PrimaryKey
    val symbol: String, // BTC, ETH, LTC, etc.
    val name: String, // Bitcoin, Ethereum, etc.
    val priceUsd: Double,
    val priceChange24h: Double, // Percentage
    val marketCapUsd: Double,
    val volume24h: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Transaction record for cryptocurrency transfers
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val walletId: Long,
    val cryptocurrency: String,
    val type: TransactionType,
    val amount: Double,
    val toAddress: String,
    val fromAddress: String,
    val txHash: String? = null, // Transaction hash (once confirmed)
    val fee: Double = 0.0,
    val status: TransactionStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

enum class TransactionType {
    SEND,
    RECEIVE,
    MINING_REWARD
}

enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED
}

/**
 * Portfolio summary with total value
 */
data class PortfolioSummary(
    val totalValueUsd: Double,
    val totalChange24h: Double, // Percentage
    val balances: List<CryptoBalanceWithPrice>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Balance with current price information
 */
data class CryptoBalanceWithPrice(
    val balance: CryptoBalance,
    val price: CryptoPrice?,
    val valueUsd: Double,
    val change24h: Double
)

/**
 * Transfer request data
 */
data class TransferRequest(
    val fromWalletId: Long,
    val cryptocurrency: String,
    val toAddress: String,
    val amount: Double,
    val fee: Double = 0.0,
    val notes: String? = null
)
