package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.CloudSyncManager
import com.meetmyartist.miner.data.local.MiningConfigDao
import com.meetmyartist.miner.data.model.CryptoAlgorithm
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.toMiningConfig
import com.meetmyartist.miner.wallet.MiningPoolInfo
import com.meetmyartist.miner.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val miningConfigDao: MiningConfigDao,
    private val walletManager: WalletManager,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    val allConfigs = miningConfigDao.getAllConfigs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _selectedCrypto = MutableStateFlow<String?>(null)
    val selectedCrypto: StateFlow<String?> = _selectedCrypto.asStateFlow()

    private val _availablePools = MutableStateFlow<List<MiningPoolInfo>>(emptyList())
    val availablePools: StateFlow<List<MiningPoolInfo>> = _availablePools.asStateFlow()

    fun selectCryptocurrency(crypto: String) {
        _selectedCrypto.value = crypto
        viewModelScope.launch {
            _availablePools.value = walletManager.getPopularPools(crypto)
        }
    }

    fun createConfig(
        cryptocurrency: String,
        algorithm: String,
        poolUrl: String,
        poolPort: Int,
        walletAddress: String,
        workerName: String
    ) {
        viewModelScope.launch {
            val config = MiningConfig(
                cryptocurrency = cryptocurrency,
                algorithm = algorithm,
                poolUrl = poolUrl,
                poolPort = poolPort,
                walletAddress = walletAddress,
                workerName = workerName,
                threadCount = Runtime.getRuntime().availableProcessors() / 2,
                cpuUsagePercent = 70
            )
            val id = miningConfigDao.insertConfig(config)
            cloudSyncManager.syncConfig(config.copy(id = id)).onFailure { e ->
                android.util.Log.w("ConfigurationViewModel", "Cloud sync failed: ${e.message}")
            }
        }
    }

    fun deleteConfig(config: MiningConfig) {
        viewModelScope.launch {
            miningConfigDao.deleteConfig(config)
            cloudSyncManager.deleteConfig(config).onFailure { e ->
                android.util.Log.w("ConfigurationViewModel", "Cloud delete failed: ${e.message}")
            }
        }
    }

    fun updateConfig(config: MiningConfig) {
        viewModelScope.launch {
            miningConfigDao.updateConfig(config)
            cloudSyncManager.syncConfig(config).onFailure { e ->
                android.util.Log.w("ConfigurationViewModel", "Cloud sync failed: ${e.message}")
            }
        }
    }

    fun setActiveConfig(config: MiningConfig) {
        viewModelScope.launch {
            miningConfigDao.deactivateAllConfigs()
            val activeConfig = config.copy(isActive = true)
            miningConfigDao.updateConfig(activeConfig)
            cloudSyncManager.setActiveConfig(activeConfig).onFailure { e ->
                android.util.Log.w("ConfigurationViewModel", "Cloud sync failed: ${e.message}")
            }
        }
    }

    fun getSupportedAlgorithms(): List<CryptoAlgorithm> {
        return CryptoAlgorithm.entries
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                cloudSyncManager.fetchConfigs()
                    .onSuccess { remoteConfigs ->
                        val remoteKeys = remoteConfigs.map { it.walletAddress to it.workerName }.toSet()

                        remoteConfigs.forEach { cloudConfig ->
                            val local = miningConfigDao.getConfigByWalletAndWorker(cloudConfig.walletAddress, cloudConfig.workerName)
                            val mapped = cloudConfig.toMiningConfig()
                            if (local == null) {
                                miningConfigDao.insertConfig(mapped)
                            } else {
                                miningConfigDao.updateConfig(mapped.copy(id = local.id))
                            }
                        }

                        val localConfigs = miningConfigDao.getAllConfigsOnce()
                        localConfigs
                            .filter { it.walletAddress to it.workerName !in remoteKeys }
                            .forEach { miningConfigDao.deleteConfig(it) }
                    }
                    .onFailure { e ->
                        android.util.Log.e("ConfigurationViewModel", "Failed to sync from cloud: ${e.message}", e)
                    }
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
