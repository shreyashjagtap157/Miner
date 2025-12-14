package com.meetmyartist.miner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meetmyartist.miner.data.model.WalletInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletInfo): Long

    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletInfo>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletInfo?

    @Update
    suspend fun updateWallet(wallet: WalletInfo)

    @Query("SELECT * FROM wallets WHERE cryptocurrency = :crypto")
    fun getWalletsByCrypto(crypto: String): Flow<List<WalletInfo>>

    @Query("SELECT * FROM wallets WHERE linkedGoogleId = :googleId")
    fun getWalletsByGoogleId(googleId: String): Flow<List<WalletInfo>>

    @Delete
    suspend fun deleteWallet(wallet: WalletInfo)

    @Query("SELECT * FROM wallets WHERE address = :address")
    suspend fun getWalletByAddress(address: String): WalletInfo?
}
