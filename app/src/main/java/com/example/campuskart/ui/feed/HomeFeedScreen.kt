package com.example.campuskart.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.data.Listing
import com.example.campuskart.ui.components.ListingImage
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 2 - Home Feed, the app's landing screen once signed in (FR-LIST-003).
 *
 * Every available listing, newest first, as cards carrying the four things a buyer scans for:
 * the photo, the title, the price, and who is selling it. The seller's branch rides along
 * because signup is open to anyone (PRD.md Section 6), so it is the only trust signal the feed
 * has to offer.
 *
 * Day 7 adds the search field and category chips above the list (FR-LIST-004/005). They are
 * deliberately absent rather than stubbed: a search box that does nothing when tapped is worse
 * than no search box, and the Day 2 mockup already shows where they will sit.
 *
 * The screen is a pure function of [HomeFeedUiState]; [HomeFeedRoute] owns the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    state: HomeFeedUiState,
    onOpenListing: (Listing) -> Unit,
    onRefresh: () -> Unit,
    onPostItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                actions = {
                    // A one-shot query rather than a live listener means the feed can go stale
                    // (see ListingRepository.availableListings), so there has to be a visible way
                    // to ask for it again. The spinner takes the button's place while it runs, so
                    // the control cannot be tapped twice and the wait is where the tap was.
                    if (state.refreshing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(20.dp),
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh listings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> FeedMessage(
                    title = "Could not load the feed",
                    detail = state.error,
                    actionLabel = "Try again",
                    actionIcon = Icons.Default.Refresh,
                    onAction = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.isEmpty -> FeedMessage(
                    title = "No listings yet",
                    detail = "Nobody has posted anything for sale. Be the first - it takes about " +
                        "a minute.",
                    actionLabel = "Post an item",
                    actionIcon = Icons.Default.Add,
                    onAction = onPostItem,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Keyed by document id so that a refresh which reorders or removes rows
                    // reuses the composables it already has, instead of rebuilding the list and
                    // re-requesting every photo Coil has already cached.
                    items(state.listings, key = Listing::id) { listing ->
                        FeedCard(listing = listing, onClick = { onOpenListing(listing) })
                    }
                }
            }
        }
    }
}

/**
 * One row of the feed. Photo on the left, then title, price, and the seller line.
 *
 * The whole card is the tap target rather than a "View" button on it: this is a list whose only
 * possible action per row is "open it", and a row-sized target is far easier to hit on a phone
 * than a button is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedCard(listing: Listing, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            ListingImage(
                url = listing.photoUrl,
                // The title is already read out by the text below it, so repeating it here would
                // make TalkBack announce every card twice.
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

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
                    // Listings posted before Day 6 carry no branch, so the separator is dropped
                    // with it rather than leaving the card reading "Aarav Shah  ·".
                    text = listOf(listing.sellerName, listing.sellerBranch)
                        .filter(String::isNotBlank)
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The centred "nothing here" / "that went wrong" block, with the one thing to do about it. */
@Composable
private fun FeedMessage(
    title: String,
    detail: String,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) {
            Icon(actionIcon, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(actionLabel)
        }
    }
}

/**
 * Owns the feed's ViewModel and reloads it whenever the tab comes back to the front.
 *
 * That reload is what makes "post an item, then tap Feed" show the new listing. The bottom bar
 * keeps each tab's state alive (`restoreState` in CampusKartApp), so the ViewModel - and the
 * list it loaded minutes ago - survives the trip to Post Item and back; without this the user
 * would have to find the Refresh button to see their own listing appear.
 */
@Composable
fun HomeFeedRoute(
    onOpenListing: (String) -> Unit,
    onPostItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeFeedViewModel = viewModel(),
) {
    LifecycleResumeEffect(Unit) {
        // Skipped on the very first resume: the ViewModel's init has already started that load,
        // and refreshing here as well would fire the same query twice on every cold start.
        if (!viewModel.uiState.loading) viewModel.refresh()
        onPauseOrDispose { }
    }

    HomeFeedScreen(
        state = viewModel.uiState,
        onOpenListing = { onOpenListing(it.id) },
        onRefresh = viewModel::refresh,
        onPostItem = onPostItem,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun HomeFeedScreenPreview() {
    CampusKartTheme {
        HomeFeedScreen(
            state = HomeFeedUiState(loading = false, listings = PreviewListings),
            onOpenListing = {},
            onRefresh = {},
            onPostItem = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun HomeFeedEmptyPreview() {
    CampusKartTheme {
        HomeFeedScreen(
            state = HomeFeedUiState(loading = false),
            onOpenListing = {},
            onRefresh = {},
            onPostItem = {},
        )
    }
}

/**
 * Preview-only rows. Real [Listing] objects rather than the Day 2 mockup's stand-ins, so the
 * preview breaks if the model changes - which is the entire point of having it.
 */
private val PreviewListings = listOf(
    Listing(
        id = "1",
        title = "Engineering Drawing drafter set",
        price = 350,
        category = "Drafter",
        condition = "Good",
        sellerName = "Aarav Shah",
        sellerBranch = "CE",
    ),
    Listing(
        id = "2",
        title = "Casio FX-991EX scientific calculator",
        price = 800,
        category = "Calculator",
        condition = "Good",
        sellerName = "Priya Mehta",
        sellerBranch = "IT",
    ),
    Listing(
        id = "3",
        title = "Lab coat, size M, barely used",
        price = 200,
        category = "Lab Coat",
        condition = "New",
        sellerName = "Devansh Patel",
        sellerBranch = "CE-AI",
    ),
)
