package com.meetmyartist.miner.utils

import java.text.NumberFormat
import java.util.Locale

/** Formats uptime in seconds to a human-readable string. */
fun formatUptime(uptimeSeconds: Long): String {
    val days = uptimeSeconds / (24 * 3600)
    val hours = (uptimeSeconds % (24 * 3600)) / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    val seconds = uptimeSeconds % 60

    return when {
        days > 0 -> String.format("%dd %02dh %02dm", days, hours, minutes)
        hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
        else -> String.format("%dm %02ds", minutes, seconds)
    }
}

/** Formats hashrate with appropriate units (H/s, KH/s, MH/s, GH/s). */
fun formatHashrate(hashrate: Double): String {
    return when {
        hashrate >= 1_000_000_000 -> String.format("%.2f GH/s", hashrate / 1_000_000_000)
        hashrate >= 1_000_000 -> String.format("%.2f MH/s", hashrate / 1_000_000)
        hashrate >= 1_000 -> String.format("%.2f KH/s", hashrate / 1_000)
        else -> String.format("%.2f H/s", hashrate)
    }
}

/** Formats bytes to human-readable size (KB, MB, GB). */
fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.2f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/** Formats currency value with proper decimal places and currency symbol. */
fun formatCurrency(value: Double, currencyCode: String = "USD"): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(value)
}

/** Formats a cryptocurrency amount with appropriate decimal precision. */
fun formatCryptoAmount(amount: Double, symbol: String): String {
    return when {
        amount >= 1.0 -> String.format("%.4f %s", amount, symbol)
        amount >= 0.0001 -> String.format("%.6f %s", amount, symbol)
        else -> String.format("%.8f %s", amount, symbol)
    }
}
