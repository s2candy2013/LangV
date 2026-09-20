package com.linusv.englishcoach.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnerDao {
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun profile(): LearnerProfileEntity?

    @Query("SELECT * FROM profiles WHERE languageCode = :languageCode LIMIT 1")
    suspend fun profile(languageCode: String): LearnerProfileEntity?

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateProfiles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LearnerProfileEntity)

    @Transaction
    suspend fun saveProfile(profile: LearnerProfileEntity) {
        deactivateProfiles()
        insertProfile(profile.copy(isActive = true))
    }

    @Query("SELECT * FROM lessons WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun lessons(languageCode: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE languageCode = :languageCode ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestLesson(languageCode: String): LessonEntity?

    @Insert
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVocabulary(item: VocabularyProgressEntity)

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE languageCode = :languageCode")
    fun vocabularyCount(languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE languageCode = :languageCode AND nextReviewAt <= :now")
    fun dueCount(languageCode: String, now: Long): Flow<Int>

    @Query("SELECT * FROM speaking_attempts WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun speakingAttempts(languageCode: String): Flow<List<SpeakingAttemptEntity>>

    @Insert
    suspend fun insertSpeakingAttempt(attempt: SpeakingAttemptEntity)
}

@Database(
    entities = [LearnerProfileEntity::class, LessonEntity::class, VocabularyProgressEntity::class, SpeakingAttemptEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class EnglishCoachDatabase : RoomDatabase() {
    abstract fun learnerDao(): LearnerDao
}
