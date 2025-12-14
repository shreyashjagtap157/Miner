package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.model.CorePriority
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.mining.CoreStats
import com.meetmyartist.miner.mining.DynamicResourceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedResourceViewModel @Inject constructor(
    private val resourceController: DynamicResourceController,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    // Get available cores from resource controller
    val availableCores: Int = resourceController.availableCores
    
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
    
    val perCoreStats = resourceController.perCoreStats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )
    
    private val _enablePerCoreControl = MutableStateFlow(false)
    val enablePerCoreControl: StateFlow<Boolean> = _enablePerCoreControl.asStateFlow()
    
    private val _enableHashrateLimit = MutableStateFlow(false)
    val enableHashrateLimit: StateFlow<Boolean> = _enableHashrateLimit.asStateFlow()
    
    private val _maxHashrate = MutableStateFlow<Double?>(null)
    val maxHashrate: StateFlow<Double?> = _maxHashrate.asStateFlow()
    
    private val coreUsageMap = mutableMapOf<Int, Int>()
    private val corePriorityMap = mutableMapOf<Int, CorePriority>()
    
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
    
    fun togglePerCoreControl(enabled: Boolean) {
        _enablePerCoreControl.value = enabled
    }
    
    fun toggleHashrateLimit(enabled: Boolean) {
        _enableHashrateLimit.value = enabled
        if (!enabled) {
            resourceController.setHashrateLimit(null)
            _maxHashrate.value = null
        } else {
            _maxHashrate.value?.let { resourceController.setHashrateLimit(it) }
        }
    }
    
    fun setHashrateLimit(maxHashrate: Double) {
        _maxHashrate.value = maxHashrate
        if (_enableHashrateLimit.value) {
            resourceController.setHashrateLimit(maxHashrate)
        }
    }
    
    fun setCoreUsage(coreId: Int, usagePercent: Int) {
        coreUsageMap[coreId] = usagePercent
        val priority = corePriorityMap[coreId] ?: CorePriority.NORMAL
        resourceController.setCoreAffinity(coreId, coreId, usagePercent)
    }
    
    fun setCorePriority(coreId: Int, priority: CorePriority) {
        corePriorityMap[coreId] = priority
        val usage = coreUsageMap[coreId] ?: 100
        resourceController.setCoreAffinity(coreId, coreId, usage)
    }
    
    fun toggleCore(coreId: Int, enabled: Boolean) {
        if (!enabled) {
            // Remove this specific thread
            val currentCount = activeThreadCount.value
            if (currentCount > 0) {
                setThreadCount(currentCount - 1)
            }
        }
    }
    
    fun enableCore(coreId: Int) {
        val currentCount = activeThreadCount.value
        if (currentCount < availableCores) {
            setThreadCount(currentCount + 1)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        resourceController.stopAll()
    }
}
