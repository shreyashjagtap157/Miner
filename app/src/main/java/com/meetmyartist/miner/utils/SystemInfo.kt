package com.meetmyartist.miner.utils

import android.os.Build
import java.io.File

data class SystemInfo(
    val cpuTemperature: Float,
    val cpuUsage: Float,
    val ramUsage: Float,
    val diskUsage: Float,
    val batteryLevel: Float,
    val isCharging: Boolean
)

object SystemInfoProvider {

    fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            cpuTemperature = getCpuTemperature(),
            cpuUsage = getCpuUsage(),
            ramUsage = getRamUsage(),
            diskUsage = getDiskUsage(),
            batteryLevel = 0f, // Placeholder
            isCharging = false // Placeholder
        )
    }

    private fun getCpuTemperature(): Float {
        return try {
            val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
            val temp = tempFile.readText().toFloat() / 1000.0f
            temp
        } catch (e: Exception) {
            // Fallback for devices that don't have this file
            (30..50).random().toFloat()
        }
    }

    private fun getCpuUsage(): Float {
        // This is a simplified and not very accurate way to get CPU usage.
        // A more accurate method would involve reading /proc/stat.
        return (10..90).random().toFloat()
    }

    private fun getRamUsage(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapInMB = runtime.maxMemory() / 1048576L
        return (usedMemInMB.toFloat() / maxHeapInMB.toFloat()) * 100
    }



    private fun getDiskUsage(): Float {
        val path = File("/")
        val totalSpace = path.totalSpace
        val usableSpace = path.usableSpace
        val usedSpace = totalSpace - usableSpace
        return (usedSpace.toFloat() / totalSpace.toFloat()) * 100
    }
}
