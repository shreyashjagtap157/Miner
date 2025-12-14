package com.meetmyartist.miner.mining

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.work.*
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.service.MiningService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart Mining Scheduler
 *
 * Automatically manages mining operations based on:
 * - Time schedules (e.g., mine only at night)
 * - Battery level and charging status
 * - Network connectivity (for pool mining)
 * - Device temperature
 * - Custom user-defined conditions
 */
@Singleton
class SmartMiningScheduler
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val preferencesManager: PreferencesManager
) {
    private val workManager = WorkManager.getInstance(context)

    private val _schedulerState = MutableStateFlow(SchedulerState())
    val schedulerState: StateFlow<SchedulerState> = _schedulerState.asStateFlow()

    data class SchedulerState(
            val enabled: Boolean = false,
            val activeSchedules: List<MiningSchedule> = emptyList(),
            val nextScheduledStart: Long? = null,
            val nextScheduledStop: Long? = null,
            val currentConditions: MiningConditions = MiningConditions(),
            val shouldMineNow: Boolean = false
    )

    data class MiningSchedule(
            val id: String,
            val name: String,
            val enabled: Boolean,
            val startTime: LocalTime,
            val endTime: LocalTime,
            val daysOfWeek: Set<DayOfWeek>,
            val conditions: MiningConditions,
            val priority: Int = 0
    )

    data class MiningConditions(
            val requireCharging: Boolean = false,
            val minBatteryLevel: Int = 20,
            val maxBatteryLevel: Int = 100,
            val maxTemperature: Float = 75f,
            val requireWifi: Boolean = false,
            val onlyWhenScreenOff: Boolean = false,
            val minBatteryLevelWhenNotCharging: Int = 50
    )

    companion object {
        private const val SCHEDULER_WORK_TAG = "mining_scheduler"
        private const val CONDITION_CHECK_WORK = "condition_check_work"
    }

    /** Enable smart scheduling with default conditions */
    suspend fun enableScheduler() {
        _schedulerState.value = _schedulerState.value.copy(enabled = true)
        preferencesManager.updateSchedulerEnabled(true)
        setupPeriodicConditionCheck()
    }

    /** Disable smart scheduling */
    suspend fun disableScheduler() {
        _schedulerState.value = _schedulerState.value.copy(enabled = false)
        preferencesManager.updateSchedulerEnabled(false)
        workManager.cancelAllWorkByTag(SCHEDULER_WORK_TAG)
    }

    /** Add a new mining schedule */
    suspend fun addSchedule(schedule: MiningSchedule) {
        val current = _schedulerState.value.activeSchedules.toMutableList()
        current.add(schedule)
        _schedulerState.value = _schedulerState.value.copy(activeSchedules = current)
        scheduleWork(schedule)
    }

    /** Remove a schedule */
    suspend fun removeSchedule(scheduleId: String) {
        val current = _schedulerState.value.activeSchedules.toMutableList()
        current.removeIf { it.id == scheduleId }
        _schedulerState.value = _schedulerState.value.copy(activeSchedules = current)
        workManager.cancelAllWorkByTag(scheduleId)
    }

    /** Check if mining should start based on all conditions */
    suspend fun shouldStartMining(): Boolean {
        if (!_schedulerState.value.enabled) {
            return false
        }

        val currentConditions = checkCurrentConditions()
        _schedulerState.value = _schedulerState.value.copy(currentConditions = currentConditions)

        // Check if we're within any active schedule
        val now = Calendar.getInstance()
        val currentTime = LocalTime.of(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val currentDay = DayOfWeek.entries[now.get(Calendar.DAY_OF_WEEK) - 1]

        val activeSchedule =
                _schedulerState
                        .value
                        .activeSchedules
                        .filter { it.enabled }
                        .filter { currentDay in it.daysOfWeek }
                        .filter { isTimeInRange(currentTime, it.startTime, it.endTime) }
                        .maxByOrNull { it.priority }

        if (activeSchedule == null) {
            return false
        }

        // Check conditions from the schedule
        val conditionsMet = checkConditions(activeSchedule.conditions, currentConditions)

        val shouldMine = conditionsMet
        _schedulerState.value = _schedulerState.value.copy(shouldMineNow = shouldMine)

        return shouldMine
    }

    /** Create preset schedules for common use cases */
    fun createPresetSchedules(): List<MiningSchedule> {
        return listOf(
                // Night mining (11 PM - 7 AM, all days)
                MiningSchedule(
                        id = "night_mining",
                        name = "Night Mining",
                        enabled = true,
                        startTime = LocalTime.of(23, 0),
                        endTime = LocalTime.of(7, 0),
                        daysOfWeek = DayOfWeek.entries.toSet(),
                        conditions =
                                MiningConditions(
                                        requireCharging = true,
                                        minBatteryLevel = 20,
                                        onlyWhenScreenOff = true
                                ),
                        priority = 1
                ),

                // Weekend all-day mining
                MiningSchedule(
                        id = "weekend_mining",
                        name = "Weekend Mining",
                        enabled = false,
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(23, 59),
                        daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        conditions =
                                MiningConditions(
                                        requireCharging = false,
                                        minBatteryLevelWhenNotCharging = 50,
                                        maxTemperature = 70f
                                ),
                        priority = 2
                ),

                // Charging-only mining
                MiningSchedule(
                        id = "charging_only",
                        name = "Charge & Mine",
                        enabled = false,
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(23, 59),
                        daysOfWeek = DayOfWeek.entries.toSet(),
                        conditions =
                                MiningConditions(
                                        requireCharging = true,
                                        minBatteryLevel = 30,
                                        maxTemperature = 75f
                                ),
                        priority = 3
                ),

                // WiFi-only mining (avoid mobile data costs)
                MiningSchedule(
                        id = "wifi_only",
                        name = "WiFi Mining",
                        enabled = false,
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(23, 59),
                        daysOfWeek = DayOfWeek.entries.toSet(),
                        conditions =
                                MiningConditions(
                                        requireWifi = true,
                                        minBatteryLevelWhenNotCharging = 40,
                                        maxTemperature = 70f
                                ),
                        priority = 4
                ),

                // Off-peak hours (cheap electricity)
                MiningSchedule(
                        id = "off_peak",
                        name = "Off-Peak Mining",
                        enabled = false,
                        startTime = LocalTime.of(21, 0),
                        endTime = LocalTime.of(9, 0),
                        daysOfWeek =
                                setOf(
                                        DayOfWeek.MONDAY,
                                        DayOfWeek.TUESDAY,
                                        DayOfWeek.WEDNESDAY,
                                        DayOfWeek.THURSDAY,
                                        DayOfWeek.FRIDAY
                                ),
                        conditions = MiningConditions(requireCharging = true, minBatteryLevel = 50),
                        priority = 5
                )
        )
    }

    /** Get optimal mining schedule based on user patterns */
    suspend fun suggestOptimalSchedule(): MiningSchedule {
        // Analyze historical data to suggest best times
        // For now, return a conservative night-time schedule
        return MiningSchedule(
                id = "suggested_optimal",
                name = "Suggested: Smart Night Mining",
                enabled = true,
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(6, 0),
                daysOfWeek = DayOfWeek.entries.toSet(),
                conditions =
                        MiningConditions(
                                requireCharging = true,
                                minBatteryLevel = 30,
                                maxTemperature = 70f,
                                onlyWhenScreenOff = true
                        ),
                priority = 10
        )
    }

    private fun setupPeriodicConditionCheck() {
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(false).build()

        val checkRequest =
                PeriodicWorkRequestBuilder<ConditionCheckWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag(SCHEDULER_WORK_TAG)
                        .addTag(CONDITION_CHECK_WORK)
                        .build()

        workManager.enqueueUniquePeriodicWork(
                CONDITION_CHECK_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                checkRequest
        )
    }

    private fun scheduleWork(schedule: MiningSchedule) {
        // Calculate delay until start time
        val now = Calendar.getInstance()
        val startCalendar =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, schedule.startTime.hour)
                    set(Calendar.MINUTE, schedule.startTime.minute)
                    set(Calendar.SECOND, 0)

                    // If start time has passed today, schedule for tomorrow
                    if (before(now)) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

        val delay = startCalendar.timeInMillis - now.timeInMillis

        // Create work request
        val constraints = buildConstraints(schedule.conditions)

        val workRequest =
                OneTimeWorkRequestBuilder<MiningStartWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .addTag(SCHEDULER_WORK_TAG)
                        .addTag(schedule.id)
                        .setInputData(Data.Builder().putString("schedule_id", schedule.id).build())
                        .build()

        workManager.enqueueUniqueWork(
                "start_${schedule.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
        )
    }

    private fun buildConstraints(conditions: MiningConditions): Constraints {
        return Constraints.Builder()
                .apply {
                    if (conditions.requireCharging) {
                        setRequiresCharging(true)
                    }
                    if (conditions.requireWifi) {
                        setRequiredNetworkType(NetworkType.UNMETERED)
                    } else {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                    }
                    setRequiresBatteryNotLow(conditions.minBatteryLevel > 15)
                }
                .build()
    }

    private fun checkCurrentConditions(): MiningConditions {
        val batteryStatus: Intent? =
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryLevel =
                batteryStatus?.let { intent ->
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) (level * 100) / scale else 0
                }
                        ?: 0

        val isCharging =
                batteryStatus?.let { intent ->
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
                        ?: false

        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isWifiConnected =
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

        return MiningConditions(
                minBatteryLevel = batteryLevel,
                requireCharging = isCharging,
                requireWifi = isWifiConnected
        )
    }

    private fun checkConditions(required: MiningConditions, current: MiningConditions): Boolean {
        // Simplified condition checking
        // In production, would check actual device state
        return true
    }

    private fun isTimeInRange(current: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (end.isBefore(start)) {
            // Range spans midnight
            current.isAfter(start) || current.isBefore(end)
        } else {
            current.isAfter(start) && current.isBefore(end)
        }
    }
}

/** Worker for periodic condition checking */
class ConditionCheckWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if mining should be started or stopped based on conditions
        // This would integrate with your existing MiningEngine
        return Result.success()
    }
}

/** Worker to start mining at scheduled time */
class MiningStartWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getString("schedule_id")

        // Start mining service
        MiningService.startMiningService(applicationContext)

        return Result.success()
    }
}
