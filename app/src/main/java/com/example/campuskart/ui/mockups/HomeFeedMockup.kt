package com.example.campuskart.ui.mockups

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 2 - Home Feed. The app's landing screen once signed in.
 *
 * Search sits in the content as a permanent field rather than behind an icon in the app bar:
 * it is one of the two ways to narrow the feed (FR-LIST-005) and hiding it behind a tap makes
 * the feature invisible during the live demo. The category chips (FR-LIST-004) scroll
 * horizontally beneath it, with "All" as the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedMockup(modifier: Modifier = Modifier, showTabBar: Boolean = true) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CampusKart",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        // Hidden when the Day 3 navigation shell hosts this sketch, because the shell draws
        // the real bottom bar itself - otherwise the screen would show two of them.
        bottomBar = { if (showTabBar) MockBottomBar(MockTab.FEED) },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Search listings") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    FilterChip(selected = true, onClick = {}, label = { Text("All") })
                }
                items(MockCategories) { category ->
                    FilterChip(selected = false, onClick = {}, label = { Text(category) })
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(MockFeed) { listing -> FeedCard(listing) }
            }
        }
    }
}

/**
 * One row of the feed. Photo on the left, then title, price and the seller's branch - the branch
 * is the only trust signal an open (non-college-restricted) signup gives us, so it earns a place
 * on the card rather than only on the detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedCard(listing: MockListing) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            PhotoPlaceholder(modifier = Modifier.size(88.dp))
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
                Text(
                    text = "${listing.category}  ·  ${listing.condition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${listing.sellerName}  ·  ${listing.sellerBranch}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ScreenMockupPreview
@Composable
private fun HomeFeedMockupPreview() {
    CampusKartTheme { HomeFeedMockup() }
}
