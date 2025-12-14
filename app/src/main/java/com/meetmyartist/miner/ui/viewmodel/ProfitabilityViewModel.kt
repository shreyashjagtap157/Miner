package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.gamification.Achievement
import com.meetmyartist.miner.gamification.AchievementManager
import com.meetmyartist.miner.mining.ProfitabilityCalculator
import com.meetmyartist.miner.network.CryptoPriceService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfitabilityViewModel @Inject constructor(
    private val calculator: ProfitabilityCalculator,
    private val priceService: CryptoPriceService,
    private val achievementManager: AchievementManager
) : ViewModel() {

    private val _profitabilityResult = MutableStateFlow<Map<String, Double>?>(null)
    val profitabilityResult = _profitabilityResult.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun calculateProfitability(hashrate: Double, crypto: String, manualPrice: Double?) {
        viewModelScope.launch {
            // Validate hashrate input
            if (hashrate <= 0) {
                _errorMessage.value = "Enter a hashrate greater than zero"
                _profitabilityResult.value = null
                return@launch
            }
            if (hashrate > 1_000_000_000) { // 1 TH/s seems excessive for mobile
                _errorMessage.value = "Hashrate value seems unrealistic (max 1 TH/s)"
                _profitabilityResult.value = null
                return@launch
            }
            
            // Validate manual price if provided
            if (manualPrice != null && (manualPrice <= 0 || manualPrice > 1_000_000)) {
                _errorMessage.value = "Price must be between 0 and 1,000,000 USD"
                _profitabilityResult.value = null
                return@launch
            }
            
            _loading.value = true
            try {
                val resolvedPrice = manualPrice?.takeIf { it > 0 }
                    ?: priceService.fetchPrice(crypto)?.priceUsd
                if (resolvedPrice == null || resolvedPrice <= 0) {
                    _errorMessage.value = "Unable to fetch live price for $crypto"
                    _profitabilityResult.value = null
                } else {
                    val result = calculator.calculate(hashrate, crypto, resolvedPrice)
                        .toMutableMap()
                    result["priceUsd"] = resolvedPrice
                    _profitabilityResult.value = result
                    achievementManager.unlockAchievement(Achievement.PROFIT_HUNTER)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Calculation failed"
                _profitabilityResult.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
