package com.meetmyartist.miner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meetmyartist.miner.data.model.*
import com.meetmyartist.miner.ui.viewmodel.PortfolioViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onTransferClick: () -> Unit = {}
) {
    val portfolioSummary by viewModel.portfolioSummary.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = viewModel::refreshPrices
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Portfolio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Total Value Card
            item {
                TotalValueCard(
                    totalValue = portfolioSummary.totalValueUsd,
                    change24h = portfolioSummary.totalChange24h,
                    onTransferClick = onTransferClick
                )
            }

            // Holdings Section
            item {
                Text(
                    text = "Your Holdings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Balance Cards
            items(portfolioSummary.balances) { balanceWithPrice ->
                CryptoBalanceCard(balanceWithPrice)
            }

            // Empty State
            if (portfolioSummary.balances.isEmpty()) {
                item {
                    EmptyPortfolioState()
                }
            }
        }
    }
}

@Composable
private fun TotalValueCard(
    totalValue: Double,
    change24h: Double,
    onTransferClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Total Portfolio Value",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${"%.2f".format(totalValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                ChangeChip(change = change24h)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Button
            Button(
                onClick = onTransferClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transfer Crypto")
            }
        }
    }
}

@Composable
private fun ChangeChip(change: Double) {
    val isPositive = change >= 0
    val color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${"%.2f".format(change)}%",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CryptoBalanceCard(balanceWithPrice: CryptoBalanceWithPrice) {
    val balance = balanceWithPrice.balance
    val price = balanceWithPrice.price
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Crypto Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(getCryptoColor(balance.cryptocurrency).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = balance.cryptocurrency.take(3),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getCryptoColor(balance.cryptocurrency)
                    )
                }
                
                Column {
                    Text(
                        text = balance.cryptocurrency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = price?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${"%.8f".format(balance.balance)} ${balance.cryptocurrency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$${"%.2f".format(balanceWithPrice.valueUsd)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (price != null) {
                    Text(
                        text = "@ $${"%.2f".format(price.priceUsd)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPortfolioState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Assets Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start mining to earn cryptocurrency",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getCryptoColor(symbol: String): Color {
    return when (symbol.uppercase()) {
        "BTC" -> Color(0xFFF7931A)
        "ETH" -> Color(0xFF627EEA)
        "LTC" -> Color(0xFF345D9D)
        "XMR" -> Color(0xFFFF6600)
        "ZEC" -> Color(0xFFF4B728)
        "RVN" -> Color(0xFF384182)
        "ETC" -> Color(0xFF328332)
        "DOGE" -> Color(0xFFC2A633)
        "BCH" -> Color(0xFF8DC351)
        else -> Color(0xFF9E9E9E)
    }
}
