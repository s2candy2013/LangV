package com.linusv.englishcoach.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnerDao {
    @Query("SELECT * FROM profiles WHERE id = 1")
    suspend fun profile(): LearnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: LearnerProfileEntity)

    @Query("SELECT * FROM lessons ORDER BY createdAt DESC")
    fun lessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestLesson(): LessonEntity?

    @Insert
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVocabulary(item: VocabularyProgressEntity)

    @Query("SELECT COUNT(*) FROM vocabulary_progress")
    fun vocabularyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE nextReviewAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM speaking_attempts ORDER BY createdAt DESC")
    fun speakingAttempts(): Flow<List<SpeakingAttemptEntity>>

    @Insert
    suspend fun insertSpeakingAttempt(attempt: SpeakingAttemptEntity)
}

@Database(
    entities = [LearnerProfileEntity::class, LessonEntity::class, VocabularyProgressEntity::class, SpeakingAttemptEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EnglishCoachDatabase : RoomDatabase() {
    abstract fun learnerDao(): LearnerDao
}
