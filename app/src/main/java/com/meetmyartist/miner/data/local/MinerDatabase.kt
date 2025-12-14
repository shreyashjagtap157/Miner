package com.meetmyartist.miner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.WalletInfo
import com.meetmyartist.miner.data.model.CryptoBalance
import com.meetmyartist.miner.data.model.CryptoPrice
import com.meetmyartist.miner.data.model.Transaction

@Database(
    entities = [
        MiningConfig::class,
        WalletInfo::class,
        CryptoBalance::class,
        CryptoPrice::class,
        Transaction::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MinerDatabase : RoomDatabase() {
    abstract fun miningConfigDao(): MiningConfigDao
    abstract fun walletDao(): WalletDao
    abstract fun cryptoBalanceDao(): CryptoBalanceDao
    abstract fun cryptoPriceDao(): CryptoPriceDao
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        const val DATABASE_NAME = "miner_db"
    }
}
