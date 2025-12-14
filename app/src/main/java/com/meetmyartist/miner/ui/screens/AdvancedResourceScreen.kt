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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.data.model.CorePriority
import com.meetmyartist.miner.ui.viewmodel.AdvancedResourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedResourceScreen(
    viewModel: AdvancedResourceViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val activeThreadCount by viewModel.activeThreadCount.collectAsState()
    val currentHashrate by viewModel.currentHashrate.collectAsState()
    val perCoreStats by viewModel.perCoreStats.collectAsState()
    val enablePerCoreControl by viewModel.enablePerCoreControl.collectAsState()
    val enableHashrateLimit by viewModel.enableHashrateLimit.collectAsState()
    val maxHashrate by viewModel.maxHashrate.collectAsState()
    
    // Get available cores dynamically from viewModel
    val maxCores = viewModel.availableCores
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Advanced Resource Control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Real-time control over mining resources",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Current Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Active Threads",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$activeThreadCount / $maxCores",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Hashrate",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${currentHashrate.toInt()} H/s",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Thread Count Control
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mining Threads: $activeThreadCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.decrementThreads() },
                            enabled = activeThreadCount > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease threads")
                        }
                        
                        IconButton(
                            onClick = { viewModel.incrementThreads() },
                            enabled = activeThreadCount < maxCores
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase threads")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Slider(
                    value = activeThreadCount.toFloat(),
                    onValueChange = { viewModel.setThreadCount(it.toInt()) },
                    valueRange = 0f..maxCores.toFloat(),
                    steps = maxCores - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Drag slider or use +/- buttons for instant adjustment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hashrate Limit Control
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hashrate Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cap maximum mining speed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = enableHashrateLimit,
                        onCheckedChange = { viewModel.toggleHashrateLimit(it) }
                    )
                }
                
                if (enableHashrateLimit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var hashrateInput by remember { mutableStateOf(maxHashrate?.toInt()?.toString() ?: "1000") }
                    
                    OutlinedTextField(
                        value = hashrateInput,
                        onValueChange = { 
                            hashrateInput = it
                            it.toDoubleOrNull()?.let { value ->
                                viewModel.setHashrateLimit(value)
                            }
                        },
                        label = { Text("Max Hashrate (H/s)") },
                        suffix = { Text("H/s") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500, 1000, 2000, 5000).forEach { preset ->
                            FilterChip(
                                selected = maxHashrate?.toInt() == preset,
                                onClick = {
                                    hashrateInput = preset.toString()
                                    viewModel.setHashrateLimit(preset.toDouble())
                                },
                                label = { Text("${preset}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Per-Core Control Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Advanced Per-Core Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure each CPU core individually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = enablePerCoreControl,
                        onCheckedChange = { viewModel.togglePerCoreControl(it) }
                    )
                }
            }
        }
        
        // Per-Core Configuration
        if (enablePerCoreControl) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Individual Core Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            perCoreStats.entries.sortedBy { it.key }.forEach { (coreId, stats) ->
                CoreControlCard(
                    coreId = coreId,
                    stats = stats,
                    onUsageChange = { usage -> viewModel.setCoreUsage(coreId, usage) },
                    onPriorityChange = { priority -> viewModel.setCorePriority(coreId, priority) },
                    onToggle = { enabled -> viewModel.toggleCore(coreId, enabled) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Show inactive cores
            (0 until maxCores).filter { it !in perCoreStats.keys }.forEach { coreId ->
                InactiveCoreCard(
                    coreId = coreId,
                    onEnable = { viewModel.enableCore(coreId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setThreadCount(maxCores) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Max Threads")
            }
            
            OutlinedButton(
                onClick = { viewModel.setThreadCount(0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Stop All")
            }
        }
    }
}

@Composable
fun CoreControlCard(
    coreId: Int,
    stats: com.meetmyartist.miner.mining.CoreStats,
    onUsageChange: (Int) -> Unit,
    onPriorityChange: (CorePriority) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Core #$coreId",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.hashrate.toInt()} H/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Switch(
                    checked = true,
                    onCheckedChange = onToggle,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Usage: ${stats.usage}%",
                style = MaterialTheme.typography.bodySmall
            )
            
            Slider(
                value = stats.usage.toFloat(),
                onValueChange = { onUsageChange(it.toInt()) },
                valueRange = 10f..100f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InactiveCoreCard(
    coreId: Int,
    onEnable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Core #$coreId",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onEnable) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Enable core",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
