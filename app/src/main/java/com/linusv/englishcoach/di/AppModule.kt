package com.linusv.englishcoach.di

import android.content.Context
import androidx.room.Room
import com.linusv.englishcoach.EnglishCoachApplication
import com.linusv.englishcoach.auth.AuthRepository
import com.linusv.englishcoach.data.DemoLessonGenerator
import com.linusv.englishcoach.data.EnglishCoachDatabase
import com.linusv.englishcoach.data.FirebaseLessonGenerator
import com.linusv.englishcoach.data.LessonGenerator
import com.linusv.englishcoach.data.LocalLessonRepository

class AppContainer(context: Context) {
    private val database: EnglishCoachDatabase =
        Room.databaseBuilder(context, EnglishCoachDatabase::class.java, "english-coach.db").build()
    private val generator: LessonGenerator =
        if ((context.applicationContext as EnglishCoachApplication).firebaseConfigured) FirebaseLessonGenerator() else DemoLessonGenerator()
    val repository = LocalLessonRepository(database.learnerDao(), generator)
    val auth = AuthRepository(context.applicationContext as EnglishCoachApplication)
}
