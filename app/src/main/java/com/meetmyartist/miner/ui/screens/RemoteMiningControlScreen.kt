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
import com.meetmyartist.miner.ui.viewmodel.RemoteMiningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteMiningControlScreen(
    viewModel: RemoteMiningViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val isClientConnected by viewModel.isClientConnected.collectAsState()
    val connectedClients by viewModel.connectedClients.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val remoteDeviceInfo by viewModel.remoteDeviceInfo.collectAsState()
    val remoteStats by viewModel.remoteStats.collectAsState()
    
    var hostInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("8888") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Remote Mining Control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Control mining operations across multiple devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Server Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServerRunning) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
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
                            text = "Server Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isServerRunning) "Server running on port $serverPort" else "Allow other devices to control this device",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Icon(
                        if (isServerRunning) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isServerRunning) {
                    Text(
                        text = "Connected Clients: ${connectedClients.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    connectedClients.forEach { client ->
                        Text(
                            text = "• $client",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { viewModel.stopServer() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Server")
                    }
                } else {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: 8888
                            viewModel.startServer(port)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Server")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Client Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isClientConnected) 
                    MaterialTheme.colorScheme.secondaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
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
                            text = "Client Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isClientConnected) "Connected to remote device" else "Control another device",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Icon(
                        if (isClientConnected) Icons.Default.Link else Icons.Default.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isClientConnected) {
                    // Remote Device Info
                    remoteDeviceInfo?.let { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = info.deviceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cores: ${info.availableCores} | Active: ${info.activeThreads}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Android ${info.androidVersion}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Remote Stats
                    remoteStats?.let { stats ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Remote Mining Stats",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StatRow("Hashrate", "%.2f H/s".format(stats.hashrate))
                                StatRow("CPU Temp", "%.1f°C".format(stats.cpuTemp))
                                StatRow("CPU Usage", "%.1f%%".format(stats.cpuUsage))
                                StatRow("Shares", "${stats.acceptedShares}/${stats.rejectedShares}")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Remote Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startRemoteMining() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { viewModel.pauseRemoteMining() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { viewModel.stopRemoteMining() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { viewModel.refreshRemoteStats() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh Stats")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.disconnectClient() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect")
                    }
                } else {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("Host IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: 8888
                            viewModel.connectToServer(hostInput, port)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hostInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Link, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
            }
        }
    }
}
