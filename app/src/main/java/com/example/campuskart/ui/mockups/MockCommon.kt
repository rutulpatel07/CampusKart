package com.example.campuskart.ui.mockups

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a mockup at roughly Pixel size in both light and dark, so every screen can be checked
 * against the fixed green-teal palette without opening two preview blocks per screen.
 */
@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 880)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 880,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ScreenMockupPreview

/**
 * The four top-level destinations. Day 2 decision: CampusKart uses a persistent bottom navigation
 * bar (SRS 3.1 left this open between a bottom bar and a top-level graph). Item Detail is *not* a
 * tab - it is pushed on top of the Feed and hides the bar, because it carries its own primary
 * action ("Chat on WhatsApp") at the bottom of the screen.
 */
enum class MockTab(val label: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.Home),
    POST("Post", Icons.Default.Add),
    MINE("My items", Icons.AutoMirrored.Filled.List),
    PROFILE("Profile", Icons.Default.Person),
}

@Composable
fun MockBottomBar(selected: MockTab) {
    NavigationBar {
        MockTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = {}, // mockup: navigation is wired up on Day 3
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * Stands in for a listing photo. Photos stay as flat grey blocks at this stage on purpose - the
 * point of the mockup is the layout around them, and the real images come from Cloudinary from
 * Day 5.
 */
@Composable
fun PhotoPlaceholder(
    modifier: Modifier = Modifier,
    label: String = "PHOTO",
    cornerRadius: Int = 12,
) {
    // Filled *and* outlined: on a card the fill alone is nearly the same tone as the card
    // behind it, and the placeholder disappears.
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Circular avatar with the user's initials - no profile photos anywhere in the app. */
@Composable
fun InitialsAvatar(initials: String, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = (size / 2.6).sp,
        )
    }
}
