package com.lin0721.linmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lin0721.linmusic.ui.home.HomeScreen
import com.lin0721.linmusic.ui.theme.LinMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinMusicTheme {
                HomeScreen()
            }
        }
    }
}