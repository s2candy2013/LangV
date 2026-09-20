package com.linusv.englishcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.linusv.englishcoach.ui.EnglishCoachApp
import com.linusv.englishcoach.ui.theme.EnglishCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as EnglishCoachApplication).container
        setContent { EnglishCoachTheme { EnglishCoachApp(container) } }
    }
}
