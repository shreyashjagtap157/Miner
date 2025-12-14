package com.meetmyartist.miner.wallet

import com.meetmyartist.miner.data.local.CryptoBalanceDao
import com.meetmyartist.miner.data.local.CryptoPriceDao
import com.meetmyartist.miner.data.local.TransactionDao
import com.meetmyartist.miner.data.model.*
import com.meetmyartist.miner.network.CryptoPriceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cryptocurrency portfolio tracking and value calculation
 */
@Singleton
class PortfolioManager @Inject constructor(
    private val balanceDao: CryptoBalanceDao,
    private val priceDao: CryptoPriceDao,
    private val transactionDao: TransactionDao,
    private val priceService: CryptoPriceService
) {
    
    /**
     * Get portfolio summary with total value
     */
    fun getPortfolioSummary(): Flow<PortfolioSummary> = combine(
        balanceDao.getAllBalances(),
        priceDao.getAllPrices()
    ) { balances, prices ->
        val priceMap = prices.associateBy { it.symbol }
        
        val balancesWithPrices = balances.map { balance ->
            val price = priceMap[balance.cryptocurrency]
            val valueUsd = (price?.priceUsd ?: 0.0) * balance.balance
            
            CryptoBalanceWithPrice(
                balance = balance,
                price = price,
                valueUsd = valueUsd,
                change24h = price?.priceChange24h ?: 0.0
            )
        }
        
        val totalValue = balancesWithPrices.sumOf { it.valueUsd }
        
        // Calculate weighted average change
        val totalChange = if (totalValue > 0) {
            balancesWithPrices.sumOf { 
                (it.valueUsd / totalValue) * it.change24h 
            }
        } else {
            0.0
        }
        
        PortfolioSummary(
            totalValueUsd = totalValue,
            totalChange24h = totalChange,
            balances = balancesWithPrices,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Get balances for specific wallet
     */
    fun getWalletBalances(walletId: Long): Flow<List<CryptoBalanceWithPrice>> = combine(
        balanceDao.getBalancesForWallet(walletId),
        priceDao.getAllPrices()
    ) { balances, prices ->
        val priceMap = prices.associateBy { it.symbol }
        
        balances.map { balance ->
            val price = priceMap[balance.cryptocurrency]
            val valueUsd = (price?.priceUsd ?: 0.0) * balance.balance
            
            CryptoBalanceWithPrice(
                balance = balance,
                price = price,
                valueUsd = valueUsd,
                change24h = price?.priceChange24h ?: 0.0
            )
        }
    }
    
    /**
     * Get balance for specific cryptocurrency
     */
    fun getCryptoBalance(cryptocurrency: String): Flow<Double> =
        balanceDao.getBalancesByCrypto(cryptocurrency).map { balances ->
            balances.sumOf { it.balance }
        }
    
    /**
     * Update balance for mining rewards
     */
    suspend fun addMiningReward(
        walletId: Long,
        cryptocurrency: String,
        amount: Double,
        address: String
    ) = withContext(Dispatchers.IO) {
        // Update balance
        balanceDao.addToBalance(walletId, cryptocurrency, amount)
        
        // Record transaction
        val transaction = Transaction(
            walletId = walletId,
            cryptocurrency = cryptocurrency,
            type = TransactionType.MINING_REWARD,
            amount = amount,
            toAddress = address,
            fromAddress = "Mining Pool",
            status = TransactionStatus.CONFIRMED
        )
        
        transactionDao.insertTransaction(transaction)
    }
    
    /**
     * Refresh cryptocurrency prices
     */
    suspend fun refreshPrices(symbols: List<String>? = null) = withContext(Dispatchers.IO) {
        val cryptosToFetch = symbols ?: listOf(
            "BTC", "ETH", "LTC", "XMR", "ZEC", "RVN", "ETC", "DOGE", "BCH"
        )
        
        val prices = priceService.fetchPrices(cryptosToFetch)
        
        prices.forEach { priceData ->
            val cryptoPrice = CryptoPrice(
                symbol = priceData.symbol,
                name = priceData.name,
                priceUsd = priceData.priceUsd,
                priceChange24h = priceData.priceChange24h,
                marketCapUsd = priceData.marketCapUsd,
                volume24h = priceData.volume24h
            )
            
            priceDao.insertPrice(cryptoPrice)
        }
    }
    
    /**
     * Initialize balance for a wallet
     */
    suspend fun initializeBalance(
        walletId: Long,
        cryptocurrency: String,
        address: String,
        initialBalance: Double = 0.0
    ) = withContext(Dispatchers.IO) {
        val balance = CryptoBalance(
            walletId = walletId,
            cryptocurrency = cryptocurrency,
            balance = initialBalance,
            address = address
        )
        
        balanceDao.insertBalance(balance)
    }
    
    /**
     * Update balance (for external sync)
     */
    suspend fun updateBalance(
        walletId: Long,
        cryptocurrency: String,
        newBalance: Double
    ) = withContext(Dispatchers.IO) {
        // Get existing balance
        val balances = balanceDao.getBalancesForWallet(walletId).first()
        val existingBalance = balances.find { it.cryptocurrency == cryptocurrency }
        
        if (existingBalance != null) {
            balanceDao.updateBalance(
                existingBalance.copy(
                    balance = newBalance,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Get transaction history
     */
    fun getTransactionHistory(walletId: Long? = null): Flow<List<Transaction>> {
        return if (walletId != null) {
            transactionDao.getTransactionsForWallet(walletId)
        } else {
            transactionDao.getAllTransactions()
        }
    }
    
    /**
     * Get price for specific cryptocurrency
     */
    fun getPrice(symbol: String): Flow<CryptoPrice?> = priceDao.getPrice(symbol)
    
    /**
     * Calculate total portfolio value
     */
    suspend fun calculateTotalValue(): Double = withContext(Dispatchers.IO) {
        val summary = getPortfolioSummary().first()
        summary.totalValueUsd
    }
    
    /**
     * Get balances by cryptocurrency across all wallets
     */
    fun getBalancesByCrypto(crypto: String): Flow<List<CryptoBalanceWithPrice>> = combine(
        balanceDao.getBalancesByCrypto(crypto),
        priceDao.getPrice(crypto)
    ) { balances, price ->
        balances.map { balance ->
            val valueUsd = (price?.priceUsd ?: 0.0) * balance.balance
            
            CryptoBalanceWithPrice(
                balance = balance,
                price = price,
                valueUsd = valueUsd,
                change24h = price?.priceChange24h ?: 0.0
            )
        }
    }
}
