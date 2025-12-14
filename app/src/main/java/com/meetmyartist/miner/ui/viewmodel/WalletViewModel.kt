package com.meetmyartist.miner.ui.viewmodel

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.auth.GoogleAuthManager
import com.meetmyartist.miner.data.CloudSyncManager
import com.meetmyartist.miner.data.model.WalletInfo
import com.meetmyartist.miner.data.model.WalletService
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.gamification.Achievement
import com.meetmyartist.miner.gamification.AchievementManager
import com.meetmyartist.miner.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WalletViewModel
@Inject
constructor(
        private val walletManager: WalletManager,
        private val googleAuthManager: GoogleAuthManager,
        private val cloudSyncManager: CloudSyncManager,
        private val preferencesManager: PreferencesManager,
        private val achievementManager: AchievementManager
) : ViewModel() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val allWallets =
            walletManager
                    .getAllWallets()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signInState =
            googleAuthManager.signInState.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    GoogleAuthManager.SignInState.SignedOut
            )

    val currentAccount =
            googleAuthManager.currentAccount.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    null
            )

    private val _walletCreationResult =
            MutableStateFlow<WalletCreationState>(WalletCreationState.Idle)
    val walletCreationResult: StateFlow<WalletCreationState> = _walletCreationResult.asStateFlow()

    init {
        viewModelScope.launch {
            googleAuthManager.signInState.collect { state ->
                when (state) {
                    is GoogleAuthManager.SignInState.SignedIn -> {
                        preferencesManager.updateGoogleAccountId(state.account.id)
                        syncFromCloud()
                    }
                    GoogleAuthManager.SignInState.SignedOut -> {
                        preferencesManager.updateGoogleAccountId(null)
                    }
                    is GoogleAuthManager.SignInState.Error -> {
                        // Ignore for now; UI observes signInState
                    }
                }
            }
        }
    }

    /** Gets the credential request for Google Sign-In using Credential Manager. */
    fun getCredentialRequest(): GetCredentialRequest = googleAuthManager.getCredentialRequest()

    /** Handles the successful credential response from Credential Manager. */
    fun handleSignInResult(result: GetCredentialResponse) {
        viewModelScope.launch { googleAuthManager.handleSignInResult(result) }
    }

    /** Handles sign-in errors from Credential Manager. */
    fun handleSignInError(exception: GetCredentialException) {
        googleAuthManager.handleSignInError(exception)
    }

    fun createWallet(
            cryptocurrency: String,
            address: String,
            privateKey: String?,
            label: String,
            walletService: WalletService
    ) {
        viewModelScope.launch {
            _walletCreationResult.value = WalletCreationState.Loading

            val googleId = currentAccount.value?.id
            val result =
                    walletManager.createWallet(
                            cryptocurrency = cryptocurrency,
                            address = address,
                            privateKey = privateKey,
                            label = label,
                            walletService = walletService,
                            googleId = googleId
                    )

            _walletCreationResult.value =
                    if (result.isSuccess) {
                        val wallet = result.getOrNull()!!
                        val ownerId = preferencesManager.googleAccountId.first()
                        if (ownerId != null) {
                            cloudSyncManager.syncWallet(wallet, ownerId).onFailure { e ->
                                // Log sync failure but don't block wallet creation
                                android.util.Log.w(
                                        "WalletViewModel",
                                        "Cloud sync failed: ${e.message}"
                                )
                            }
                        }
                        val walletCount = walletManager.getAllWallets().first().size
                        if (walletCount >= 3) {
                            achievementManager.unlockAchievement(Achievement.WALLET_MASTER)
                        }
                        WalletCreationState.Success(wallet)
                    } else {
                        WalletCreationState.Error(
                                result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
        }
    }

    fun deleteWallet(wallet: WalletInfo) {
        viewModelScope.launch {
            walletManager.deleteWallet(wallet)
            val ownerId = preferencesManager.googleAccountId.first()
            if (ownerId != null) {
                cloudSyncManager.deleteWallet(wallet.address).onFailure { e ->
                    android.util.Log.w("WalletViewModel", "Cloud delete failed: ${e.message}")
                }
            }
        }
    }

    fun getWalletsByCrypto(crypto: String): Flow<List<WalletInfo>> {
        return walletManager.getWalletsByCrypto(crypto)
    }

    fun signOut() {
        googleAuthManager.signOut()
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            val ownerId = preferencesManager.googleAccountId.first()
            if (ownerId == null) {
                _isSyncing.value = false
                return@launch
            }
            _isSyncing.value = true
            try {
                cloudSyncManager
                        .fetchWallets()
                        .onSuccess { remoteWallets ->
                            walletManager.syncWalletsFromCloud(ownerId, remoteWallets)
                        }
                        .onFailure { e ->
                            android.util.Log.e(
                                    "WalletViewModel",
                                    "Failed to sync from cloud: ${e.message}",
                                    e
                            )
                        }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun resetWalletCreationState() {
        _walletCreationResult.value = WalletCreationState.Idle
    }

    sealed class WalletCreationState {
        data object Idle : WalletCreationState()
        data object Loading : WalletCreationState()
        data class Success(val wallet: WalletInfo) : WalletCreationState()
        data class Error(val message: String) : WalletCreationState()
    }
}
