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

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun lesson(id: Long): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVocabulary(item: VocabularyProgressEntity)

    @Query("SELECT * FROM vocabulary_progress WHERE languageCode = :languageCode AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun dueVocabulary(languageCode: String, now: Long): Flow<List<VocabularyProgressEntity>>

    @Query("SELECT * FROM vocabulary_progress WHERE languageCode = :languageCode ORDER BY reviewCount ASC, nextReviewAt ASC")
    fun vocabulary(languageCode: String): Flow<List<VocabularyProgressEntity>>

    @Query("SELECT * FROM vocabulary_progress WHERE languageCode = :languageCode AND term = :term LIMIT 1")
    suspend fun vocabulary(languageCode: String, term: String): VocabularyProgressEntity?

    @Query("UPDATE vocabulary_progress SET isFavorite = :favorite WHERE languageCode = :languageCode AND term = :term")
    suspend fun setFavorite(languageCode: String, term: String, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE languageCode = :languageCode")
    fun vocabularyCount(languageCode: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_progress WHERE languageCode = :languageCode AND nextReviewAt <= :now")
    fun dueCount(languageCode: String, now: Long): Flow<Int>

    @Query("SELECT * FROM speaking_attempts WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun speakingAttempts(languageCode: String): Flow<List<SpeakingAttemptEntity>>

    @Insert
    suspend fun insertSpeakingAttempt(attempt: SpeakingAttemptEntity)

    @Insert
    suspend fun insertContent(item: ContentHistoryEntity): Long

    @Query("SELECT * FROM content_history WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun contentHistory(languageCode: String): Flow<List<ContentHistoryEntity>>

    @Insert
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun studySessions(languageCode: String): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM study_sessions WHERE languageCode = :languageCode")
    fun totalStudyMinutes(languageCode: String): Flow<Int>
}

@Database(
    entities = [LearnerProfileEntity::class, LessonEntity::class, VocabularyProgressEntity::class, SpeakingAttemptEntity::class, ContentHistoryEntity::class, StudySessionEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class EnglishCoachDatabase : RoomDatabase() {
    abstract fun learnerDao(): LearnerDao
}
