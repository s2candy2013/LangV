package com.linusv.englishcoach

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.linusv.englishcoach.di.AppContainer

class EnglishCoachApplication : Application() {
    lateinit var container: AppContainer
    val firebaseConfigured: Boolean
        get() = FirebaseApp.getApps(this).isNotEmpty()

    override fun onCreate() {
        super.onCreate()
        val firebaseApp = runCatching { FirebaseApp.initializeApp(this) }.getOrNull()
        if (firebaseApp != null) {
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG || BuildConfig.APP_CHECK_DEBUG) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            } else {
                appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
        // Firebase must be initialized before the container decides between real and demo AI.
        container = AppContainer(this)
    }
}
