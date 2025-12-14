package com.meetmyartist.miner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetmyartist.miner.gamification.AchievementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val achievementManager: AchievementManager
) : ViewModel() {
    val unlockedAchievements = achievementManager.unlockedAchievementsFlow
}
