package com.meetmyartist.miner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meetmyartist.miner.data.model.CryptoPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: CryptoPrice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<CryptoPrice>)

    @Query("SELECT * FROM crypto_prices WHERE symbol = :symbol")
    fun getPrice(symbol: String): Flow<CryptoPrice?>

    @Query("SELECT * FROM crypto_prices")
    fun getAllPrices(): Flow<List<CryptoPrice>>
}
