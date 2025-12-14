package com.meetmyartist.miner.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stratum mining protocol client for pool connectivity
 * Implements JSON-RPC 2.0 over TCP
 */
@Singleton
class StratumClient @Inject constructor() {
    
    private val gson = Gson()
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var receiveJob: Job? = null
    private val messageId = AtomicInteger(1)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _currentJob = MutableStateFlow<MiningJob?>(null)
    val currentJob: StateFlow<MiningJob?> = _currentJob.asStateFlow()
    
    private val _difficulty = MutableStateFlow(1.0)
    val difficulty: StateFlow<Double> = _difficulty.asStateFlow()
    
    private val _submittedShares = MutableStateFlow(0L)
    val submittedShares: StateFlow<Long> = _submittedShares.asStateFlow()
    
    private val _acceptedShares = MutableStateFlow(0L)
    val acceptedShares: StateFlow<Long> = _acceptedShares.asStateFlow()
    
    private val _rejectedShares = MutableStateFlow(0L)
    val rejectedShares: StateFlow<Long> = _rejectedShares.asStateFlow()
    
    private var extranonce1: String = ""
    private var extranonce2Size: Int = 0
    private var host: String = ""
    private var port: Int = 0
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    data class MiningJob(
        val jobId: String,
        val prevHash: String,
        val coinbase1: String,
        val coinbase2: String,
        val merkleBranch: List<String>,
        val version: String,
        val nbits: String,
        val ntime: String,
        val cleanJobs: Boolean
    )
    
    suspend fun connect(
        host: String,
        port: Int,
        walletAddress: String,
        workerName: String,
        password: String = "x"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            this@StratumClient.host = host
            this@StratumClient.port = port
            _connectionState.value = ConnectionState.Connecting
            
            try {
                val newSocket = Socket(host, port)
                this@StratumClient.socket = newSocket
                writer = BufferedWriter(OutputStreamWriter(newSocket.getOutputStream()))
                reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
                _connectionState.value = ConnectionState.Connected
                
                // Start listening for messages
                receiveJob = launch { receiveMessages() }
                
                // Subscribe and authorize
                subscribe(host, port)
                authorize("$walletAddress.$workerName", password)
                
                Result.success(Unit)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
                Result.failure(e)
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }
    
    fun disconnect() {
        receiveJob?.cancel()
        reader?.close()
        writer?.close()
        socket?.close()
        
        socket = null
        reader = null
        writer = null
        
        _connectionState.value = ConnectionState.Disconnected
    }
    
    private suspend fun subscribe(host: String, port: Int) {
        val request = buildJsonRpcRequest(
            method = "mining.subscribe",
            params = listOf("MinerApp/1.0", null, host, port)
        )
        sendMessage(request)
    }
    
    private suspend fun authorize(username: String, password: String) {
        val request = buildJsonRpcRequest(
            method = "mining.authorize",
            params = listOf(username, password)
        )
        sendMessage(request)
    }
    
    suspend fun submitShare(
        jobId: String,
        extranonce2: String,
        ntime: String,
        nonce: String
    ): Boolean {
        return try {
            val request = buildJsonRpcRequest(
                method = "mining.submit",
                params = listOf(
                    extranonce1,
                    extranonce2,
                    ntime,
                    nonce,
                    jobId
                )
            )
            
            sendMessage(request)
            _submittedShares.value++
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        writer?.let {
            it.write(message)
            it.newLine()
            it.flush()
        }
    }
    
    private suspend fun receiveMessages() {
        try {
            while (currentCoroutineContext().isActive && reader != null) {
                val line = reader?.readLine() ?: break
                
                if (line.isNotBlank()) {
                    processMessage(line)
                }
            }
        } catch (e: Exception) {
            if (currentCoroutineContext().isActive) {
                _connectionState.value = ConnectionState.Error("Connection lost: ${e.message}")
            }
        }
    }
    
    private fun processMessage(message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            
            // Check if it's a response or notification
            if (json.has("method")) {
                // It's a notification from pool
                val method = json.get("method").asString
                val params = json.getAsJsonArray("params")
                
                when (method) {
                    "mining.notify" -> {
                        // New mining job
                        val job = parseMiningJob(params)
                        _currentJob.value = job
                    }
                    
                    "mining.set_difficulty" -> {
                        // Difficulty change
                        if (params.size() > 0) {
                            _difficulty.value = params.get(0).asDouble
                        }
                    }
                    
                    "client.reconnect" -> {
                        // Pool requests reconnection
                        // Handle reconnection logic
                    }
                }
            } else if (json.has("result")) {
                // It's a response to our request
                val result = json.get("result")
                val id = json.get("id").asInt
                
                when {
                    // Subscribe response
                    result.isJsonArray && result.asJsonArray.size() >= 2 -> {
                        val subscriptionDetails = result.asJsonArray
                        if (subscriptionDetails.size() >= 2) {
                            extranonce1 = subscriptionDetails.get(1).asString
                            extranonce2Size = subscriptionDetails.get(2).asInt
                        }
                    }
                    
                    // Submit response
                    result.isJsonPrimitive && result.asJsonPrimitive.isBoolean -> {
                        if (result.asBoolean) {
                            _acceptedShares.value++
                        } else {
                            _rejectedShares.value++
                        }
                    }
                }
            } else if (json.has("error") && !json.get("error").isJsonNull) {
                // Error response
                val error = json.get("error")
                _rejectedShares.value++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseMiningJob(params: com.google.gson.JsonArray): MiningJob {
        return MiningJob(
            jobId = params.get(0).asString,
            prevHash = params.get(1).asString,
            coinbase1 = params.get(2).asString,
            coinbase2 = params.get(3).asString,
            merkleBranch = params.get(4).asJsonArray.map { it.asString },
            version = params.get(5).asString,
            nbits = params.get(6).asString,
            ntime = params.get(7).asString,
            cleanJobs = params.get(8).asBoolean
        )
    }
    
    private fun buildJsonRpcRequest(method: String, params: List<Any?>): String {
        val request = mapOf(
            "id" to messageId.getAndIncrement(),
            "method" to method,
            "params" to params
        )
        return gson.toJson(request)
    }
    
    fun isConnected(): Boolean {
        return socket?.isConnected == true && !socket?.isClosed!!
    }
    
    fun getExtranonce1(): String = extranonce1
    fun getExtranonce2Size(): Int = extranonce2Size
}
