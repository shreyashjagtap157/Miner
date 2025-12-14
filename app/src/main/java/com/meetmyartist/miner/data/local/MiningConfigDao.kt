package com.meetmyartist.miner.data.local

import androidx.room.*
import com.meetmyartist.miner.data.model.MiningConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface MiningConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: MiningConfig): Long

    @Update
    suspend fun updateConfig(config: MiningConfig)

    @Delete
    suspend fun deleteConfig(config: MiningConfig)

    @Query("SELECT * FROM mining_configs WHERE id = :id")
    fun getMiningConfig(id: Long): Flow<MiningConfig>

    @Query("SELECT * FROM mining_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfig(): Flow<MiningConfig?>

    @Query("SELECT * FROM mining_configs")
    fun getAllConfigs(): Flow<List<MiningConfig>>

    @Query("SELECT * FROM mining_configs")
    suspend fun getAllConfigsOnce(): List<MiningConfig>

    @Query("SELECT * FROM mining_configs WHERE walletAddress = :walletAddress AND workerName = :workerName LIMIT 1")
    suspend fun getConfigByWalletAndWorker(walletAddress: String, workerName: String): MiningConfig?

    @Query("UPDATE mining_configs SET isActive = 0")
    suspend fun deactivateAllConfigs()
}
