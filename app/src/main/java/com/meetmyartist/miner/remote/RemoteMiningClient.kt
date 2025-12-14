package com.meetmyartist.miner.remote

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteMiningClient @Inject constructor() {
    private val TAG = "RemoteMiningClient"
    
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var clientJob: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _remoteDeviceInfo = MutableStateFlow<RemoteDeviceInfo?>(null)
    val remoteDeviceInfo: StateFlow<RemoteDeviceInfo?> = _remoteDeviceInfo.asStateFlow()
    
    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()
    
    suspend fun connect(host: String, port: Int = 8888): Boolean = withContext(Dispatchers.IO) {
        try {
            if (_isConnected.value) {
                Log.w(TAG, "Already connected")
                return@withContext false
            }
            
            socket = Socket(host, port)
            reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
            writer = PrintWriter(socket?.getOutputStream(), true)
            
            _isConnected.value = true
            Log.i(TAG, "Connected to remote mining server at $host:$port")
            
            // Read welcome message
            val welcomeLine = reader?.readLine()
            welcomeLine?.let {
                val response = JSONObject(it)
                if (response.getBoolean("success")) {
                    val data = response.getJSONObject("data")
                    _remoteDeviceInfo.value = RemoteDeviceInfo(
                        deviceName = data.getString("deviceName"),
                        availableCores = data.getInt("availableCores")
                    )
                }
            }
            
            // Start listening for responses
            clientJob = coroutineScope.launch {
                listenForResponses()
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server", e)
            _isConnected.value = false
            false
        }
    }
    
    fun disconnect() {
        Log.i(TAG, "Disconnecting from remote server")
        
        clientJob?.cancel()
        clientJob = null
        
        reader?.close()
        writer?.close()
        socket?.close()
        
        reader = null
        writer = null
        socket = null
        
        _isConnected.value = false
        _remoteDeviceInfo.value = null
    }
    
    private suspend fun listenForResponses() {
        try {
            while (_isConnected.value) {
                val line = reader?.readLine() ?: break
                _lastResponse.value = line
                Log.d(TAG, "Received response: $line")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for responses", e)
            _isConnected.value = false
        }
    }
    
    suspend fun sendCommand(action: String, params: Map<String, Any>? = null): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    Log.w(TAG, "Not connected to server")
                    return@withContext null
                }
                
                val command = JSONObject().apply {
                    put("action", action)
                    params?.forEach { (key, value) ->
                        put(key, value)
                    }
                }
                
                writer?.println(command.toString())
                Log.d(TAG, "Sent command: $action")
                
                // Wait for response (with timeout)
                withTimeout(5000) {
                    while (_lastResponse.value == null) {
                        delay(100)
                    }
                    val response = _lastResponse.value
                    _lastResponse.value = null // Reset for next command
                    response?.let { JSONObject(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
                null
            }
        }
    }
    
    // Convenience methods for common commands
    suspend fun startRemoteMining(): Boolean {
        val response = sendCommand("start_mining")
        return response?.optBoolean("success") == true
    }
    
    suspend fun stopRemoteMining(): Boolean {
        val response = sendCommand("stop_mining")
        return response?.optBoolean("success") == true
    }
    
    suspend fun pauseRemoteMining(): Boolean {
        val response = sendCommand("pause_mining")
        return response?.optBoolean("success") == true
    }
    
    suspend fun resumeRemoteMining(): Boolean {
        val response = sendCommand("resume_mining")
        return response?.optBoolean("success") == true
    }
    
    suspend fun setRemoteThreads(threads: Int): Boolean {
        val response = sendCommand("set_threads", mapOf("threads" to threads))
        return response?.optBoolean("success") == true
    }
    
    suspend fun setRemoteHashrateLimit(limit: Double): Boolean {
        val response = sendCommand("set_hashrate_limit", mapOf("limit" to limit))
        return response?.optBoolean("success") == true
    }
    
    suspend fun getRemoteStats(): RemoteStats? {
        val response = sendCommand("get_stats")
        return if (response?.optBoolean("success") == true) {
            val data = response.getJSONObject("data")
            RemoteStats(
                hashrate = data.getDouble("hashrate"),
                cpuTemp = data.getDouble("cpuTemp").toFloat(),
                cpuUsage = data.getDouble("cpuUsage").toFloat(),
                uptime = data.getLong("uptime"),
                totalHashes = data.getLong("totalHashes"),
                acceptedShares = data.getLong("acceptedShares"),
                rejectedShares = data.getLong("rejectedShares")
            )
        } else null
    }
    
    suspend fun getRemoteDeviceInfo(): RemoteDeviceInfo? {
        val response = sendCommand("get_device_info")
        return if (response?.optBoolean("success") == true) {
            val data = response.getJSONObject("data")
            RemoteDeviceInfo(
                deviceName = "${data.getString("manufacturer")} ${data.getString("model")}",
                availableCores = data.getInt("availableCores"),
                activeThreads = data.optInt("activeThreads", 0),
                androidVersion = data.optString("androidVersion", "Unknown")
            )
        } else null
    }
}

data class RemoteDeviceInfo(
    val deviceName: String,
    val availableCores: Int,
    val activeThreads: Int = 0,
    val androidVersion: String = "Unknown"
)

data class RemoteStats(
    val hashrate: Double,
    val cpuTemp: Float,
    val cpuUsage: Float,
    val uptime: Long,
    val totalHashes: Long,
    val acceptedShares: Long,
    val rejectedShares: Long
)
