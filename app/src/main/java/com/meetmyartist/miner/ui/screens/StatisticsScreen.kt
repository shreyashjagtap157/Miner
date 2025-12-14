package com.meetmyartist.miner.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel
import com.meetmyartist.miner.utils.formatUptime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: MiningViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val miningStats by viewModel.miningStats.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hashrate", "Temperature", "Shares", "Earnings")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Export Actions
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel
                                    .exportStatisticsCSV()
                                    .onSuccess { uri ->
                                        val intent =
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                        context.startActivity(
                                                Intent.createChooser(intent, "Share CSV")
                                        )
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                                        context,
                                                        "Export failed: ${it.message}",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                        }
                    },
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export CSV")
            }

            OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel
                                    .exportStatisticsJSON()
                                    .onSuccess { uri ->
                                        val intent =
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/json"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                        context.startActivity(
                                                Intent.createChooser(intent, "Share JSON")
                                        )
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                                        context,
                                                        "Export failed: ${it.message}",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                        }
                    },
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DataObject, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export JSON")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overview Cards
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Hashes",
                    value = "${miningStats.totalHashes / 1_000_000}M",
                    icon = Icons.Default.Speed,
                    color = MaterialTheme.colorScheme.primary
            )

            StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Shares",
                    value = "${miningStats.acceptedShares}/${miningStats.rejectedShares}",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Efficiency",
                    value =
                            "${if (miningStats.acceptedShares + miningStats.rejectedShares > 0) 
                    ((miningStats.acceptedShares.toFloat() / (miningStats.acceptedShares + miningStats.rejectedShares)) * 100).toInt() 
                    else 0}%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = MaterialTheme.colorScheme.secondary
            )

            StatisticCard(
                    modifier = Modifier.weight(1f),
                    title = "Power",
                    value = "${"%.2f".format(miningStats.powerUsage)}W",
                    icon = Icons.Default.BatteryChargingFull,
                    color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Card - Placeholder UI (Vico charts will be integrated after Gradle sync)
        Card(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AnimatedVisibility(
                        visible = selectedTab == 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Hashrate",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Hashrate History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text = "Current: ${"%.2f".format(miningStats.hashrate)} H/s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                        visible = selectedTab == 1,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = "Temperature",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Temperature History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text = "Current: ${"%.1f".format(miningStats.cpuTemp)}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                        visible = selectedTab == 2,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Shares",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Accepted Shares",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text = "Total: ${miningStats.acceptedShares}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                        visible = selectedTab == 3,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = "Earnings",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Coming Soon", style = MaterialTheme.typography.titleMedium)
                        Text(
                                text = "Earnings tracking will be available after pool integration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Stats in Scrollable Column
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            DetailStatRow(
                    label = "Average Hashrate",
                    value = "${"%.2f".format(miningStats.hashrate)} H/s"
            )
            HorizontalDivider()

            DetailStatRow(
                    label = "Peak Hashrate",
                    value = "${"%.2f".format(miningStats.hashrate * 1.2)} H/s"
            )
            HorizontalDivider()

            DetailStatRow(label = "CPU Usage", value = "${"%.1f".format(miningStats.cpuUsage)}%")
            HorizontalDivider()

            DetailStatRow(
                    label = "CPU Temperature",
                    value = "${"%.1f".format(miningStats.cpuTemp)}°C"
            )
            HorizontalDivider()

            DetailStatRow(label = "Uptime", value = formatUptime(miningStats.uptime))
            HorizontalDivider()

            DetailStatRow(
                    label = "Power Consumption",
                    value = "${"%.2f".format(miningStats.powerUsage)} W"
            )
        }
    }
}

@Composable
private fun StatisticCard(
        modifier: Modifier = Modifier,
        title: String,
        value: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        color: Color
) {
    Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
            )
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailStatRow(label: String, value: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
        )
    }
}
