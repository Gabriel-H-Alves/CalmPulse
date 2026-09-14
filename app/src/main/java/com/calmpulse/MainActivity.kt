package com.calmpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calmpulse.ui.chat.ChatScreen
import com.calmpulse.ui.theme.CalmPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemInDark) }

            CalmPulseTheme(darkTheme = isDarkTheme) {
                ChatScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
