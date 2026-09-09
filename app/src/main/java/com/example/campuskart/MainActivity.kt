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
 *
 * The Day 1 setup check and the Day 2 mockup gallery are still reachable, but no longer as the
 * launch screen - they now sit behind the "Dev tools" link at the bottom of Login.
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
