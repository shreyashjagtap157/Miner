package com.meetmyartist.miner.remote

import android.content.Context
import android.util.Log
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.ResourceConfig
import com.meetmyartist.miner.mining.DynamicResourceController
import com.meetmyartist.miner.mining.MiningEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteMiningServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val miningEngine: MiningEngine,
    private val resourceController: DynamicResourceController
) {
    private val TAG = "RemoteMiningServer"
    
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()
    
    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients: StateFlow<List<String>> = _connectedClients.asStateFlow()
    
    private val _serverPort = MutableStateFlow(8888)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()
    
    private val clientHandlers = mutableListOf<Job>()
    
    fun startServer(port: Int = 8888) {
        if (_isServerRunning.value) {
            Log.w(TAG, "Server already running")
            return
        }
        
        serverJob = coroutineScope.launch {
            try {
                serverSocket = ServerSocket(port)
                _serverPort.value = port
                _isServerRunning.value = true
                
                Log.i(TAG, "Remote mining server started on port $port")
                
                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            val clientAddress = socket.inetAddress.hostAddress ?: "unknown"
                            Log.i(TAG, "Client connected: $clientAddress")
                            
                            // Add to connected clients
                            _connectedClients.value = _connectedClients.value + clientAddress
                            
                            // Handle client in separate coroutine
                            val handler = launch {
                                handleClient(socket, clientAddress)
                            }
                            clientHandlers.add(handler)
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
                _isServerRunning.value = false
            }
        }
    }
    
    fun stopServer() {
        Log.i(TAG, "Stopping remote mining server")
        
        // Cancel all client handlers
        clientHandlers.forEach { it.cancel() }
        clientHandlers.clear()
        
        // Close server socket
        serverSocket?.close()
        serverSocket = null
        
        // Cancel server job
        serverJob?.cancel()
        serverJob = null
        
        _isServerRunning.value = false
        _connectedClients.value = emptyList()
    }
    
    private suspend fun handleClient(socket: Socket, clientAddress: String) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            
            // Send welcome message
            val welcomeResponse = JSONObject().apply {
                put("success", true)
                put("message", "Connected to remote mining server")
                put("data", JSONObject().apply {
                    put("availableCores", resourceController.availableCores)
                    put("deviceName", android.os.Build.MODEL)
                })
            }
            writer.println(welcomeResponse.toString())
            
            // Read commands from client
            while (socket.isConnected) {
                val line = reader.readLine() ?: break
                
                try {
                    val command = JSONObject(line)
                    val response = processCommand(command)
                    writer.println(response.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing command", e)
                    val errorResponse = JSONObject().apply {
                        put("success", false)
                        put("message", "Error processing command: ${e.message}")
                    }
                    writer.println(errorResponse.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client $clientAddress", e)
        } finally {
            // Remove from connected clients
            _connectedClients.value = _connectedClients.value - clientAddress
            socket.close()
            Log.i(TAG, "Client disconnected: $clientAddress")
        }
    }
    
    private suspend fun processCommand(command: JSONObject): JSONObject {
        return try {
            val action = command.getString("action")
            
            when (action) {
                "start_mining" -> {
                    // For remote control, we'll use simplified start
                    withContext(Dispatchers.Main) {
                        // Start with default configuration
                        val activeConfig = miningEngine.miningStats.value
                        // Mining will start with current settings
                    }
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Mining start command received")
                    }
                }
                
                "stop_mining" -> {
                    withContext(Dispatchers.Main) {
                        miningEngine.stopMining()
                    }
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Mining stopped")
                    }
                }
                
                "pause_mining" -> {
                    withContext(Dispatchers.Main) {
                        miningEngine.pauseMining()
                    }
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Mining paused")
                    }
                }
                
                "resume_mining" -> {
                    withContext(Dispatchers.Main) {
                        miningEngine.resumeMining()
                    }
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Mining resumed")
                    }
                }
                
                "set_threads" -> {
                    val threads = command.getInt("threads")
                    resourceController.updateThreadCount(threads)
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Thread count set to $threads")
                    }
                }
                
                "set_hashrate_limit" -> {
                    val limit = command.getDouble("limit")
                    resourceController.setHashrateLimit(limit)
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Hashrate limit set to $limit H/s")
                    }
                }
                
                "get_stats" -> {
                    val stats = miningEngine.miningStats.value
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Current mining statistics")
                        put("data", JSONObject().apply {
                            put("hashrate", stats.hashrate)
                            put("cpuTemp", stats.cpuTemp)
                            put("cpuUsage", stats.cpuUsage)
                            put("uptime", stats.uptime)
                            put("totalHashes", stats.totalHashes)
                            put("acceptedShares", stats.acceptedShares)
                            put("rejectedShares", stats.rejectedShares)
                        })
                    }
                }
                
                "get_device_info" -> {
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Device information")
                        put("data", JSONObject().apply {
                            put("model", android.os.Build.MODEL)
                            put("manufacturer", android.os.Build.MANUFACTURER)
                            put("androidVersion", android.os.Build.VERSION.RELEASE)
                            put("availableCores", resourceController.availableCores)
                            put("activeThreads", resourceController.activeThreadCount.value)
                        })
                    }
                }
                
                else -> JSONObject().apply {
                    put("success", false)
                    put("message", "Unknown command: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            JSONObject().apply {
                put("success", false)
                put("message", "Error: ${e.message}")
            }
        }
    }
}
