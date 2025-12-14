package com.meetmyartist.miner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Advanced Resource Control Screen with real-time visualizations,
 * detailed per-core monitoring, and intelligent resource allocation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedResourceControlScreen(
    viewModel: MiningViewModel = hiltViewModel()
) {
    val selectedThreads by viewModel.selectedThreads.collectAsState()
    val cpuUsageLimit by viewModel.cpuUsageLimit.collectAsState()
    val maxTemperature by viewModel.maxTemperature.collectAsState()
    val thermalThrottleEnabled by viewModel.thermalThrottleEnabled.collectAsState()
    val batteryOptimizationEnabled by viewModel.batteryOptimizationEnabled.collectAsState()
    val miningStats by viewModel.miningStats.collectAsState()
    val miningState by viewModel.miningState.collectAsState()
    
    val maxThreads = Runtime.getRuntime().availableProcessors()
    val scrollState = rememberScrollState()
    
    // Animated values for smooth transitions
    val animatedCpuUsage by animateFloatAsState(
        targetValue = miningStats.cpuUsage,
        animationSpec = tween(durationMillis = 500),
        label = "cpuUsage"
    )
    
    val animatedTemperature by animateFloatAsState(
        targetValue = miningStats.cpuTemp,
        animationSpec = tween(durationMillis = 500),
        label = "temperature"
    )
    
    // Color transitions based on values
    val tempColor by animateColorAsState(
        targetValue = when {
            miningStats.cpuTemp < 50f -> Color(0xFF4CAF50)
            miningStats.cpuTemp < 70f -> Color(0xFFFFA726)
            else -> Color(0xFFEF5350)
        },
        label = "tempColor"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Advanced Resource Control",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Real-time monitoring & intelligent optimization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Real-time Performance Dashboard
        PerformanceDashboard(
            cpuUsage = animatedCpuUsage,
            temperature = animatedTemperature,
            powerUsage = miningStats.powerUsage,
            hashrate = miningStats.hashrate,
            tempColor = tempColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Per-Core Activity Visualization
        PerCoreActivityCard(
            selectedThreads = selectedThreads,
            maxThreads = maxThreads,
            isActive = miningState.name == "RUNNING",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Advanced Thread Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thread Allocation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thread count with presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Threads",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$selectedThreads / $maxThreads cores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = selectedThreads.toFloat(),
                    onValueChange = { viewModel.updateSelectedThreads(it.roundToInt()) },
                    valueRange = 1f..maxThreads.toFloat(),
                    steps = maxThreads - 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetButton(
                        text = "ECO",
                        description = "25%",
                        onClick = { viewModel.updateSelectedThreads(maxOf(1, maxThreads / 4)) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        text = "BALANCED",
                        description = "50%",
                        onClick = { viewModel.updateSelectedThreads(maxOf(1, maxThreads / 2)) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        text = "PERFORMANCE",
                        description = "75%",
                        onClick = { viewModel.updateSelectedThreads(maxOf(1, maxThreads * 3 / 4)) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        text = "MAX",
                        description = "100%",
                        onClick = { viewModel.updateSelectedThreads(maxThreads) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Performance impact indicator
                PerformanceImpactIndicator(
                    threadPercentage = (selectedThreads.toFloat() / maxThreads) * 100
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // CPU Usage Limiter with visual feedback
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CPU Usage Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Usage Limit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$cpuUsageLimit%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Usage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.1f".format(miningStats.cpuUsage)}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (miningStats.cpuUsage > cpuUsageLimit) Color(0xFFEF5350) else Color(0xFF4CAF50)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Slider(
                    value = cpuUsageLimit.toFloat(),
                    onValueChange = { viewModel.updateCpuUsageLimit(it.roundToInt()) },
                    valueRange = 10f..100f,
                    steps = 17,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "⚡ Throttling will activate when usage exceeds this limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Thermal Management
        ThermalManagementCard(
            maxTemperature = maxTemperature,
            currentTemperature = miningStats.cpuTemp,
            thermalThrottleEnabled = thermalThrottleEnabled,
            onTemperatureChange = { viewModel.updateMaxTemperature(it) },
            onThrottleToggle = { viewModel.updateThermalThrottle(it) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Battery & Power Management
        PowerManagementCard(
            batteryOptimizationEnabled = batteryOptimizationEnabled,
            powerUsage = miningStats.powerUsage,
            onOptimizationToggle = { viewModel.updateBatteryOptimization(it) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PerformanceDashboard(
    cpuUsage: Float,
    temperature: Float,
    powerUsage: Float,
    hashrate: Double,
    tempColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Monitor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // CPU Usage Gauge
                CircularGauge(
                    value = cpuUsage,
                    maxValue = 100f,
                    label = "CPU",
                    unit = "%",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                // Temperature Gauge
                CircularGauge(
                    value = temperature,
                    maxValue = 100f,
                    label = "TEMP",
                    unit = "°C",
                    color = tempColor,
                    modifier = Modifier.weight(1f)
                )
                
                // Power Usage Gauge
                CircularGauge(
                    value = powerUsage,
                    maxValue = 5f,
                    label = "POWER",
                    unit = "W",
                    color = Color(0xFFFFA726),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hashrate display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Hashrate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${"%.2f".format(hashrate)} H/s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CircularGauge(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOutCubic),
        label = "gaugeValue"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                val arcSize = Size(diameter, diameter)
                val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)
                
                // Background arc
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Value arc
                val sweepAngle = (animatedValue / maxValue) * 360f
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(animatedValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PerCoreActivityCard(
    selectedThreads: Int,
    maxThreads: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Core Activity Map",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid of cores
            val columns = 4
            val rows = (maxThreads + columns - 1) / columns
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until columns) {
                            val coreIndex = row * columns + col
                            if (coreIndex < maxThreads) {
                                CoreIndicator(
                                    coreNumber = coreIndex,
                                    isActive = isActive && coreIndex < selectedThreads,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoreIndicator(
    coreNumber: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coreActivity")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreAlpha"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        label = "coreBackground"
    )
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.copy(alpha = if (isActive) alpha else 1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$coreNumber",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PresetButton(
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PerformanceImpactIndicator(
    threadPercentage: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = when {
                    threadPercentage <= 25 -> "ECO MODE - Minimal power, low hashrate"
                    threadPercentage <= 50 -> "BALANCED - Good efficiency & moderate heat"
                    threadPercentage <= 75 -> "PERFORMANCE - High hashrate, increased heat"
                    else -> "MAXIMUM - Best hashrate, highest power consumption"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThermalManagementCard(
    maxTemperature: Float,
    currentTemperature: Float,
    thermalThrottleEnabled: Boolean,
    onTemperatureChange: (Float) -> Unit,
    onThrottleToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thermal Protection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Switch(
                    checked = thermalThrottleEnabled,
                    onCheckedChange = onThrottleToggle
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Temperature bar
            TemperatureBar(
                currentTemp = currentTemperature,
                maxTemp = maxTemperature
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Max Temperature Threshold: ${"%.0f".format(maxTemperature)}°C",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = maxTemperature,
                onValueChange = onTemperatureChange,
                valueRange = 60f..90f,
                steps = 29,
                modifier = Modifier.fillMaxWidth(),
                enabled = thermalThrottleEnabled
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (thermalThrottleEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mining will pause automatically if temperature exceeds threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureBar(
    currentTemp: Float,
    maxTemp: Float
) {
    val percentage = (currentTemp / maxTemp).coerceIn(0f, 1.2f)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Current: ${"%.1f".format(currentTemp)}°C",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    currentTemp < maxTemp * 0.8 -> Color(0xFF4CAF50)
                    currentTemp < maxTemp -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                }
            )
            Text(
                text = "Limit: ${"%.0f".format(maxTemp)}°C",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            percentage < 0.8f -> Color(0xFF4CAF50)
                            percentage < 1f -> Color(0xFFFFA726)
                            else -> Color(0xFFEF5350)
                        }
                    )
            )
            
            // Threshold marker
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun PowerManagementCard(
    batteryOptimizationEnabled: Boolean,
    powerUsage: Float,
    onOptimizationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Power Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current draw: ${"%.2f".format(powerUsage)}W",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = batteryOptimizationEnabled,
                    onCheckedChange = onOptimizationToggle
                )
            }
            
            if (batteryOptimizationEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "✓ Active Optimizations",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Auto-pause when battery < 20%\n" +
                                   "• Resume when battery > 30%\n" +
                                   "• Reduced performance when unplugged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
