package com.meetmyartist.miner.gamification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class Achievement(val title: String, val description: String) {
    FIRST_SESSION("First Session", "Complete your first mining session."),
    HOUR_OF_POWER("Hour of Power", "Mine continuously for 1 hour."),
    NIGHT_OWL("Night Owl", "Mine for 8 hours overnight."),
    WALLET_MASTER("Wallet Master", "Add 3 different cryptocurrency wallets."),
    PROFIT_HUNTER("Profit Hunter", "Use the profitability calculator for the first time.")
}

private val Context.achievementStore: DataStore<Preferences> by preferencesDataStore(
    name = "achievement_store"
)

@Singleton
class AchievementManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACHIEVEMENTS_KEY = stringSetPreferencesKey("unlocked_achievements")
    private val dataStore = context.achievementStore

    val unlockedAchievementsFlow = dataStore.data.map { preferences ->
        preferences[ACHIEVEMENTS_KEY]?.mapNotNull { Achievement.valueOf(it) }?.toSet() ?: emptySet()
    }

    suspend fun unlockAchievement(achievement: Achievement) {
        dataStore.edit { settings ->
            val current = settings[ACHIEVEMENTS_KEY] ?: emptySet()
            if (!current.contains(achievement.name)) {
                settings[ACHIEVEMENTS_KEY] = current + achievement.name
                // Here you would trigger a UI notification
            }
        }
    }

    suspend fun getUnlockedAchievements(): Set<Achievement> = unlockedAchievementsFlow.first()
}
