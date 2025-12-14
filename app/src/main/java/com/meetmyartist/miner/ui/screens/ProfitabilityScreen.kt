package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.ui.viewmodel.ProfitabilityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityScreen(
    viewModel: ProfitabilityViewModel = hiltViewModel()
) {
    val profitabilityResult by viewModel.profitabilityResult.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var hashrate by remember { mutableStateOf("100") }
    var crypto by remember { mutableStateOf("BTC") }
    var price by remember { mutableStateOf("60000") }
    var useLivePrice by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profitability Calculator") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = hashrate,
                onValueChange = { hashrate = it },
                label = { Text("Your Hashrate (MH/s)") }
            )
            OutlinedTextField(
                value = crypto,
                onValueChange = { crypto = it },
                label = { Text("Cryptocurrency (e.g., BTC)") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use Live Price", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = useLivePrice,
                    onCheckedChange = { useLivePrice = it }
                )
            }
            if (!useLivePrice) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Current Price (USD)") }
                )
            }
            Button(onClick = {
                viewModel.calculateProfitability(
                    hashrate.toDoubleOrNull() ?: 0.0,
                    crypto,
                    if (useLivePrice) null else price.toDoubleOrNull()
                )
            }) {
                Text("Calculate")
            }
            if (loading) {
                CircularProgressIndicator()
            }

            profitabilityResult?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Estimated Earnings", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        it["priceUsd"]?.let { livePrice ->
                            Text(
                                text = "Price Used: $${"%.2f".format(livePrice)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text("Per Day: $${"%.2f".format(it["usdPerDay"])}", fontWeight = FontWeight.Bold)
                        Text("Per Month: $${"%.2f".format(it["usdPerMonth"])}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
}
