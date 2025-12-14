package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.remote.RemoteDeviceInfo
import com.meetmyartist.miner.remote.RemoteMiningClient
import com.meetmyartist.miner.remote.RemoteMiningServer
import com.meetmyartist.miner.remote.RemoteStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteMiningViewModel @Inject constructor(
    private val remoteMiningServer: RemoteMiningServer,
    private val remoteMiningClient: RemoteMiningClient
) : ViewModel() {
    
    // Server state
    val isServerRunning: StateFlow<Boolean> = remoteMiningServer.isServerRunning
    val connectedClients: StateFlow<List<String>> = remoteMiningServer.connectedClients
    val serverPort: StateFlow<Int> = remoteMiningServer.serverPort
    
    // Client state
    val isClientConnected: StateFlow<Boolean> = remoteMiningClient.isConnected
    val remoteDeviceInfo: StateFlow<RemoteDeviceInfo?> = remoteMiningClient.remoteDeviceInfo
    
    private val _remoteStats = MutableStateFlow<RemoteStats?>(null)
    val remoteStats: StateFlow<RemoteStats?> = _remoteStats.asStateFlow()
    
    fun startServer(port: Int = 8888) {
        remoteMiningServer.startServer(port)
    }
    
    fun stopServer() {
        remoteMiningServer.stopServer()
    }
    
    fun connectToServer(host: String, port: Int = 8888) {
        viewModelScope.launch {
            val connected = remoteMiningClient.connect(host, port)
            if (connected) {
                // Refresh device info and stats
                refreshRemoteStats()
            }
        }
    }
    
    fun disconnectClient() {
        remoteMiningClient.disconnect()
        _remoteStats.value = null
    }
    
    fun startRemoteMining() {
        viewModelScope.launch {
            remoteMiningClient.startRemoteMining()
            refreshRemoteStats()
        }
    }
    
    fun stopRemoteMining() {
        viewModelScope.launch {
            remoteMiningClient.stopRemoteMining()
            refreshRemoteStats()
        }
    }
    
    fun pauseRemoteMining() {
        viewModelScope.launch {
            remoteMiningClient.pauseRemoteMining()
            refreshRemoteStats()
        }
    }
    
    fun resumeRemoteMining() {
        viewModelScope.launch {
            remoteMiningClient.resumeRemoteMining()
            refreshRemoteStats()
        }
    }
    
    fun setRemoteThreads(threads: Int) {
        viewModelScope.launch {
            remoteMiningClient.setRemoteThreads(threads)
            refreshRemoteStats()
        }
    }
    
    fun setRemoteHashrateLimit(limit: Double) {
        viewModelScope.launch {
            remoteMiningClient.setRemoteHashrateLimit(limit)
            refreshRemoteStats()
        }
    }
    
    fun refreshRemoteStats() {
        viewModelScope.launch {
            val stats = remoteMiningClient.getRemoteStats()
            _remoteStats.value = stats
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopServer()
        disconnectClient()
    }
}
