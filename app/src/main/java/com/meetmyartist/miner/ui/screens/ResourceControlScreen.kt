package com.meetmyartist.miner.ui.screens

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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.meetmyartist.miner.ui.navigation.Screen
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceControlScreen(
    viewModel: MiningViewModel = hiltViewModel(),
    navController: NavController = rememberNavController()
) {
    val selectedThreads by viewModel.selectedThreads.collectAsState()
    val cpuUsageLimit by viewModel.cpuUsageLimit.collectAsState()
    val maxTemperature by viewModel.maxTemperature.collectAsState()
    val thermalThrottleEnabled by viewModel.thermalThrottleEnabled.collectAsState()
    val batteryOptimizationEnabled by viewModel.batteryOptimizationEnabled.collectAsState()
    val miningStats by viewModel.miningStats.collectAsState()
    
    val maxThreads = Runtime.getRuntime().availableProcessors()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resource Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = {
                    navController.navigate(Screen.AdvancedResource.route)
                }
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Advanced")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // CPU Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CPU Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(
                    label = "Available Cores",
                    value = "$maxThreads"
                )
                InfoRow(
                    label = "Current Temperature",
                    value = "${"%.1f".format(miningStats.cpuTemp)}°C"
                )
                InfoRow(
                    label = "Current Usage",
                    value = "${"%.1f".format(miningStats.cpuUsage)}%"
                )
                InfoRow(
                    label = "Estimated Power",
                    value = "${"%.2f".format(miningStats.powerUsage)}W"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Thread Count Control
        Text(
            text = "CPU Threads: $selectedThreads / $maxThreads",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = selectedThreads.toFloat(),
            onValueChange = { viewModel.updateSelectedThreads(it.roundToInt()) },
            valueRange = 1f..maxThreads.toFloat(),
            steps = maxThreads - 2,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "More threads = higher hashrate but more power consumption",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // CPU Usage Limit
        Text(
            text = "CPU Usage Limit: $cpuUsageLimit%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = cpuUsageLimit.toFloat(),
            onValueChange = { viewModel.updateCpuUsageLimit(it.roundToInt()) },
            valueRange = 10f..100f,
            steps = 17,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Limits maximum CPU usage to prevent overheating",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Temperature Limit
        Text(
            text = "Max Temperature: ${"%.0f".format(maxTemperature)}°C",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = maxTemperature,
            onValueChange = { viewModel.updateMaxTemperature(it) },
            valueRange = 60f..90f,
            steps = 29,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Mining will pause if temperature exceeds this threshold",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Thermal Throttling
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thermal Throttling",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically reduce performance when overheating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = thermalThrottleEnabled,
                    onCheckedChange = { viewModel.updateThermalThrottle(it) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Battery Optimization
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pause mining when battery is low",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = batteryOptimizationEnabled,
                    onCheckedChange = { viewModel.updateBatteryOptimization(it) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Performance Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Performance Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Keep device cool for better performance\n" +
                               "• Use lower thread count on older devices\n" +
                               "• Enable battery optimization for safety\n" +
                               "• Monitor temperature regularly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
