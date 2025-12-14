package com.meetmyartist.miner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.data.model.TransferRequest
import com.meetmyartist.miner.ui.viewmodel.PortfolioViewModel
import com.meetmyartist.miner.wallet.TransferManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
        walletId: Long,
        cryptocurrency: String,
        availableBalance: Double,
        viewModel: PortfolioViewModel = hiltViewModel(),
        onTransferComplete: () -> Unit = {}
) {
    val transferState by viewModel.transferState.collectAsState()
    val scope = rememberCoroutineScope()

    var recipientAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TransferManager.FeePriority.MEDIUM) }
    var estimatedFee by remember { mutableStateOf(0.0) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Calculate estimated fee when amount or priority changes
    LaunchedEffect(amount, selectedPriority) {
        if (amount.toDoubleOrNull() != null) {
            estimatedFee = viewModel.estimateFee(cryptocurrency, amount.toDouble())
        }
    }

    // Handle transfer state
    LaunchedEffect(transferState) {
        when (transferState) {
            is PortfolioViewModel.TransferState.Success -> {
                // Show success and navigate back
                kotlinx.coroutines.delay(2000)
                viewModel.resetTransferState()
                onTransferComplete()
            }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                        text = "Send $cryptocurrency",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "Available: ${"%.8f".format(availableBalance)} $cryptocurrency",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recipient Address
        OutlinedTextField(
                value = recipientAddress,
                onValueChange = { recipientAddress = it },
                label = { Text("Recipient Address") },
                placeholder = { Text("Enter $cryptocurrency address") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                trailingIcon = {
                    if (recipientAddress.isNotEmpty()) {
                        val isValid = viewModel.validateAddress(cryptocurrency, recipientAddress)
                        Icon(
                                imageVector =
                                        if (isValid) Icons.Default.CheckCircle
                                        else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                },
                isError =
                        recipientAddress.isNotEmpty() &&
                                !viewModel.validateAddress(cryptocurrency, recipientAddress),
                supportingText = {
                    if (recipientAddress.isNotEmpty() &&
                                    !viewModel.validateAddress(cryptocurrency, recipientAddress)
                    ) {
                        Text("Invalid $cryptocurrency address format")
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount
        OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                placeholder = { Text("0.00000000") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                trailingIcon = {
                    TextButton(
                            onClick = {
                                amount =
                                        (availableBalance - estimatedFee)
                                                .coerceAtLeast(0.0)
                                                .toString()
                            }
                    ) { Text("MAX") }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > availableBalance) {
                        Text("Insufficient balance", color = MaterialTheme.colorScheme.error)
                    }
                },
                isError = (amount.toDoubleOrNull() ?: 0.0) > availableBalance
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Fee Priority
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Transaction Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                TransferManager.FeePriority.entries.forEach { priority ->
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text =
                                            when (priority) {
                                                TransferManager.FeePriority.LOW -> "Low (30-60 min)"
                                                TransferManager.FeePriority.MEDIUM ->
                                                        "Medium (10-30 min)"
                                                TransferManager.FeePriority.HIGH ->
                                                        "High (5-10 min)"
                                            },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                                text =
                                        when (priority) {
                                            TransferManager.FeePriority.LOW ->
                                                    "${"%.8f".format(estimatedFee * 0.5)}"
                                            TransferManager.FeePriority.MEDIUM ->
                                                    "${"%.8f".format(estimatedFee)}"
                                            TransferManager.FeePriority.HIGH ->
                                                    "${"%.8f".format(estimatedFee * 2.0)}"
                                        } + " $cryptocurrency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes (Optional)
        OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add a note...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Card
        Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Transaction Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                SummaryRow("Amount", "$amount $cryptocurrency")
                SummaryRow("Network Fee", "${"%.8f".format(estimatedFee)} $cryptocurrency")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryRow(
                        "Total",
                        "${"%.8f".format((amount.toDoubleOrNull() ?: 0.0) + estimatedFee)} $cryptocurrency",
                        isTotal = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send Button
        Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                        recipientAddress.isNotEmpty() &&
                                viewModel.validateAddress(cryptocurrency, recipientAddress) &&
                                (amount.toDoubleOrNull() ?: 0.0) > 0 &&
                                (amount.toDoubleOrNull() ?: 0.0) <= availableBalance &&
                                transferState !is PortfolioViewModel.TransferState.Processing
        ) {
            when (transferState) {
                is PortfolioViewModel.TransferState.Processing -> {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                }
                else -> {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send $cryptocurrency")
                }
            }
        }

        // Transfer State Messages
        AnimatedVisibility(
                visible = transferState is PortfolioViewModel.TransferState.Success,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            )
            ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                                "Transfer Successful!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                        )
                        (transferState as? PortfolioViewModel.TransferState.Success)?.txHash?.let {
                                hash ->
                            Text(
                                    "TX: ${hash.take(16)}...",
                                    style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
                visible = transferState is PortfolioViewModel.TransferState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                            )
            ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                            (transferState as? PortfolioViewModel.TransferState.Error)?.message
                                    ?: "Error",
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Confirm Transfer") },
                text = {
                    Column {
                        Text("You are about to send:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "$amount $cryptocurrency",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("To:")
                        Text(recipientAddress, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "This action cannot be undone.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                            onClick = {
                                showConfirmDialog = false
                                val request =
                                        TransferRequest(
                                                fromWalletId = walletId,
                                                cryptocurrency = cryptocurrency,
                                                toAddress = recipientAddress,
                                                amount = amount.toDouble(),
                                                fee = estimatedFee,
                                                notes = notes.ifEmpty { null }
                                        )
                                viewModel.initiateTransfer(request)
                            }
                    ) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
                text = label,
                style =
                        if (isTotal) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
                text = value,
                style =
                        if (isTotal) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}
