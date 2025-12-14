package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meetmyartist.miner.data.model.Cryptocurrency

@Composable
fun CryptocurrencySelectorDialog(
    cryptos: List<Cryptocurrency>,
    selectedCrypto: Cryptocurrency?,
    onCryptoSelected: (Cryptocurrency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Cryptocurrency") },
        text = {
            LazyColumn {
                items(cryptos) { crypto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCryptoSelected(crypto) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = crypto == selectedCrypto,
                            onClick = { onCryptoSelected(crypto) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = crypto.name)
                            Text(text = crypto.symbol)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
