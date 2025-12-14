package com.meetmyartist.miner.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class BatteryHealth(
        val level: Int,
        val temperature: Float,
        val voltage: Int,
        val health: Int,
        val isCharging: Boolean,
        val chargingSource: String,
        val capacity: Int,
        val cycleCount: Int?
    )
    
    fun monitorBatteryHealth(): Flow<BatteryHealth> = flow {
        while (true) {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            
            batteryStatus?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level * 100) / scale
                
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val chargingSource = when (chargePlug) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Not Charging"
                }
                
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                
                val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                } else {
                    null
                }
                
                emit(
                    BatteryHealth(
                        level = batteryPct,
                        temperature = temp,
                        voltage = voltage,
                        health = health,
                        isCharging = isCharging,
                        chargingSource = chargingSource,
                        capacity = capacity,
                        cycleCount = cycleCount
                    )
                )
            }
            
            delay(5000) // Update every 5 seconds
        }
    }
    
    fun getBatteryHealthDescription(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
    
    fun shouldPauseMiningForBattery(health: BatteryHealth, lowThreshold: Int): Boolean {
        return when {
            health.level < lowThreshold && !health.isCharging -> true
            health.temperature > 45f -> true // Battery temp above 45°C
            health.health == BatteryManager.BATTERY_HEALTH_OVERHEAT -> true
            health.health == BatteryManager.BATTERY_HEALTH_DEAD -> true
            else -> false
        }
    }
}
