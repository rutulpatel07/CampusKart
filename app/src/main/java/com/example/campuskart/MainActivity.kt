package com.example.campuskart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.campuskart.ui.navigation.CampusKartApp
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * The app's single activity. Everything else is Compose destinations inside the one navigation
 * graph in [CampusKartApp], which opens on Login - or straight on the Feed, when Firebase still
 * holds a signed-in session from a previous run.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusKartTheme {
                CampusKartApp()
            }
        }
    }
}
