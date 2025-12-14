package com.meetmyartist.miner.di

import android.content.Context
import androidx.room.Room
import com.meetmyartist.miner.data.local.MinerDatabase
import com.meetmyartist.miner.data.local.MiningConfigDao
import com.meetmyartist.miner.data.local.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideMinerDatabase(@ApplicationContext context: Context): MinerDatabase {
        return Room.databaseBuilder(
            context,
            MinerDatabase::class.java,
            MinerDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMiningConfigDao(database: MinerDatabase): MiningConfigDao {
        return database.miningConfigDao()
    }
    
    @Provides
    @Singleton
    fun provideWalletDao(database: MinerDatabase): WalletDao {
        return database.walletDao()
    }
    
    @Provides
    @Singleton
    fun provideCryptoBalanceDao(database: MinerDatabase): com.meetmyartist.miner.data.local.CryptoBalanceDao {
        return database.cryptoBalanceDao()
    }
    
    @Provides
    @Singleton
    fun provideCryptoPriceDao(database: MinerDatabase): com.meetmyartist.miner.data.local.CryptoPriceDao {
        return database.cryptoPriceDao()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: MinerDatabase): com.meetmyartist.miner.data.local.TransactionDao {
        return database.transactionDao()
    }
}
