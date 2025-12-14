package com.meetmyartist.miner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meetmyartist.miner.gamification.Achievement
import com.meetmyartist.miner.ui.viewmodel.GamificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: GamificationViewModel = hiltViewModel()) {
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsState(initial = emptySet())

    Scaffold(topBar = { TopAppBar(title = { Text("Achievements") }) }) { padding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Achievement.entries) { achievement ->
                AchievementCard(
                        achievement = achievement,
                        isUnlocked = unlockedAchievements.contains(achievement)
                )
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, isUnlocked: Boolean) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isUnlocked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Achievement",
                    modifier = Modifier.size(40.dp),
                    tint =
                            if (isUnlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
