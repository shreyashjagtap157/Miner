package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.data.model.CryptoAlgorithm
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.ui.viewmodel.ConfigurationViewModel
import com.meetmyartist.miner.ui.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val allConfigs by viewModel.allConfigs.collectAsState()
    val wallets by walletViewModel.allWallets.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf<MiningConfig?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mining Configurations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.syncFromCloud() }) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync Configurations")
                    }
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Configuration")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allConfigs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Configurations",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Create a mining configuration to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = allConfigs,
                    key = { config -> config.id }
                ) { config ->
                    ConfigItem(
                        config = config,
                        onDelete = { viewModel.deleteConfig(config) },
                        onSetActive = { viewModel.setActiveConfig(config) },
                        onSelect = {
                            selectedConfig = config
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog || selectedConfig != null) {
        ConfigDialog(
            existingConfig = selectedConfig,
            viewModel = viewModel,
            onDismiss = {
                showCreateDialog = false
                selectedConfig = null
            },
            onConfirm = { cryptocurrency, algorithm, poolUrl, poolPort, walletAddress, workerName ->
                if (selectedConfig != null) {
                    viewModel.updateConfig(
                        selectedConfig!!.copy(
                            cryptocurrency = cryptocurrency,
                            algorithm = algorithm,
                            poolUrl = poolUrl,
                            poolPort = poolPort,
                            walletAddress = walletAddress,
                            workerName = workerName
                        )
                    )
                } else {
                    viewModel.createConfig(
                        cryptocurrency,
                        algorithm,
                        poolUrl,
                        poolPort,
                        walletAddress,
                        workerName
                    )
                }
                showCreateDialog = false
                selectedConfig = null
            }
        )
    }
}

@Composable
fun ConfigItem(
    config: MiningConfig,
    onDelete: () -> Unit,
    onSetActive: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = "Configuration",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${config.workerName} (${config.cryptocurrency})",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = config.poolUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (config.isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSetActive) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Set Active"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDialog(
    existingConfig: MiningConfig? = null,
    viewModel: ConfigurationViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String) -> Unit
) {
    val supportedAlgorithms = viewModel.getSupportedAlgorithms()
    var cryptocurrency by remember { mutableStateOf(existingConfig?.cryptocurrency ?: "") }
    var algorithm by remember { mutableStateOf(existingConfig?.algorithm ?: supportedAlgorithms.first().name) }
    var poolUrl by remember { mutableStateOf(existingConfig?.poolUrl ?: "") }
    var poolPort by remember { mutableStateOf(existingConfig?.poolPort?.toString() ?: "") }
    var walletAddress by remember { mutableStateOf(existingConfig?.walletAddress ?: "") }
    var workerName by remember { mutableStateOf(existingConfig?.workerName ?: "worker1") }
    val selectedAlgorithm by remember(algorithm) {
        derivedStateOf { supportedAlgorithms.find { it.name == algorithm } }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (existingConfig != null) "Edit Configuration" else "Create New Configuration",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = cryptocurrency,
                    onValueChange = { cryptocurrency = it },
                    label = { Text("Cryptocurrency (e.g., XMR)") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedAlgorithm?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Algorithm") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        supportedAlgorithms.forEach { algo ->
                            DropdownMenuItem(
                                text = { Text(text = algo.name) },
                                onClick = {
                                    algorithm = algo.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = poolUrl,
                    onValueChange = { poolUrl = it },
                    label = { Text("Pool URL") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = poolPort,
                    onValueChange = { poolPort = it },
                    label = { Text("Pool Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = walletAddress,
                    onValueChange = { walletAddress = it },
                    label = { Text("Wallet Address") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = workerName,
                    onValueChange = { workerName = it },
                    label = { Text("Worker Name") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val port = poolPort.toIntOrNull() ?: 0
                            if (port > 0) {
                                onConfirm(cryptocurrency, algorithm, poolUrl, port, walletAddress, workerName)
                            }
                        }
                    ) {
                        Text(if (existingConfig != null) "Update" else "Create")
                    }
                }
            }
        }
    }
}
