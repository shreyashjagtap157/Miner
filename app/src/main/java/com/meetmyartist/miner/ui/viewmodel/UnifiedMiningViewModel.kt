package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.model.Cryptocurrency
import com.meetmyartist.miner.data.model.CryptocurrencyDefaults
import com.meetmyartist.miner.data.model.MiningStats
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.mining.DynamicResourceController
import com.meetmyartist.miner.mining.MiningEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnifiedMiningViewModel @Inject constructor(
    private val miningEngine: MiningEngine,
    private val resourceController: DynamicResourceController,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    val availableCores: Int = resourceController.availableCores
    
    val miningStats: StateFlow<MiningStats> = miningEngine.miningStats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MiningStats()
    )
    
    val activeThreadCount = resourceController.activeThreadCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )
    
    val currentHashrate = resourceController.currentHashrate.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.0
    )
    
    private val _selectedCryptocurrency = MutableStateFlow<Cryptocurrency?>(
        CryptocurrencyDefaults.getCryptoBySymbol("XMR") // Default to Monero (CPU-friendly)
    )
    val selectedCryptocurrency: StateFlow<Cryptocurrency?> = _selectedCryptocurrency.asStateFlow()
    
    private val _totalMinedValue = MutableStateFlow(0L) // In smallest units
    val totalMinedValue: StateFlow<Long> = _totalMinedValue.asStateFlow()
    
    private val _cpuUsageLimit = MutableStateFlow(70f)
    val cpuUsageLimit: StateFlow<Float> = _cpuUsageLimit.asStateFlow()
    
    private val _showCryptoSelector = MutableStateFlow(false)
    val showCryptoSelector: StateFlow<Boolean> = _showCryptoSelector.asStateFlow()
    
    init {
        // Load preferences
        viewModelScope.launch {
            preferencesManager.selectedThreads.collect { threads ->
                if (threads != activeThreadCount.value) {
                    resourceController.updateThreadCount(threads)
                }
            }
        }
        
        viewModelScope.launch {
            preferencesManager.cpuUsageLimit.collect { limit ->
                _cpuUsageLimit.value = limit.toFloat()
            }
        }
        
        // Calculate mined value based on hashrate and time
        viewModelScope.launch {
            miningStats.collect { stats ->
                _selectedCryptocurrency.value?.let { crypto ->
                    // Simplified calculation: total hashes converted to smallest units
                    // In reality, this would depend on network difficulty, block rewards, etc.
                    val estimatedUnits = calculateMinedUnits(stats.totalHashes, crypto)
                    _totalMinedValue.value = estimatedUnits
                }
            }
        }
    }
    
    fun setThreadCount(count: Int) {
        resourceController.updateThreadCount(count)
        viewModelScope.launch {
            preferencesManager.updateSelectedThreads(count)
        }
    }
    
    fun incrementThreads() {
        val current = activeThreadCount.value
        if (current < availableCores) {
            setThreadCount(current + 1)
        }
    }
    
    fun decrementThreads() {
        val current = activeThreadCount.value
        if (current > 0) {
            setThreadCount(current - 1)
        }
    }
    
    fun setCpuUsageLimit(limit: Float) {
        _cpuUsageLimit.value = limit
        viewModelScope.launch {
            preferencesManager.updateCpuUsageLimit(limit.toInt())
        }
    }
    
    fun selectCryptocurrency(crypto: Cryptocurrency) {
        _selectedCryptocurrency.value = crypto
        _totalMinedValue.value = 0L // Reset mined value when switching coins
        _showCryptoSelector.value = false
    }
    
    fun showCryptoSelector() {
        _showCryptoSelector.value = true
    }
    
    fun hideCryptoSelector() {
        _showCryptoSelector.value = false
    }
    
    fun optimizeForMaxHashrate() {
        // Set max threads and 100% CPU
        setThreadCount(availableCores)
        setCpuUsageLimit(100f)
        resourceController.setHashrateLimit(null) // Remove any limits
    }
    
    fun optimizeForEfficiency() {
        // Set moderate threads and 50% CPU for efficiency
        val efficientThreads = (availableCores * 0.5).toInt().coerceAtLeast(2)
        setThreadCount(efficientThreads)
        setCpuUsageLimit(50f)
    }
    
    private fun calculateMinedUnits(totalHashes: Long, crypto: Cryptocurrency): Long {
        // Simplified calculation for demonstration
        // Real calculation would use: (hashrate * time) / difficulty * block_reward
        // This is a placeholder that converts hashes to smallest units
        
        return when (crypto.algorithm) {
            com.meetmyartist.miner.data.model.MiningAlgorithm.RANDOMX -> {
                // Monero: ~1000 H/s might earn ~0.001 XMR per day
                // Simplified: totalHashes / 10^12 = piconero
                (totalHashes / 100_000).coerceAtLeast(0)
            }
            com.meetmyartist.miner.data.model.MiningAlgorithm.THETA_EDGE -> {
                // Theta: Based on edge computing contribution
                (totalHashes / 50_000).coerceAtLeast(0)
            }
            com.meetmyartist.miner.data.model.MiningAlgorithm.KASPA -> {
                // Kaspa: Fast block time, more frequent rewards
                (totalHashes / 25_000).coerceAtLeast(0)
            }
            else -> {
                // Generic calculation for other algorithms
                (totalHashes / 100_000).coerceAtLeast(0)
            }
        }
    }
    
    fun getAllSupportedCryptos(): List<Cryptocurrency> {
        return CryptocurrencyDefaults.getAllSupportedCryptos()
    }
    
    fun getCPUFriendlyCoins(): List<Cryptocurrency> {
        return CryptocurrencyDefaults.getCPUFriendlyCoins()
    }
}
