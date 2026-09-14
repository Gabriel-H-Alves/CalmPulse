package com.calmpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.calmpulse.ui.chat.ChatScreen
import com.calmpulse.ui.theme.CalmPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalmPulseTheme {
                ChatScreen()
            }
        }
    }
}
