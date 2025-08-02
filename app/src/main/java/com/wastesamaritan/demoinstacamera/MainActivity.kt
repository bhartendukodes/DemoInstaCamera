package com.wastesamaritan.demoinstacamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wastesamaritan.demoinstacamera.ui.theme.DemoInstaCameraTheme
import com.wastesamaritan.demoinstacamera.presentation.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoInstaCameraTheme {
                MainScreen()
            }
        }
    }
}
