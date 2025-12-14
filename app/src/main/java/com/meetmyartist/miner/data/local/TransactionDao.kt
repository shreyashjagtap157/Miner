package com.meetmyartist.miner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meetmyartist.miner.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId")
    fun getTransactionsForWallet(walletId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :txId")
    suspend fun getTransaction(txId: Long): Transaction?

    @androidx.room.Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET status = :status, txHash = :txHash WHERE id = :txId")
    suspend fun updateTransactionStatus(txId: Long, status: String, txHash: String)
}
