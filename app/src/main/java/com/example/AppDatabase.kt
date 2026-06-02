package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "games")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val difficulty: String, // "EASY", "INTERMEDIATE", "HARD", "EXTREME"
    val gridSize: Int,      // 4, 6, 9
    val timeSeconds: Int,
    val hintsUsed: Int,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_challenges")
data class DailyChallenge(
    @PrimaryKey val date: String, // e.g. "2026-05-31"
    val puzzleString: String,     // Solved board + puzzle board state representation
    val completed: Boolean,
    val timeSeconds: Int
)

// ==========================================
// DATA ACCESS OBJECT (DAO)
// ==========================================

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(gameRecord: GameRecord)

    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameRecord>>

    @Query("DELETE FROM games")
    suspend fun clearAllStats()

    // Daily Challenge
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyChallenge(challenge: DailyChallenge)

    @Query("SELECT * FROM daily_challenges WHERE date = :date LIMIT 1")
    suspend fun getDailyChallengeByDate(date: String): DailyChallenge?

    @Query("SELECT * FROM daily_challenges")
    fun getAllDailyChallenges(): Flow<List<DailyChallenge>>
}

// ==========================================
// DATABASE INSTANCE
// ==========================================

@Database(entities = [GameRecord::class, DailyChallenge::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudoku_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// REPOSITORY PATTERN
// ==========================================

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<GameRecord>> = gameDao.getAllGames()
    val allDailyChallenges: Flow<List<DailyChallenge>> = gameDao.getAllDailyChallenges()

    suspend fun saveGame(record: GameRecord) {
        gameDao.insertGame(record)
    }

    suspend fun clearStats() {
        gameDao.clearAllStats()
    }

    suspend fun saveDailyChallenge(challenge: DailyChallenge) {
        gameDao.insertDailyChallenge(challenge)
    }

    suspend fun getDailyChallenge(date: String): DailyChallenge? {
        return gameDao.getDailyChallengeByDate(date)
    }
}
