package com.meetmyartist.miner.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.meetmyartist.miner.R
import com.meetmyartist.miner.auth.BiometricAuthManager
import com.meetmyartist.miner.service.MiningService
import com.meetmyartist.miner.ui.navigation.Screen
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        navController: NavController? = null,
        viewModel: MiningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val biometricAuthManager = remember { BiometricAuthManager() }
    val canUseBiometric =
            remember(context) {
                BiometricManager.from(context)
                        .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                        BiometricManager.BIOMETRIC_SUCCESS
            }
    val scrollState = rememberScrollState()

    var showAboutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mining Service Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Mining Service",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                        onClick = { MiningService.startMiningService(context) },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Foreground Service")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                        onClick = { MiningService.stopMiningService(context) },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Foreground Service")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Monitor Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "System Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text =
                                "Real-time monitoring of battery health, CPU performance, and system recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                        onClick = { navController?.navigate(Screen.SystemMonitor.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                )
                ) {
                    Icon(Icons.Default.Monitor, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open System Monitor")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unified Mining Dashboard Section
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Unified Mining Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text =
                                "All-in-one view: hashrate, mined crypto, value, and resource controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                        onClick = { navController?.navigate(Screen.UnifiedMining.route) },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Unified Dashboard")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Remote Mining Control Section
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Remote Mining Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "Control mining operations across multiple devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                        onClick = { navController?.navigate(Screen.RemoteControl.route) },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Remote Control")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Energy Mode Presets Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Energy Mode Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = "Quick presets to optimize mining performance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                com.meetmyartist.miner.data.model.EnergyMode.entries.forEach { mode ->
                    OutlinedButton(
                            onClick = { viewModel.applyEnergyMode(mode) },
                            modifier = Modifier.fillMaxWidth()
                    ) { Text(mode.displayName) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val themeMode by viewModel.themeMode.collectAsState()

                Text(text = "Theme", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light" to "light", "Dark" to "dark", "System" to "system").forEach {
                            (label, value) ->
                        FilterChip(
                                selected = themeMode == value,
                                onClick = { viewModel.updateThemeMode(value) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Require biometric to start mining",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                text = "Adds an extra confirmation step before mining begins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (!canUseBiometric) {
                                    Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            R.string.biometric_not_available
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    return@Switch
                                }
                                if (enabled) {
                                    val activity = context as? FragmentActivity
                                    if (activity == null) {
                                        Toast.makeText(
                                                        context,
                                                        context.getString(
                                                                R.string.biometric_not_available
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    } else {
                                        biometricAuthManager.showBiometricPrompt(
                                                activity = activity,
                                                onSuccess = {
                                                    viewModel.updateBiometricEnabled(true)
                                                },
                                                onFailure = {
                                                    Toast.makeText(
                                                                    context,
                                                                    "Authentication failed",
                                                                    Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                    viewModel.updateBiometricEnabled(false)
                                                }
                                        )
                                    }
                                } else {
                                    viewModel.updateBiometricEnabled(false)
                                }
                            }
                    )
                }
                if (!canUseBiometric) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "Biometric authentication is not available on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Info Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "App Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow("Version", "1.0.0")
                InfoRow("Build", "Release")
                InfoRow("Min SDK", "29 (Android 10)")
                InfoRow("Target SDK", "35")

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.fillMaxWidth()
                ) { Text("About") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Tips Card
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                            text = "Important Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text =
                                "• Mobile mining is educational and experimental\n" +
                                        "• Not recommended for profit due to low hashrates\n" +
                                        "• Always monitor device temperature\n" +
                                        "• Use thermal protection features\n" +
                                        "• Mining may reduce device lifespan\n" +
                                        "• Check manufacturer warranty policies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text("About Crypto Miner") },
                text = {
                    Column {
                        Text(
                                text = "Crypto Miner v1.0.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text =
                                        "A comprehensive Android cryptocurrency mining application with " +
                                                "multi-algorithm support, resource management, and wallet integration.",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = "Features:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text =
                                        "• Multi-algorithm mining\n" +
                                                "• CPU resource control\n" +
                                                "• Wallet management\n" +
                                                "• Google Sign-In\n" +
                                                "• Secure credential storage\n" +
                                                "• Real-time monitoring",
                                style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = "Open Source • MIT License",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
                }
        )
    }
}
