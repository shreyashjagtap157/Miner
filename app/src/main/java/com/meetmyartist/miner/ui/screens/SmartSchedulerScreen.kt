import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meetmyartist.miner.mining.SmartMiningScheduler
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSchedulerScreen(scheduler: SmartMiningScheduler) {
    val schedulerState by scheduler.schedulerState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<SmartMiningScheduler.MiningSchedule?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Smart Mining Scheduler") },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor =
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                        onClick = { showAddDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Schedule") }
                )
            }
    ) { paddingValues ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scheduler Status Card
            item {
                SchedulerStatusCard(
                        enabled = schedulerState.enabled,
                        shouldMineNow = schedulerState.shouldMineNow,
                        onToggle = { enabled ->
                            coroutineScope.launch {
                                if (enabled) scheduler.enableScheduler()
                                else scheduler.disableScheduler()
                            }
                        }
                )
            }

            // Current Conditions Card
            item { CurrentConditionsCard(conditions = schedulerState.currentConditions) }

            // Quick Presets
            item {
                Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                PresetSchedulesGrid(
                        onPresetSelected = { preset ->
                            coroutineScope.launch { scheduler.addSchedule(preset) }
                        }
                )
            }

            // Active Schedules
            item {
                Text(
                        text =
                                "Active Schedules (${schedulerState.activeSchedules.filter { it.enabled }.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (schedulerState.activeSchedules.isEmpty()) {
                item { EmptySchedulesCard(onAddPreset = { showAddDialog = true }) }
            } else {
                items(schedulerState.activeSchedules.sortedByDescending { it.priority }) { schedule
                    ->
                    ScheduleCard(
                            schedule = schedule,
                            onClick = { selectedSchedule = schedule },
                            onToggle = { enabled ->
                                coroutineScope.launch {
                                    scheduler.removeSchedule(schedule.id)
                                    scheduler.addSchedule(schedule.copy(enabled = enabled))
                                }
                            },
                            onDelete = {
                                coroutineScope.launch { scheduler.removeSchedule(schedule.id) }
                            }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { schedule ->
                    coroutineScope.launch { scheduler.addSchedule(schedule) }
                    showAddDialog = false
                }
        )
    }

    selectedSchedule?.let { schedule ->
        ScheduleDetailsDialog(schedule = schedule, onDismiss = { selectedSchedule = null })
    }
}

@Composable
fun SchedulerStatusCard(enabled: Boolean, shouldMineNow: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (enabled) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            if (enabled) Icons.Default.Schedule else Icons.Default.ScheduleSend,
                            contentDescription = null,
                            tint =
                                    if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "Smart Scheduler",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color =
                                    if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                        text =
                                if (enabled) {
                                    if (shouldMineNow) "✓ Conditions met - Mining active"
                                    else "⏳ Waiting for conditions"
                                } else {
                                    "Tap to enable automatic scheduling"
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                                if (enabled)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.8f
                                        )
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun CurrentConditionsCard(conditions: SmartMiningScheduler.MiningConditions) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = "Current Device Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConditionIndicator(
                        icon = Icons.Default.BatteryFull,
                        label = "Battery",
                        value = "${conditions.minBatteryLevel}%",
                        color = Color(0xFF4CAF50)
                )
                ConditionIndicator(
                        icon = Icons.Default.Thermostat,
                        label = "Temp",
                        value = "${"%.0f".format(conditions.maxTemperature)}°C",
                        color = Color(0xFFFFA726)
                )
                ConditionIndicator(
                        icon =
                                if (conditions.requireCharging) Icons.Rounded.Bolt
                                else Icons.Default.BatteryChargingFull,
                        label = "Charging",
                        value = if (conditions.requireCharging) "Yes" else "No",
                        color = if (conditions.requireCharging) Color(0xFF2196F3) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ConditionIndicator(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        value: String,
        color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
        )
    }
}

@Composable
fun PresetSchedulesGrid(onPresetSelected: (SmartMiningScheduler.MiningSchedule) -> Unit) {
    val presets = remember {
        listOf(
                PresetInfo("🌙 Night", "11PM-7AM\nWhile charging", "night_mining"),
                PresetInfo("🔋 Charging", "Anytime\nWhen plugged in", "charging_only"),
                PresetInfo("📶 WiFi Only", "All day\nWiFi required", "wifi_only"),
                PresetInfo("⚡ Off-Peak", "9PM-9AM\nWeekdays", "off_peak")
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            PresetCard(
                    preset = preset,
                    onClick = {
                        // Generate actual schedule based on preset ID
                        val schedule =
                                SmartMiningScheduler.MiningSchedule(
                                        id = preset.id,
                                        name = preset.title,
                                        enabled = true,
                                        startTime = java.time.LocalTime.of(23, 0),
                                        endTime = java.time.LocalTime.of(7, 0),
                                        daysOfWeek = DayOfWeek.entries.toSet(),
                                        conditions = SmartMiningScheduler.MiningConditions()
                                )
                        onPresetSelected(schedule)
                    },
                    modifier = Modifier.weight(1f)
            )
        }
    }
}

data class PresetInfo(val title: String, val description: String, val id: String)

@Composable
fun PresetCard(preset: PresetInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.aspectRatio(0.8f).clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScheduleCard(
        schedule: SmartMiningScheduler.MiningSchedule,
        onClick: () -> Unit,
        onToggle: (Boolean) -> Unit,
        onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
            modifier =
                    Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = schedule.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = "${schedule.startTime} - ${schedule.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                            checked = schedule.enabled,
                            onCheckedChange = onToggle,
                            modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                                if (expanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                                contentDescription = null
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Days of week
                Text(
                        text = "Active Days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        DayChip(
                                day = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                isActive = day in schedule.daysOfWeek
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Conditions
                Text(
                        text = "Conditions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                ConditionChips(schedule.conditions)

                Spacer(modifier = Modifier.height(12.dp))

                // Delete button
                TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Schedule")
                }
            }
        }
    }
}

@Composable
fun DayChip(day: String, isActive: Boolean) {
    Box(
            modifier =
                    Modifier.size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                    if (isActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                            ),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color =
                        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConditionChips(conditions: SmartMiningScheduler.MiningConditions) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (conditions.requireCharging) {
            ConditionChip("⚡ Requires charging")
        }
        if (conditions.requireWifi) {
            ConditionChip("📶 WiFi required")
        }
        if (conditions.onlyWhenScreenOff) {
            ConditionChip("📱 Screen off only")
        }
        if (conditions.minBatteryLevel > 0) {
            ConditionChip("🔋 Battery > ${conditions.minBatteryLevel}%")
        }
        if (conditions.maxTemperature < 90f) {
            ConditionChip("🌡️ Temp < ${"%.0f".format(conditions.maxTemperature)}°C")
        }
    }
}

@Composable
fun ConditionChip(text: String) {
    Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun EmptySchedulesCard(onAddPreset: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "No Schedules Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    text = "Add a schedule or choose a preset to get started",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddScheduleDialog(
        onDismiss: () -> Unit,
        onConfirm: (SmartMiningScheduler.MiningSchedule) -> Unit
) {
    // Simplified dialog - full implementation would have time pickers, day selectors, etc.
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Custom Schedule") },
            text = { Text("Use preset schedules or implement custom schedule builder here") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun ScheduleDetailsDialog(schedule: SmartMiningScheduler.MiningSchedule, onDismiss: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(schedule.name) },
            text = {
                Column {
                    Text("Time: ${schedule.startTime} - ${schedule.endTime}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "Days: ${schedule.daysOfWeek.joinToString { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }}"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Priority: ${schedule.priority}")
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

data class Schedule(
        val dayOfWeek: String,
        val startTime: String,
        val endTime: String,
        val hashratePercentage: Float,
        val name: String = "Schedule",
        val priority: Int = 0,
        val enabled: Boolean = true
)

@Composable
fun ScheduleItem(schedule: Schedule, onUpdate: (Schedule) -> Unit, onDelete: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (isEditing) {
        EditScheduleDialog(
                schedule = schedule,
                onDismiss = { isEditing = false },
                onSchedule = { day, start, end, hash ->
                    coroutineScope.launch {
                        onDelete()
                        onUpdate(
                                Schedule(
                                        dayOfWeek = day,
                                        startTime = start,
                                        endTime = end,
                                        hashratePercentage = hash
                                )
                        )
                    }
                    isEditing = false
                },
                onDelete = {
                    coroutineScope.launch { onDelete() }
                    isEditing = false
                }
        )
    } else {
        Card(
                modifier = Modifier.fillMaxWidth().clickable { isEditing = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "${schedule.startTime} - ${schedule.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "Priority: ${schedule.priority}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun EditScheduleDialog(
        schedule: Schedule? = null,
        onDismiss: () -> Unit,
        onSchedule: (String, String, String, Float) -> Unit,
        onDelete: (() -> Unit)? = null
) {
    var dayOfWeek by remember { mutableStateOf(schedule?.dayOfWeek ?: "Monday") }
    var startTime by remember { mutableStateOf(schedule?.startTime ?: "00:00") }
    var endTime by remember { mutableStateOf(schedule?.endTime ?: "01:00") }
    var hashratePercentage by remember {
        mutableStateOf(schedule?.hashratePercentage?.toString() ?: "100")
    }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (schedule == null) "Add Schedule" else "Edit Schedule") },
            text = {
                Column {
                    // Day of week selector (simplified)
                    Text("Day of Week:")
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(
                                    text = day,
                                    modifier =
                                            Modifier.weight(1f)
                                                    .clickable { dayOfWeek = day }
                                                    .padding(8.dp)
                                                    .background(
                                                            if (day == dayOfWeek)
                                                                    MaterialTheme.colorScheme
                                                                            .primaryContainer
                                                            else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                    ),
                                    textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time pickers (simplified as text fields)
                    Text("Start Time:")
                    TextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            placeholder = { Text("HH:MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                    )

                    Text("End Time:")
                    TextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            placeholder = { Text("HH:MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hashrate percentage
                    Text("Hashrate Percentage:")
                    TextField(
                            value = hashratePercentage,
                            onValueChange = { hashratePercentage = it },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (schedule != null) {
                        Button(
                                onClick = {
                                    onDelete?.invoke()
                                    onDismiss()
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        )
                        ) { Text("Delete") }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                            onClick = {
                                onSchedule(
                                        dayOfWeek,
                                        startTime,
                                        endTime,
                                        hashratePercentage.toFloatOrNull() ?: 100f
                                )
                            }
                    ) { Text(if (schedule == null) "Add" else "Update") }
                }
            }
    )
}
