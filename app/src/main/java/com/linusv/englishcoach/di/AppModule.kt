package com.linusv.englishcoach.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linusv.englishcoach.EnglishCoachApplication
import com.linusv.englishcoach.auth.AuthRepository
import com.linusv.englishcoach.data.DemoLessonGenerator
import com.linusv.englishcoach.data.EnglishCoachDatabase
import com.linusv.englishcoach.data.FirebaseLessonGenerator
import com.linusv.englishcoach.data.LessonGenerator
import com.linusv.englishcoach.data.LocalLessonRepository

class AppContainer(context: Context) {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS profiles_new (`languageCode` TEXT NOT NULL, `level` TEXT NOT NULL, `goal` TEXT NOT NULL, `interests` TEXT NOT NULL, `sessionMinutes` INTEGER NOT NULL, `voiceTag` TEXT NOT NULL, `completedOnboarding` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`languageCode`))")
            db.execSQL("INSERT INTO profiles_new (`languageCode`,`level`,`goal`,`interests`,`sessionMinutes`,`voiceTag`,`completedOnboarding`,`isActive`) SELECT 'en',`level`,`goal`,`interests`,`sessionMinutes`,CASE WHEN `accent`='UK' THEN 'en-GB' ELSE 'en-US' END,`completedOnboarding`,1 FROM profiles WHERE `id`=1")
            db.execSQL("DROP TABLE profiles")
            db.execSQL("ALTER TABLE profiles_new RENAME TO profiles")

            db.execSQL("ALTER TABLE lessons ADD COLUMN `languageCode` TEXT NOT NULL DEFAULT 'en'")

            db.execSQL("CREATE TABLE IF NOT EXISTS vocabulary_progress_new (`languageCode` TEXT NOT NULL, `term` TEXT NOT NULL, `intervalDays` INTEGER NOT NULL, `nextReviewAt` INTEGER NOT NULL, `reviewCount` INTEGER NOT NULL, PRIMARY KEY(`languageCode`, `term`))")
            db.execSQL("INSERT INTO vocabulary_progress_new (`languageCode`,`term`,`intervalDays`,`nextReviewAt`,`reviewCount`) SELECT 'en',`term`,`intervalDays`,`nextReviewAt`,`reviewCount` FROM vocabulary_progress")
            db.execSQL("DROP TABLE vocabulary_progress")
            db.execSQL("ALTER TABLE vocabulary_progress_new RENAME TO vocabulary_progress")

            db.execSQL("ALTER TABLE speaking_attempts ADD COLUMN `languageCode` TEXT NOT NULL DEFAULT 'en'")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE vocabulary_progress ADD COLUMN lastReviewedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary_progress ADD COLUMN status TEXT NOT NULL DEFAULT 'new'")
            db.execSQL("ALTER TABLE vocabulary_progress ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary_progress ADD COLUMN meaningVi TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE vocabulary_progress ADD COLUMN exampleTarget TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS content_history (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `languageCode` TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS study_sessions (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `activity` TEXT NOT NULL, `completed` INTEGER NOT NULL, `languageCode` TEXT NOT NULL)")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE profiles ADD COLUMN dailyGoalMinutes INTEGER NOT NULL DEFAULT 15")
            db.execSQL("ALTER TABLE profiles ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE profiles ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 20")
        }
    }

    private val database: EnglishCoachDatabase =
        Room.databaseBuilder(context, EnglishCoachDatabase::class.java, "english-coach.db")
            .addMigrations(migration1To2, migration2To3, migration3To4)
            .build()
    private val generator: LessonGenerator =
        if ((context.applicationContext as EnglishCoachApplication).firebaseConfigured) FirebaseLessonGenerator(context) else DemoLessonGenerator()
    val repository = LocalLessonRepository(database.learnerDao(), generator)
    val auth = AuthRepository(context.applicationContext as EnglishCoachApplication)
}
