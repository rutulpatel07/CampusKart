package com.example.campuskart.ui.mockups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

/** The signed-in user's own listings - one still for sale, one already sold. */
private val MyMockListings = listOf(
    MockFeed[0],
    MockFeed[2].copy(sold = true),
)

/**
 * Screen 5 - My Listings (FR-LIST-010). Each card carries its own Edit / Mark as sold / Delete
 * row (FR-LIST-007/008/009) rather than hiding them behind a long-press or an overflow menu -
 * three actions is few enough to show outright, and it makes them demonstrable in one tap.
 *
 * @param empty renders the empty state, which is what a brand new account actually sees.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsMockup(
    empty: Boolean = false,
    modifier: Modifier = Modifier,
    showTabBar: Boolean = true,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("My listings") }) },
        // Hidden when the Day 3 navigation shell hosts this sketch, because the shell draws
        // the real bottom bar itself - otherwise the screen would show two of them.
        bottomBar = { if (showTabBar) MockBottomBar(MockTab.MINE) },
    ) { inner ->
        if (empty) {
            EmptyState(modifier = Modifier.padding(inner))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(MyMockListings) { listing -> MyListingCard(listing) }
            }
        }
    }
}

@Composable
private fun MyListingCard(listing: MockListing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                PhotoPlaceholder(modifier = Modifier.size(72.dp))
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₹${listing.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusBadge(sold = listing.sold)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CardAction(Icons.Default.Edit, "Edit", enabled = !listing.sold)
                CardAction(
                    icon = Icons.Default.CheckCircle,
                    label = if (listing.sold) "Sold" else "Mark sold",
                    enabled = !listing.sold,
                )
                CardAction(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = {},
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}

@Composable
private fun StatusBadge(sold: Boolean) {
    // The sold badge is outlined rather than filled: a grey fill on a grey card reads as
    // nothing at all, which is exactly the state that most needs to be obvious.
    Surface(
        color = if (sold) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        border = if (sold) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = if (sold) "SOLD" else "AVAILABLE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (sold) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Sketched now, built on Day 8. A first-run user lands here with nothing, and an empty screen
 * with no explanation is the most common way a demo looks broken.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoPlaceholder(
            modifier = Modifier.size(96.dp),
            label = "",
            cornerRadius = 48,
        )
        Spacer(Modifier.height(20.dp))
        Text("Nothing listed yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Items you post will show up here, and you can edit, mark them sold or " +
                "delete them from this screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = {}, modifier = Modifier.height(48.dp)) {
            Text("Post your first item")
        }
    }
}

@ScreenMockupPreview
@Composable
private fun MyListingsMockupPreview() {
    CampusKartTheme { MyListingsMockup() }
}

@ScreenMockupPreview
@Composable
private fun MyListingsEmptyMockupPreview() {
    CampusKartTheme { MyListingsMockup(empty = true) }
}
