package com.meetmyartist.miner.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meetmyartist.miner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Modern Google Authentication Manager using Credential Manager API. Replaces the deprecated
 * GoogleSignIn API with the new Credential Manager.
 */
@Singleton
class GoogleAuthManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedOut)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleAccount?>(null)
    val currentAccount: StateFlow<GoogleAccount?> = _currentAccount.asStateFlow()

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Check if user is already signed in with Firebase
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val account =
                    GoogleAccount(
                            id = currentUser.uid,
                            email = currentUser.email,
                            displayName = currentUser.displayName,
                            photoUrl = currentUser.photoUrl?.toString()
                    )
            _currentAccount.value = account
            _signInState.value = SignInState.SignedIn(account)
        }
    }

    /** Creates a GetCredentialRequest for Google Sign-In using Credential Manager. */
    fun getCredentialRequest(): GetCredentialRequest {
        val webClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption =
                GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(true)
                        .setNonce(generateNonce())
                        .build()

        return GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    }

    /** Handles the credential response from Credential Manager. */
    suspend fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                        authenticateWithFirebase(googleIdTokenCredential)
                    } catch (e: GoogleIdTokenParsingException) {
                        _signInState.value =
                                SignInState.Error("Invalid Google ID token: ${e.message}")
                    }
                } else {
                    _signInState.value = SignInState.Error("Unsupported credential type")
                }
            }
            else -> {
                _signInState.value = SignInState.Error("Unsupported credential type")
            }
        }
    }

    /** Handles sign-in errors from Credential Manager. */
    fun handleSignInError(exception: GetCredentialException) {
        _signInState.value = SignInState.Error(exception.message ?: "Sign in failed")
    }

    /** Signs out the current user from both Firebase and clears credentials. */
    fun signOut() {
        scope.launch {
            try {
                firebaseAuth.signOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                _currentAccount.value = null
                _signInState.value = SignInState.SignedOut
            } catch (e: Exception) {
                // Still sign out from Firebase even if credential clearing fails
                _currentAccount.value = null
                _signInState.value = SignInState.SignedOut
            }
        }
    }

    /** Authenticates with Firebase using the Google ID token. */
    private suspend fun authenticateWithFirebase(googleIdTokenCredential: GoogleIdTokenCredential) {
        try {
            val idToken = googleIdTokenCredential.idToken
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val account =
                        GoogleAccount(
                                id = firebaseUser.uid,
                                email = firebaseUser.email,
                                displayName = firebaseUser.displayName,
                                photoUrl = firebaseUser.photoUrl?.toString()
                        )
                _currentAccount.value = account
                _signInState.value = SignInState.SignedIn(account)
            } else {
                _signInState.value = SignInState.Error("Firebase authentication failed")
            }
        } catch (e: Exception) {
            _signInState.value = SignInState.Error(e.message ?: "Authentication failed")
        }
    }

    /** Generates a nonce for Google Sign-In security. */
    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /** Data class representing a signed-in Google account. */
    data class GoogleAccount(
            val id: String,
            val email: String?,
            val displayName: String?,
            val photoUrl: String?
    )

    /** Sealed class representing the sign-in state. */
    sealed class SignInState {
        data object SignedOut : SignInState()
        data class SignedIn(val account: GoogleAccount) : SignInState()
        data class Error(val message: String) : SignInState()
    }
}
