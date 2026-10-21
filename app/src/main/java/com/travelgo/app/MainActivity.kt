package com.travelgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.travelgo.app.navigation.TravelGoApp
import com.travelgo.app.ui.theme.TravelGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelGoTheme {
                TravelGoApp()
            }
        }
    }
}
