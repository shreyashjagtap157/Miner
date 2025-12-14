package com.meetmyartist.miner.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miner_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        val SELECTED_THREADS = intPreferencesKey("selected_threads")
        val CPU_USAGE_LIMIT = intPreferencesKey("cpu_usage_limit")
        val MAX_TEMPERATURE = floatPreferencesKey("max_temperature")
        val THERMAL_THROTTLE_ENABLED = booleanPreferencesKey("thermal_throttle_enabled")
        val BATTERY_OPTIMIZATION_ENABLED = booleanPreferencesKey("battery_optimization_enabled")
        val PAUSE_ON_LOW_BATTERY = booleanPreferencesKey("pause_on_low_battery")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
        val IS_MINING_ACTIVE = booleanPreferencesKey("is_mining_active")
        val GOOGLE_ACCOUNT_ID = stringPreferencesKey("google_account_id")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_START_MINING = booleanPreferencesKey("auto_start_mining")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val ENERGY_MODE = stringPreferencesKey("energy_mode") // "ECO", "BALANCED", "PERFORMANCE"
        val SCHEDULER_ENABLED = booleanPreferencesKey("scheduler_enabled")
    }
    
    val selectedThreads: Flow<Int> = dataStore.data.map { prefs ->
        prefs[SELECTED_THREADS] ?: (Runtime.getRuntime().availableProcessors() / 2)
    }
    
    val cpuUsageLimit: Flow<Int> = dataStore.data.map { prefs ->
        prefs[CPU_USAGE_LIMIT] ?: 70
    }
    
    val maxTemperature: Flow<Float> = dataStore.data.map { prefs ->
        prefs[MAX_TEMPERATURE] ?: 80f
    }
    
    val thermalThrottleEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[THERMAL_THROTTLE_ENABLED] ?: true
    }
    
    val batteryOptimizationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BATTERY_OPTIMIZATION_ENABLED] ?: true
    }
    
    val pauseOnLowBattery: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PAUSE_ON_LOW_BATTERY] ?: true
    }
    
    val lowBatteryThreshold: Flow<Int> = dataStore.data.map { prefs ->
        prefs[LOW_BATTERY_THRESHOLD] ?: 20
    }
    
    val isMiningActive: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_MINING_ACTIVE] ?: false
    }
    
    val googleAccountId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[GOOGLE_ACCOUNT_ID]
    }
    
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: false
    }
    
    val autoStartMining: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_START_MINING] ?: false
    }
    
    val notificationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFICATION_ENABLED] ?: true
    }
    
    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }
    
    val energyMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[ENERGY_MODE] ?: "BALANCED"
    }

    val schedulerEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SCHEDULER_ENABLED] ?: false
    }
    
    suspend fun updateSelectedThreads(threads: Int) {
        dataStore.edit { prefs -> prefs[SELECTED_THREADS] = threads }
    }
    
    suspend fun updateCpuUsageLimit(limit: Int) {
        dataStore.edit { prefs -> prefs[CPU_USAGE_LIMIT] = limit }
    }
    
    suspend fun updateMaxTemperature(temp: Float) {
        dataStore.edit { prefs -> prefs[MAX_TEMPERATURE] = temp }
    }
    
    suspend fun updateThermalThrottle(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[THERMAL_THROTTLE_ENABLED] = enabled }
    }
    
    suspend fun updateBatteryOptimization(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BATTERY_OPTIMIZATION_ENABLED] = enabled }
    }
    
    suspend fun updatePauseOnLowBattery(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PAUSE_ON_LOW_BATTERY] = enabled }
    }
    
    suspend fun updateLowBatteryThreshold(threshold: Int) {
        dataStore.edit { prefs -> prefs[LOW_BATTERY_THRESHOLD] = threshold }
    }
    
    suspend fun updateMiningActive(active: Boolean) {
        dataStore.edit { prefs -> prefs[IS_MINING_ACTIVE] = active }
    }
    
    suspend fun updateGoogleAccountId(accountId: String?) {
        dataStore.edit { prefs ->
            if (accountId != null) {
                prefs[GOOGLE_ACCOUNT_ID] = accountId
            } else {
                prefs.remove(GOOGLE_ACCOUNT_ID)
            }
        }
    }
    
    suspend fun updateBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED] = enabled }
    }
    
    suspend fun updateAutoStartMining(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_START_MINING] = enabled }
    }
    
    suspend fun updateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NOTIFICATION_ENABLED] = enabled }
    }
    
    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }
    
    suspend fun updateEnergyMode(mode: String) {
        dataStore.edit { prefs -> prefs[ENERGY_MODE] = mode }
    }

    suspend fun updateSchedulerEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SCHEDULER_ENABLED] = enabled }
    }
    
    suspend fun applyEnergyModePreset(mode: com.meetmyartist.miner.data.model.EnergyMode) {
        val maxThreads = Runtime.getRuntime().availableProcessors()
        dataStore.edit { prefs ->
            prefs[ENERGY_MODE] = mode.name
            prefs[SELECTED_THREADS] = mode.threads(maxThreads)
            prefs[CPU_USAGE_LIMIT] = mode.cpuLimit
            prefs[MAX_TEMPERATURE] = mode.maxTemp
        }
    }
}
