package com.meetmyartist.miner.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.auth.GoogleAuthManager
import com.meetmyartist.miner.data.model.WalletInfo
import com.meetmyartist.miner.data.model.WalletService
import com.meetmyartist.miner.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val wallets by viewModel.allWallets.collectAsState()
    val signInState by viewModel.signInState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showAddWalletDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = "Wallets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (signInState is GoogleAuthManager.SignInState.SignedIn) {
                    IconButton(onClick = { viewModel.syncFromCloud() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync Wallets")
                        }
                    }
                }
                IconButton(onClick = { showAddWalletDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Wallet")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Card
        when (signInState) {
            is GoogleAuthManager.SignInState.SignedOut -> {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Link Google Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Connect your Google account to sync wallets across devices",
                                style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val request = viewModel.getCredentialRequest()
                                            val result =
                                                    credentialManager.getCredential(
                                                            request = request,
                                                            context = context
                                                    )
                                            viewModel.handleSignInResult(result)
                                        } catch (e: GetCredentialException) {
                                            viewModel.handleSignInError(e)
                                            Toast.makeText(
                                                            context,
                                                            "Sign in failed: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        }
                                    }
                                }
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            is GoogleAuthManager.SignInState.SignedIn -> {
                val account = (signInState as GoogleAuthManager.SignInState.SignedIn).account
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Signed in as", style = MaterialTheme.typography.bodySmall)
                            Text(
                                    text = account.email ?: "Unknown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { viewModel.signOut() }) { Text("Sign Out") }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            is GoogleAuthManager.SignInState.Error -> {
                val error = (signInState as GoogleAuthManager.SignInState.Error).message
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                ) {
                    Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Wallets List
        if (wallets.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No Wallets Added", style = MaterialTheme.typography.titleMedium)
                    Text(
                            text = "Add a wallet to receive mining rewards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets) { wallet ->
                    WalletCard(wallet = wallet, onDelete = { viewModel.deleteWallet(wallet) })
                }
            }
        }
    }

    if (showAddWalletDialog) {
        AddWalletDialog(
                onDismiss = { showAddWalletDialog = false },
                onAddWallet = { crypto, address, privateKey, label, service ->
                    viewModel.createWallet(crypto, address, privateKey, label, service)
                    showAddWalletDialog = false
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletCard(wallet: WalletInfo, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = wallet.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = wallet.cryptocurrency,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = wallet.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                        onClick = {},
                        label = { Text(wallet.walletService.serviceName) },
                        leadingIcon = {
                            Icon(Icons.Default.AccountBalance, contentDescription = null)
                        }
                )

                if (wallet.linkedGoogleId != null) {
                    AssistChip(
                            onClick = {},
                            label = { Text("Linked") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Wallet") },
                text = {
                    Text(
                            "Are you sure you want to delete this wallet? This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                onDelete()
                                showDeleteDialog = false
                            }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletDialog(
        onDismiss: () -> Unit,
        onAddWallet: (String, String, String?, String, WalletService) -> Unit
) {
    var cryptocurrency by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf(WalletService.CUSTOM) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                        text = "Add Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Wallet Label") },
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                        value = cryptocurrency,
                        onValueChange = { cryptocurrency = it },
                        label = { Text("Cryptocurrency (e.g., BTC, ETH, XMR)") },
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                            value = selectedService.serviceName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Wallet Service") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                    ) {
                        WalletService.entries.forEach { service ->
                            DropdownMenuItem(
                                    text = { Text(service.serviceName) },
                                    onClick = {
                                        selectedService = service
                                        expanded = false
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Wallet Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text("Private Key (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Stored securely with encryption") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                            onClick = {
                                if (cryptocurrency.isNotBlank() &&
                                                address.isNotBlank() &&
                                                label.isNotBlank()
                                ) {
                                    onAddWallet(
                                            cryptocurrency,
                                            address,
                                            privateKey.takeIf { it.isNotBlank() },
                                            label,
                                            selectedService
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled =
                                    cryptocurrency.isNotBlank() &&
                                            address.isNotBlank() &&
                                            label.isNotBlank()
                    ) { Text("Add") }
                }
            }
        }
    }
}
