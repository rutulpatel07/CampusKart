package com.example.campuskart.ui.mockups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuskart.setup.SetupStatusScreen
import com.example.campuskart.ui.theme.CampusKartTheme

private data class MockupEntry(val title: String, val note: String)

private val Entries = listOf(
    MockupEntry("1a. Login", "Email, password, link to signup"),
    MockupEntry("1b. Signup", "Name, branch, semester, WhatsApp number, email, password"),
    MockupEntry("2. Home Feed", "Search, category chips, listing cards"),
    MockupEntry("3. Item Detail", "Full listing + the Chat on WhatsApp button"),
    MockupEntry("4. Post Item - empty", "Before a photo has been chosen"),
    MockupEntry("4. Post Item - filled", "After a photo, with the Day 9 AI suggestion in place"),
    MockupEntry("5. My Listings", "Edit / Mark sold / Delete on each card"),
    MockupEntry("5. My Listings - empty", "What a brand new account sees"),
    MockupEntry("6. Profile", "Profile fields and logout"),
    MockupEntry("Day 1 setup check", "The Firebase / Cloudinary connectivity screen"),
)

/**
 * Day 2 deliverable, and throwaway like the rest of this package.
 *
 * Android Studio's `@Preview` panel already renders every mockup, but reviewing them on the
 * actual phone catches things a desktop preview does not - real text sizes, real thumb reach,
 * and whether the fixed green palette holds up on a real display. This gallery exists only so
 * that review can happen by installing the app. Press system back to return to the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockupGallery(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    if (selected != null) {
        BackHandler { selected = null }
        when (selected) {
            0 -> LoginMockup()
            1 -> SignupMockup()
            2 -> HomeFeedMockup()
            3 -> ItemDetailMockup()
            4 -> PostItemMockup(hasPhoto = false)
            5 -> PostItemMockup(hasPhoto = true)
            6 -> MyListingsMockup()
            7 -> MyListingsMockup(empty = true)
            8 -> ProfileMockup()
            else -> SetupStatusScreen()
        }
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("CampusKart - Day 2 mockups") }) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    text = "Sketches only. Nothing here is connected to Firebase or Cloudinary, " +
                        "and no button does anything. Tap a screen to open it, press back to " +
                        "come back here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(Entries) { index, entry ->
                Card(onClick = { selected = index }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@ScreenMockupPreview
@Composable
private fun MockupGalleryPreview() {
    CampusKartTheme { MockupGallery() }
}
