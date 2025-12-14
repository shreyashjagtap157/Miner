package com.meetmyartist.miner.ui.screens

import com.meetmyartist.miner.mining.MiningEngine
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDashboardScreen(
    viewModel: MiningViewModel = hiltViewModel()
) {
    val miningState by viewModel.miningState.collectAsState()
    val miningStats by viewModel.miningStats.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val scrollState = rememberScrollState()

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refreshStats() }
    ) {
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

            // Animated Mining Status Card
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MiningStatusCard(
                    miningState = miningState,
                    activeConfig = activeConfig,
                    onStartClick = { viewModel.startMining() },
                    onStopClick = { viewModel.stopMining() },
                    onPauseClick = { viewModel.pauseMining() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mining Stats Cards with Animation
            AnimatedVisibility(
                visible = miningState == MiningEngine.MiningState.MINING,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Hashrate",
                            value = "${"%.2f".format(miningStats.hashrate)} H/s",
                            icon = Icons.Default.Speed,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AnimatedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Uptime",
                            value = formatUptime(miningStats.uptime),
                            icon = Icons.Default.Timer,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "CPU Temp",
                            value = "${"%.1f".format(miningStats.cpuTemp)}°C",
                            icon = Icons.Default.Thermostat,
                            color = if (miningStats.cpuTemp > 70)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.tertiary
                        )

                        AnimatedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Shares",
                            value = "${miningStats.acceptedShares}/${miningStats.rejectedShares}",
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Card
            PerformanceCard(miningStats = miningStats)

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Card
            QuickActionsCard(
                miningState = miningState,
                onStartClick = { viewModel.startMining() },
                onStopClick = { viewModel.stopMining() }
            )
        }
    }
}

@Composable
private fun MiningStatusCard(
    miningState: MiningEngine.MiningState,
    activeConfig: com.meetmyartist.miner.data.model.MiningConfig?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (miningState) {
                MiningEngine.MiningState.MINING -> MaterialTheme.colorScheme.primaryContainer
                MiningEngine.MiningState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedStatusChip(state = miningState)
                }

                // Mining Animation
                if (miningState == MiningEngine.MiningState.MINING) {
                    MiningAnimation()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeConfig != null) {
                Text(
                    text = "Mining: ${activeConfig.cryptocurrency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Algorithm: ${activeConfig.algorithm}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pool: ${activeConfig.poolUrl ?: "Not connected"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No active configuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (miningState) {
                    MiningEngine.MiningState.STOPPED, MiningEngine.MiningState.PAUSED -> {
                        Button(
                            onClick = onStartClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Mining")
                        }
                    }
                    MiningEngine.MiningState.MINING -> {
                        OutlinedButton(
                            onClick = onPauseClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = onStopClick,
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
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun AnimatedStatusChip(state: MiningEngine.MiningState) {
    val statusColor = when (state) {
        MiningEngine.MiningState.MINING -> Color(0xFF4CAF50)
        MiningEngine.MiningState.PAUSED -> Color(0xFFFF9800)
        MiningEngine.MiningState.THROTTLED -> Color(0xFFFF5722)
        MiningEngine.MiningState.ERROR -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    val statusText = when (state) {
        MiningEngine.MiningState.STOPPED -> "Stopped"
        MiningEngine.MiningState.STARTING -> "Starting..."
        MiningEngine.MiningState.MINING -> "Mining"
        MiningEngine.MiningState.PAUSED -> "Paused"
        MiningEngine.MiningState.THROTTLED -> "Throttled"
        MiningEngine.MiningState.ERROR -> "Error"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    SuggestionChip(
        onClick = {},
        label = { Text(statusText) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = statusColor.copy(
                alpha = if (state == MiningEngine.MiningState.MINING) alpha else 1f
            ),
            labelColor = Color.White
        )
    )
}

@Composable
private fun MiningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "mining")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Mining",
        modifier = Modifier
            .size(48.dp)
            .rotate(rotation),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun AnimatedStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val scale by rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
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
private fun PerformanceCard(miningStats: com.meetmyartist.miner.data.model.MiningStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PerformanceIndicator(
                label = "CPU Usage",
                value = miningStats.cpuUsage.toInt(),
                maxValue = 100,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            PerformanceIndicator(
                label = "Temperature",
                value = miningStats.cpuTemp.toInt(),
                maxValue = 100,
                color = if (miningStats.cpuTemp > 70)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(8.dp))

            PerformanceIndicator(
                label = "Power Usage",
                value = (miningStats.powerUsage * 10).toInt(),
                maxValue = 50,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PerformanceIndicator(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value / $maxValue",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
        )
    }
}

@Composable
private fun QuickActionsCard(
    miningState: MiningEngine.MiningState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = { /* Navigate to configuration */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Config")
                }

                ElevatedButton(
                    onClick = { /* Navigate to wallets */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wallets")
                }
            }
        }
    }
}
