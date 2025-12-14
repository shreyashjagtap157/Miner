package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.ui.viewmodel.UnifiedMiningViewModel
import java.text.NumberFormat
import java.util.Locale
import com.meetmyartist.miner.data.model.MiningStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedMiningDashboard(
    viewModel: UnifiedMiningViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val miningStats by viewModel.miningStats.collectAsState()
    val selectedCrypto by viewModel.selectedCryptocurrency.collectAsState()
    val totalMinedValue by viewModel.totalMinedValue.collectAsState()
    val activeThreadCount by viewModel.activeThreadCount.collectAsState()
    val currentHashrate by viewModel.currentHashrate.collectAsState()
    val cpuUsageLimit by viewModel.cpuUsageLimit.collectAsState()
    val showCryptoSelector by viewModel.showCryptoSelector.collectAsState()
    val maxCores = viewModel.availableCores
    
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    
    // Show cryptocurrency selector dialog
    if (showCryptoSelector) {
        CryptocurrencySelectorDialog(
            cryptos = viewModel.getAllSupportedCryptos(),
            selectedCrypto = selectedCrypto,
            onCryptoSelected = { crypto -> viewModel.selectCryptocurrency(crypto) },
            onDismiss = { viewModel.hideCryptoSelector() }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Unified Mining Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cryptocurrency Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = selectedCrypto?.name ?: "Select Cryptocurrency",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedCrypto?.symbol ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    IconButton(onClick = { viewModel.showCryptoSelector() }) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Change Cryptocurrency",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Algorithm: ${selectedCrypto?.algorithm?.displayName ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mining Statistics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hashrate Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${numberFormat.format(currentHashrate.toLong())}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "H/s",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Hashes Performed Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatLargeNumber(miningStats.totalHashes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hashes",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mining Value Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Mined",
                            style = MaterialTheme.typography.labelMedium
                        )
                        selectedCrypto?.let { crypto ->
                            val minedAmount = totalMinedValue / crypto.unitsPerCoin.toDouble()
                            Text(
                                text = "%.8f %s".format(minedAmount, crypto.symbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${numberFormat.format(totalMinedValue)} ${crypto.minableUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "USD Value",
                            style = MaterialTheme.typography.labelMedium
                        )
                        selectedCrypto?.let { crypto ->
                            val usdValue = (totalMinedValue / crypto.unitsPerCoin.toDouble()) * crypto.currentPrice
                            Text(
                                text = "$%.6f".format(usdValue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resource Management Section
        Text(
            text = "Resource Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Thread Control
                Text(
                    text = "Active Threads: $activeThreadCount / $maxCores",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.decrementThreads() },
                        enabled = activeThreadCount > 0
                    ) {
                        Icon(Icons.Default.Remove, "Decrease threads")
                    }
                    
                    Slider(
                        value = activeThreadCount.toFloat(),
                        onValueChange = { viewModel.setThreadCount(it.toInt()) },
                        valueRange = 0f..maxCores.toFloat(),
                        steps = maxCores - 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { viewModel.incrementThreads() },
                        enabled = activeThreadCount < maxCores
                    ) {
                        Icon(Icons.Default.Add, "Increase threads")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // CPU Usage Limit
                Text(
                    text = "CPU Usage Limit: ${cpuUsageLimit.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = cpuUsageLimit,
                    onValueChange = { viewModel.setCpuUsageLimit(it) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { miningStats.cpuUsage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        miningStats.cpuUsage > 90 -> MaterialTheme.colorScheme.error
                        miningStats.cpuUsage > 70 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Current CPU: ${miningStats.cpuUsage.toInt()}% | Temp: ${miningStats.cpuTemp.toInt()}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Performance Stats Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Performance Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StatRow("Accepted Shares", "${miningStats.acceptedShares}")
                StatRow("Rejected Shares", "${miningStats.rejectedShares}")
                
                val rejectionRate = if (miningStats.acceptedShares > 0) {
                    (miningStats.rejectedShares.toFloat() / (miningStats.acceptedShares + miningStats.rejectedShares)) * 100
                } else 0f
                
                StatRow("Rejection Rate", "%.2f%%".format(rejectionRate))
                StatRow("Uptime", formatUptime(miningStats.uptime))
                StatRow("Power Usage", "%.1fW".format(miningStats.powerUsage))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.optimizeForMaxHashrate() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Max Performance")
            }
            
            OutlinedButton(
                onClick = { viewModel.optimizeForEfficiency() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Eco, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Eco Mode")
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatLargeNumber(number: Long): String {
    return when {
        number >= 1_000_000_000_000 -> "%.2fT".format(number / 1_000_000_000_000.0)
        number >= 1_000_000_000 -> "%.2fB".format(number / 1_000_000_000.0)
        number >= 1_000_000 -> "%.2fM".format(number / 1_000_000.0)
        number >= 1_000 -> "%.2fK".format(number / 1_000.0)
        else -> number.toString()
    }
}

fun formatUptime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, secs)
}
