package com.meetmyartist.miner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meetmyartist.miner.data.model.Cryptocurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptocurrencySelectorDialog(
    cryptos: List<Cryptocurrency>,
    selectedCrypto: Cryptocurrency?,
    onCryptoSelected: (Cryptocurrency) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCPUFriendlyOnly by remember { mutableStateOf(false) }
    
    val filteredCryptos = remember(cryptos, searchQuery, showCPUFriendlyOnly) {
        cryptos.filter { crypto ->
            val matchesSearch = crypto.name.contains(searchQuery, ignoreCase = true) ||
                    crypto.symbol.contains(searchQuery, ignoreCase = true)
            val matchesFilter = !showCPUFriendlyOnly || isCPUFriendly(crypto)
            matchesSearch && matchesFilter
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Cryptocurrency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // CPU-Friendly Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show CPU-friendly only",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = showCPUFriendlyOnly,
                        onCheckedChange = { showCPUFriendlyOnly = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${filteredCryptos.size} cryptocurrencies",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Crypto List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCryptos) { crypto ->
                        CryptoListItem(
                            crypto = crypto,
                            isSelected = crypto.symbol == selectedCrypto?.symbol,
                            onClick = { onCryptoSelected(crypto) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoListItem(
    crypto: Cryptocurrency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = crypto.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isCPUFriendly(crypto)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "CPU",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
                
                Text(
                    text = crypto.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = crypto.algorithm.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun isCPUFriendly(crypto: Cryptocurrency): Boolean {
    val cpuFriendlyAlgorithms = setOf(
        com.meetmyartist.miner.data.model.MiningAlgorithm.RANDOMX,
        com.meetmyartist.miner.data.model.MiningAlgorithm.CUCKATOO32,
        com.meetmyartist.miner.data.model.MiningAlgorithm.VERTCOIN,
        com.meetmyartist.miner.data.model.MiningAlgorithm.ERGO,
        com.meetmyartist.miner.data.model.MiningAlgorithm.KASPA,
        com.meetmyartist.miner.data.model.MiningAlgorithm.CRYPTONIGHT,
        com.meetmyartist.miner.data.model.MiningAlgorithm.BLAKE3,
        com.meetmyartist.miner.data.model.MiningAlgorithm.THETA_EDGE
    )
    return crypto.algorithm in cpuFriendlyAlgorithms
}
