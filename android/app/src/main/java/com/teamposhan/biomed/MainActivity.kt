package com.teamposhan.biomed

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.teamposhan.biomed.ui.PoshanApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars; every screen applies its own safe-area insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PoshanApplication).container
        setContent {
            PoshanApp(container)
        }
    }
}
