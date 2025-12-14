package com.meetmyartist.miner.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.fragment.app.FragmentActivity
import com.meetmyartist.miner.auth.BiometricAuthManager
import com.meetmyartist.miner.mining.MiningEngine
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MiningViewModel = hiltViewModel()
) {
    val miningState by viewModel.miningState.collectAsState()
    val miningStats by viewModel.miningStats.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val context = LocalContext.current
    val biometricAuthManager = remember { BiometricAuthManager() }

    val secureAction: (onSecure: () -> Unit) -> Unit = remember(biometricEnabled, context) {
        { action ->
            if (biometricEnabled) {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    biometricAuthManager.showBiometricPrompt(
                        activity = activity,
                        onSuccess = action,
                        onFailure = {
                            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    action()
                }
            } else {
                action()
            }
        }
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Mining Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mining Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(state = miningState)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (activeConfig != null) {
                    Text(
                        text = "Mining: ${activeConfig?.cryptocurrency}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Algorithm: ${activeConfig?.algorithm}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "No active configuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mining Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Hashrate",
                value = "${"%.2f".format(miningStats.hashrate)} H/s",
                icon = Icons.Default.Speed
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Uptime",
                value = formatUptime(miningStats.uptime),
                icon = Icons.Default.Timer
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "CPU Temp",
                value = "${"%.1f".format(miningStats.cpuTemp)}°C",
                icon = Icons.Default.Thermostat
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                title = "CPU Usage",
                value = "${"%.1f".format(miningStats.cpuUsage)}%",
                icon = Icons.Default.Memory
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Shares Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Shares",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Accepted",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${miningStats.acceptedShares}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Rejected",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${miningStats.rejectedShares}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Total Hashes",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatNumber(miningStats.totalHashes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Control Buttons
        if (activeConfig != null) {
            when (miningState) {
                MiningEngine.MiningState.STOPPED -> {
                    Button(
                        onClick = { secureAction { viewModel.startMining(activeConfig!!) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Mining")
                    }
                }
                
                MiningEngine.MiningState.MINING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.pauseMining() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        
                        Button(
                            onClick = { viewModel.stopMining() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
                
                MiningEngine.MiningState.PAUSED, MiningEngine.MiningState.THROTTLED, MiningEngine.MiningState.ERROR, MiningEngine.MiningState.STARTING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { secureAction { viewModel.resumeMining() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                        
                        Button(
                            onClick = { viewModel.stopMining() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
                
                else -> {}
            }
        }
    }
}

@Composable
fun StatusChip(state: MiningEngine.MiningState) {
    val (color, text) = when (state) {
        MiningEngine.MiningState.STOPPED -> MaterialTheme.colorScheme.error to "Stopped"
        MiningEngine.MiningState.STARTING -> MaterialTheme.colorScheme.tertiary to "Starting"
        MiningEngine.MiningState.MINING -> MaterialTheme.colorScheme.primary to "Running"
        MiningEngine.MiningState.PAUSED -> MaterialTheme.colorScheme.secondary to "Paused"
        MiningEngine.MiningState.THROTTLED -> MaterialTheme.colorScheme.error to "Throttled"
        MiningEngine.MiningState.ERROR -> MaterialTheme.colorScheme.error to "Error"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> "${"%.1f".format(number / 1_000_000_000.0)}B"
        number >= 1_000_000 -> "${"%.1f".format(number / 1_000_000.0)}M"
        number >= 1_000 -> "${"%.1f".format(number / 1_000.0)}K"
        else -> number.toString()
    }
}
