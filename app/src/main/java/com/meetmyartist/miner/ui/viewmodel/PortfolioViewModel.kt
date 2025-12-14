package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.data.model.*
import com.meetmyartist.miner.wallet.PortfolioManager
import com.meetmyartist.miner.wallet.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioManager: PortfolioManager,
    private val transferManager: TransferManager
) : ViewModel() {
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()
    
    val portfolioSummary = portfolioManager.getPortfolioSummary()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PortfolioSummary(0.0, 0.0, emptyList())
        )
    
    val transactions = portfolioManager.getTransactionHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    
    init {
        // Initial price refresh
        refreshPrices()
    }
    
    fun refreshPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                portfolioManager.refreshPrices()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun getWalletBalances(walletId: Long): Flow<List<CryptoBalanceWithPrice>> {
        return portfolioManager.getWalletBalances(walletId)
    }
    
    fun getCryptoBalance(cryptocurrency: String): Flow<Double> {
        return portfolioManager.getCryptoBalance(cryptocurrency)
    }
    
    fun getPrice(symbol: String): Flow<CryptoPrice?> {
        return portfolioManager.getPrice(symbol)
    }
    
    fun initiateTransfer(request: TransferRequest) {
        viewModelScope.launch {
            _transferState.value = TransferState.Processing
            
            val result = transferManager.initiateTransfer(request)
            
            _transferState.value = when (result) {
                is TransferManager.TransferResult.Success -> {
                    TransferState.Success(result.transactionId, result.txHash)
                }
                is TransferManager.TransferResult.InsufficientFunds -> {
                    TransferState.Error("Insufficient funds. Available: ${result.available}, Required: ${result.required}")
                }
                is TransferManager.TransferResult.InvalidAddress -> {
                    TransferState.Error(result.reason)
                }
                is TransferManager.TransferResult.Error -> {
                    TransferState.Error(result.message)
                }
            }
        }
    }
    
    suspend fun estimateFee(cryptocurrency: String, amount: Double): Double {
        return transferManager.estimateFee(cryptocurrency, amount)
    }
    
    fun validateAddress(cryptocurrency: String, address: String): Boolean {
        return transferManager.isValidAddress(cryptocurrency, address)
    }
    
    fun resetTransferState() {
        _transferState.value = TransferState.Idle
    }
    
    fun getTransactionHistory(walletId: Long? = null): Flow<List<Transaction>> {
        return portfolioManager.getTransactionHistory(walletId)
    }
    
    fun cancelTransaction(txId: Long) {
        viewModelScope.launch {
            val success = transferManager.cancelTransaction(txId)
            if (success) {
                // Transaction cancelled successfully
            }
        }
    }
    
    sealed class TransferState {
        object Idle : TransferState()
        object Processing : TransferState()
        data class Success(val transactionId: Long, val txHash: String?) : TransferState()
        data class Error(val message: String) : TransferState()
    }
}
