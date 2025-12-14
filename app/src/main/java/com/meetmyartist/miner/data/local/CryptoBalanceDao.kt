package com.meetmyartist.miner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meetmyartist.miner.data.model.CryptoBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoBalanceDao {
    @Query("SELECT * FROM crypto_balances WHERE walletId = :walletId")
    fun getBalancesForWallet(walletId: Long): Flow<List<CryptoBalance>>
    
    @Query("SELECT * FROM crypto_balances")
    fun getAllBalances(): Flow<List<CryptoBalance>>
    
    @Query("SELECT * FROM crypto_balances WHERE cryptocurrency = :crypto")
    fun getBalancesByCrypto(crypto: String): Flow<List<CryptoBalance>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: CryptoBalance): Long
    
    @Query("DELETE FROM crypto_balances WHERE id = :balanceId")
    suspend fun deleteBalance(balanceId: Long)
    
    @Query("UPDATE crypto_balances SET balance = balance + :amount WHERE walletId = :walletId AND cryptocurrency = :crypto")
    suspend fun addToBalance(walletId: Long, crypto: String, amount: Double)

    @androidx.room.Update
    suspend fun updateBalance(balance: CryptoBalance)
}
