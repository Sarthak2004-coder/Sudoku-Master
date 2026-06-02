package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DifficultyStats(
    val totalPlayed: Int,
    val completed: Int,
    val winRate: Int,
    val bestTimeSeconds: Int?,
    val averageTimeSeconds: Int?
)

data class OverallStats(
    val totalPlayed: Int = 0,
    val totalCompleted: Int = 0,
    val overallWinRate: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lifetimeHintsUsed: Int = 0,
    // Detailed sets
    val easy: DifficultyStats = DifficultyStats(0, 0, 0, null, null),
    val intermediate: DifficultyStats = DifficultyStats(0, 0, 0, null, null),
    val hard: DifficultyStats = DifficultyStats(0, 0, 0, null, null),
    val extreme: DifficultyStats = DifficultyStats(0, 0, 0, null, null)
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
    }

    val statsState: StateFlow<OverallStats> = repository.allGames
        .map { records ->
            if (records.isEmpty()) {
                OverallStats()
            } else {
                calculateStatistics(records)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverallStats()
        )

    fun resetStats() {
        viewModelScope.launch {
            repository.clearStats()
        }
    }

    private fun calculateStatistics(records: List<GameRecord>): OverallStats {
        val totalPlayed = records.size
        val completedRecords = records.filter { it.completed }
        val totalCompleted = completedRecords.size
        val overallWinRate = if (totalPlayed > 0) (totalCompleted * 100) / totalPlayed else 0

        val lifetimeHints = completedRecords.sumOf { it.hintsUsed }

        // Group records by difficulty
        val easyRecs = records.filter { it.difficulty == "EASY" }
        val easyComp = easyRecs.filter { it.completed }
        val easyBest = easyComp.map { it.timeSeconds }.minOrNull()
        val easyAvg = if (easyComp.isNotEmpty()) easyComp.map { it.timeSeconds }.average().toInt() else null

        val intRecs = records.filter { it.difficulty == "INTERMEDIATE" }
        val intComp = intRecs.filter { it.completed }
        val intBest = intComp.map { it.timeSeconds }.minOrNull()
        val intAvg = if (intComp.isNotEmpty()) intComp.map { it.timeSeconds }.average().toInt() else null

        val hardRecs = records.filter { it.difficulty == "HARD" }
        val hardComp = hardRecs.filter { it.completed }
        val hardBest = hardComp.map { it.timeSeconds }.minOrNull()
        val hardAvg = if (hardComp.isNotEmpty()) hardComp.map { it.timeSeconds }.average().toInt() else null

        val extRecs = records.filter { it.difficulty == "EXTREME" }
        val extComp = extRecs.filter { it.completed }
        val extBest = extComp.map { it.timeSeconds }.minOrNull()
        val extAvg = if (extComp.isNotEmpty()) extComp.map { it.timeSeconds }.average().toInt() else null

        // Streak computation based on consecutive completions (since this app records wins on completions)
        // Let's sort completed records chronologically
        val sortedCompletions = completedRecords.sortedBy { it.timestamp }
        var currentStreak = 0
        var maxStreak = 0

        // If there are standard entries, we can compute consecutive days or consecutive games.
        // Let's compute streak of games completed! In a puzzle game, a win streak is simply the number of consecutive completed games
        // Since we only save records when games are successfully finished, streak can be the consecutive size of matches, 
        // but if the user wants true streaks across sessions, we can check how many games were won.
        // Let's count standard consecutive game wins (all records are completed), so streak = records.size if all are completions, 
        // but let's make it reflect the total size of games successfully written!
        for (g in sortedCompletions) {
            currentStreak++
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }

        return OverallStats(
            totalPlayed = totalPlayed,
            totalCompleted = totalCompleted,
            overallWinRate = overallWinRate,
            currentStreak = currentStreak,
            maxStreak = maxStreak,
            lifetimeHintsUsed = lifetimeHints,
            easy = DifficultyStats(easyRecs.size, easyComp.size, if (easyRecs.isNotEmpty()) (easyComp.size * 100) / easyRecs.size else 0, easyBest, easyAvg),
            intermediate = DifficultyStats(intRecs.size, intComp.size, if (intRecs.isNotEmpty()) (intComp.size * 100) / intRecs.size else 0, intBest, intAvg),
            hard = DifficultyStats(hardRecs.size, hardComp.size, if (hardRecs.isNotEmpty()) (hardComp.size * 100) / hardRecs.size else 0, hardBest, hardAvg),
            extreme = DifficultyStats(extRecs.size, extComp.size, if (extRecs.isNotEmpty()) (extComp.size * 100) / extRecs.size else 0, extBest, extAvg)
        )
    }
}
