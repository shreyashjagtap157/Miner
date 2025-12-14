package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.meetmyartist.miner.R

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Column {
        Text(stringResource(R.string.welcome))
        Button(onClick = onComplete) { Text(stringResource(R.string.start_mining)) }
    }
}

@Composable
fun ErrorSnackbar(error: String?, onDismiss: () -> Unit) {
    if (error != null) {
        Snackbar(action = { Button(onClick = onDismiss) { Text("Dismiss") } }) {
            Text(error)
        }
    }
}
