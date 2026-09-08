package com.example.campuskart.ui.mockups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme
import com.example.campuskart.ui.theme.OnWhatsAppGreen
import com.example.campuskart.ui.theme.WhatsAppGreen

/**
 * Screen 3 - Item Detail. Pushed on top of the feed, so it replaces the bottom navigation bar
 * rather than sitting inside a tab: the whole screen exists to lead to one action.
 *
 * That action - "Chat on WhatsApp" (FR-CONTACT-001/002) - is pinned to the bottom in WhatsApp's
 * own green so it is unmistakable and reachable with a thumb without scrolling. Everything above
 * it is read-only detail. The seller's number is deliberately absent from the screen; it only
 * ever goes into the `wa.me` link when the button is tapped (FR-CONTACT-003).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailMockup(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val listing = MockDetailListing

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Listing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = { WhatsAppContactBar() },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PhotoPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                cornerRadius = 0,
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "₹${listing.price}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailChip(listing.category)
                    DetailChip(listing.condition)
                    DetailChip("Available", highlighted = true)
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Description", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = MockDetailDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Seller", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(initials = "AS", size = 44)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(listing.sellerName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${listing.sellerBranch} · Semester ${listing.sellerSemester}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Posted 2 days ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WhatsAppContactBar() {
    // Scaffold does not inset an arbitrary bottomBar, so without this the helper line sits
    // underneath the system gesture bar.
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = OnWhatsAppGreen,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("Chat on WhatsApp", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Opens WhatsApp with a message about this listing",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A read-only fact pill. `highlighted` marks the listing's status (available / sold). */
@Composable
private fun DetailChip(text: String, highlighted: Boolean = false) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        colors = if (highlighted) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
    )
}

@ScreenMockupPreview
@Composable
private fun ItemDetailMockupPreview() {
    CampusKartTheme { ItemDetailMockup() }
}
