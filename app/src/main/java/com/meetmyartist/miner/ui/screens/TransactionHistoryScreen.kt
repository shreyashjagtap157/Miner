package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.data.model.Transaction
import com.meetmyartist.miner.data.model.TransactionStatus
import com.meetmyartist.miner.data.model.TransactionType
import com.meetmyartist.miner.ui.viewmodel.PortfolioViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    walletId: Long? = null
) {
    val transactions by viewModel.getTransactionHistory(walletId).collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transaction History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (transactions.isEmpty()) {
            EmptyTransactionState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionCard(transaction)
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Transaction Type Icon
                    TransactionIcon(transaction.type)
                    
                    Column {
                        Text(
                            text = when (transaction.type) {
                                TransactionType.SEND -> "Sent ${transaction.cryptocurrency}"
                                TransactionType.RECEIVE -> "Received ${transaction.cryptocurrency}"
                                TransactionType.MINING_REWARD -> "Mining Reward"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${if (transaction.type == TransactionType.SEND) "-" else "+"}" +
                                "${"%.8f".format(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == TransactionType.SEND)
                            MaterialTheme.colorScheme.error
                        else
                            Color(0xFF4CAF50)
                    )
                    TransactionStatusChip(transaction.status)
                }
            }
            
            // Expanded Details
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                DetailRow("Transaction ID", "#${transaction.id}")
                
                if (transaction.txHash != null) {
                    DetailRow("Transaction Hash", transaction.txHash.take(16) + "...")
                }
                
                DetailRow("From", transaction.fromAddress.take(16) + "...")
                DetailRow("To", transaction.toAddress.take(16) + "...")
                
                if (transaction.fee > 0) {
                    DetailRow("Network Fee", "${"%.8f".format(transaction.fee)} ${transaction.cryptocurrency}")
                }
                
                if (transaction.notes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${transaction.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionIcon(type: TransactionType) {
    val (icon, color) = when (type) {
        TransactionType.SEND -> Icons.Default.ArrowUpward to Color(0xFFF44336)
        TransactionType.RECEIVE -> Icons.Default.ArrowDownward to Color(0xFF4CAF50)
        TransactionType.MINING_REWARD -> Icons.Default.Diamond to Color(0xFF2196F3)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
            tint = color
        )
    }
}

@Composable
private fun TransactionStatusChip(status: TransactionStatus) {
    val (text, color) = when (status) {
        TransactionStatus.PENDING -> "Pending" to Color(0xFFFF9800)
        TransactionStatus.CONFIRMED -> "Confirmed" to Color(0xFF4CAF50)
        TransactionStatus.FAILED -> "Failed" to Color(0xFFF44336)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyTransactionState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Transactions Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your transaction history will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
