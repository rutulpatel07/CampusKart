package com.example.campuskart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.campuskart.ui.mockups.MockupGallery
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Day 2: the app launches into the mockup gallery so the six screen sketches can be reviewed on
 * a real device. The Day 1 setup check is still reachable as the last entry in that list.
 *
 * From Day 3 this is replaced by the real navigation graph, starting at the Login screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusKartTheme {
                MockupGallery()
            }
        }
    }
}
