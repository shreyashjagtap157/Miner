package com.meetmyartist.miner.network

import com.meetmyartist.miner.data.model.MiningConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoolConnectionManager @Inject constructor(
    private val stratumClient: StratumClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var connectionConfig: MiningConfig? = null
    
    private val _poolStatus = MutableStateFlow<PoolStatus>(PoolStatus.Disconnected)
    val poolStatus: StateFlow<PoolStatus> = _poolStatus.asStateFlow()
    
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 5000L
    
    sealed class PoolStatus {
        object Disconnected : PoolStatus()
        object Connecting : PoolStatus()
        data class Connected(val poolUrl: String, val latency: Long) : PoolStatus()
        data class Error(val message: String, val attempts: Int) : PoolStatus()
        object Reconnecting : PoolStatus()
    }
    
    suspend fun connectToPool(config: MiningConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                connectionConfig = config
                _poolStatus.value = PoolStatus.Connecting
                
                val startTime = System.currentTimeMillis()
                val result = stratumClient.connect(
                    host = config.poolUrl,
                    port = config.poolPort,
                    walletAddress = config.walletAddress,
                    workerName = config.workerName
                )
                
                if (result.isSuccess) {
                    val latency = System.currentTimeMillis() - startTime
                    _poolStatus.value = PoolStatus.Connected(config.poolUrl, latency)
                    reconnectAttempts = 0
                    startConnectionMonitoring()
                    Result.success(Unit)
                } else {
                    _poolStatus.value = PoolStatus.Error(
                        result.exceptionOrNull()?.message ?: "Connection failed",
                        reconnectAttempts
                    )
                    Result.failure(result.exceptionOrNull() ?: Exception("Connection failed"))
                }
            } catch (e: Exception) {
                _poolStatus.value = PoolStatus.Error(e.message ?: "Unknown error", reconnectAttempts)
                Result.failure(e)
            }
        }
    }
    
    fun disconnect() {
        reconnectJob?.cancel()
        stratumClient.disconnect()
        _poolStatus.value = PoolStatus.Disconnected
        reconnectAttempts = 0
    }
    
    private fun startConnectionMonitoring() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (isActive) {
                delay(10000) // Check every 10 seconds
                
                if (!stratumClient.isConnected()) {
                    handleDisconnection()
                    break
                }
            }
        }
    }
    
    private suspend fun handleDisconnection() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            _poolStatus.value = PoolStatus.Error(
                "Max reconnection attempts reached",
                reconnectAttempts
            )
            return
        }
        
        _poolStatus.value = PoolStatus.Reconnecting
        reconnectAttempts++
        
        delay(reconnectDelayMs * reconnectAttempts) // Exponential backoff
        
        connectionConfig?.let { config ->
            connectToPool(config)
        }
    }
    
    suspend fun submitShare(
        jobId: String,
        extranonce2: String,
        ntime: String,
        nonce: String
    ): Boolean {
        return stratumClient.submitShare(jobId, extranonce2, ntime, nonce)
    }
    
    fun getConnectionState() = stratumClient.connectionState
    fun getCurrentJob() = stratumClient.currentJob
    fun getDifficulty() = stratumClient.difficulty
    fun getSubmittedShares() = stratumClient.submittedShares
    fun getAcceptedShares() = stratumClient.acceptedShares
    fun getRejectedShares() = stratumClient.rejectedShares
    
    fun getLatency(): Long {
        return when (val status = _poolStatus.value) {
            is PoolStatus.Connected -> status.latency
            else -> 0
        }
    }
}
