package com.meetmyartist.miner.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.ui.viewmodel.SystemMonitorViewModel
import com.meetmyartist.miner.utils.SystemInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(
    viewModel: SystemMonitorViewModel = hiltViewModel()
) {
    val batteryHealth by viewModel.batteryHealth.collectAsState()
    val miningStats by viewModel.miningStats.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "System Monitor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Real-time system health monitoring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Battery Health Card
        batteryHealth?.let { health ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        health.temperature > 45f -> MaterialTheme.colorScheme.errorContainer
                        health.level < 20 && !health.isCharging -> MaterialTheme.colorScheme.errorContainer
                        health.temperature > 40f -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
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
                            text = "Battery Health",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Icon(
                            imageVector = if (health.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BatteryInfoItem("Level", "${health.level}%")
                        BatteryInfoItem("Temperature", "${health.temperature}°C")
                        BatteryInfoItem("Voltage", "${health.voltage / 1000f}V")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = health.level / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: ${viewModel.getBatteryHealthDescription(health.health)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = health.chargingSource,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (health.temperature > 40f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Battery temperature is high",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // CPU Performance Card
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
                        text = "CPU Performance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CpuInfoItem("Temperature", "${miningStats.cpuTemp.toInt()}°C")
                    CpuInfoItem("Usage", "${miningStats.cpuUsage.toInt()}%")
                    CpuInfoItem("Power", "${miningStats.powerUsage}W")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = (miningStats.cpuUsage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        miningStats.cpuUsage > 90 -> MaterialTheme.colorScheme.error
                        miningStats.cpuUsage > 70 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                if (miningStats.cpuTemp > 80f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CPU temperature is high - consider reducing load",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mining Performance Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Mining Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MiningMetricRow("Hashrate", "${miningStats.hashrate.toInt()} H/s")
                MiningMetricRow("Accepted Shares", "${miningStats.acceptedShares}")
                MiningMetricRow("Rejected Shares", "${miningStats.rejectedShares}")
                
                val rejectionRate = if (miningStats.acceptedShares > 0) {
                    (miningStats.rejectedShares.toFloat() / (miningStats.acceptedShares + miningStats.rejectedShares)) * 100
                } else 0f
                
                MiningMetricRow("Rejection Rate", "${"%.2f".format(rejectionRate)}%")
                
                if (rejectionRate > 5f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ High rejection rate - check pool connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recommendations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                batteryHealth?.let { health ->
                    if (health.temperature > 40f) {
                        RecommendationItem(
                            icon = Icons.Default.AcUnit,
                            text = "Cool down device - battery temperature is elevated"
                        )
                    }
                    
                    if (!health.isCharging && health.level < 30) {
                        RecommendationItem(
                            icon = Icons.Default.BatteryAlert,
                            text = "Connect charger for stable mining"
                        )
                    }
                }
                
                if (miningStats.cpuTemp > 75f) {
                    RecommendationItem(
                        icon = Icons.Rounded.Tune,
                        text = "Reduce thread count or CPU usage limit"
                    )
                }
                
                if (miningStats.cpuUsage < 50f && miningStats.hashrate > 0) {
                    RecommendationItem(
                        icon = Icons.Rounded.TrendingUp,
                        text = "You can increase performance settings"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // System Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                systemInfo.let { info ->
                    Text("CPU Temperature: ${info.cpuTemperature}°C")
                    Text("CPU Usage: ${info.cpuUsage}%")
                    Text("RAM Usage: ${info.ramUsage}%")
                    Text("Disk Usage: ${info.diskUsage}%")
                    Text("Battery Level: ${info.batteryLevel}%")
                    Text("Is Charging: ${info.isCharging}")
                }
            }
        }
    }
}

@Composable
fun BatteryInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CpuInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MiningMetricRow(label: String, value: String) {
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

@Composable
fun RecommendationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}