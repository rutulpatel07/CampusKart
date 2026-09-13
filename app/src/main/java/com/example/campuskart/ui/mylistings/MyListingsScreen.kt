package com.example.campuskart.ui.mylistings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Screen 5 - My Listings (FR-LIST-010), and the only place a seller manages what they posted.
 *
 * Every card carries its own Edit / Mark sold / Delete row, as sketched on Day 2, rather than
 * hiding them behind a long-press or an overflow menu: three actions is few enough to show
 * outright, and it makes each one demonstrable in a single tap.
 *
 * Sold listings stay on this screen, sorted below the available ones. They are gone from the
 * Home Feed - that is what marking sold does - but the seller still needs to see them, both to
 * confirm the change happened and to put one back up if the deal falls through.
 *
 * The screen is a pure function of [MyListingsUiState]; [MyListingsRoute] owns the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    state: MyListingsUiState,
    onOpenListing: (Listing) -> Unit,
    onEdit: (Listing) -> Unit,
    onToggleSold: (Listing) -> Unit,
    onAskDelete: (Listing) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onRefresh: () -> Unit,
    onPostItem: () -> Unit,
    onActionErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // A failed action is reported over the list rather than in place of it: the list is still
    // correct, and the card the user tapped is still there to try again.
    LaunchedEffect(state.actionError) {
        val message = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onActionErrorShown()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My listings") },
                actions = {
                    if (state.refreshing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(20.dp),
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh my listings")
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

                state.error != null -> MyListingsMessage(
                    title = "Could not load your listings",
                    detail = state.error,
                    actionLabel = "Try again",
                    actionIcon = Icons.Default.Refresh,
                    onAction = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.isEmpty -> MyListingsMessage(
                    title = "Nothing listed yet",
                    detail = "Items you post show up here, and you can edit them, mark them sold " +
                        "or delete them from this screen.",
                    actionLabel = "Post your first item",
                    actionIcon = Icons.Default.Add,
                    onAction = onPostItem,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.listings, key = Listing::id) { listing ->
                        MyListingCard(
                            listing = listing,
                            busy = state.busyId == listing.id,
                            // Every action is disabled while any write is in flight, not just
                            // the card's own - the ViewModel runs one at a time, so offering a
                            // second would be a tap that silently does nothing.
                            enabled = state.busyId == null,
                            onClick = { onOpenListing(listing) },
                            onEdit = { onEdit(listing) },
                            onToggleSold = { onToggleSold(listing) },
                            onDelete = { onAskDelete(listing) },
                        )
                    }
                }
            }
        }
    }

    // FR-LIST-009 is irreversible and the photo cannot be recovered from Cloudinary, so it is
    // the one action on this screen that asks first.
    state.pendingDelete?.let { listing ->
        DeleteConfirmation(
            listing = listing,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

/**
 * One of the seller's own listings, with its three actions underneath.
 *
 * The card body opens Item Detail, so a seller can see exactly what a buyer sees - which is also
 * the quickest way to check that an edit landed the way it was meant to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyListingCard(
    listing: Listing,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleSold: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sold = listing.status == Listing.STATUS_SOLD

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                ListingImage(
                    url = listing.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
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
                    StatusBadge(sold = sold)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            if (busy) {
                // Takes the action row's place rather than sitting beside it, so the row cannot
                // be tapped twice and the wait appears where the tap was.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Editing a sold listing would be changing the price of something already
                    // gone. Marking it available again is the way back to editing it.
                    CardAction(
                        icon = Icons.Default.Edit,
                        label = "Edit",
                        enabled = enabled && !sold,
                        onClick = onEdit,
                    )
                    CardAction(
                        icon = if (sold) Icons.Default.AddCircle else Icons.Default.CheckCircle,
                        label = if (sold) "Mark available" else "Mark sold",
                        enabled = enabled,
                        onClick = onToggleSold,
                    )
                    CardAction(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        enabled = enabled,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}

/**
 * AVAILABLE / SOLD, as drawn in the Day 2 sketch.
 *
 * The sold badge is outlined rather than filled, because a grey fill on a grey card reads as
 * nothing at all - and that is exactly the state that most needs to be obvious.
 */
@Composable
private fun StatusBadge(sold: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (sold) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        border = if (sold) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier,
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

/** The one irreversible action on the screen, so it is the one that asks first. */
@Composable
private fun DeleteConfirmation(
    listing: Listing,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text("Delete this listing?") },
        text = {
            Text(
                "\"${listing.title}\" will be removed for good. If you have only sold it, mark " +
                    "it sold instead - that keeps it here and takes it off the feed.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The centred "nothing here" / "that went wrong" block, with the one thing to do about it. */
@Composable
private fun MyListingsMessage(
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
 * Owns the ViewModel and reloads whenever the screen comes back to the front - the same
 * arrangement as the Home Feed, and for the same reason: posting an item and then tapping My
 * items should show it without having to find the Refresh button.
 *
 * That reload is also how a save on the Edit screen gets here. Edit is pushed over this screen,
 * so popping back off it resumes this one, which re-reads the list - no result has to be handed
 * back along the navigation graph.
 */
@Composable
fun MyListingsRoute(
    onOpenListing: (String) -> Unit,
    onEdit: (String) -> Unit,
    onPostItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListingsViewModel = viewModel(),
) {
    LifecycleResumeEffect(Unit) {
        // Skipped on the very first resume: the ViewModel's init has already started that load.
        if (!viewModel.uiState.loading) viewModel.refresh()
        onPauseOrDispose { }
    }

    MyListingsScreen(
        state = viewModel.uiState,
        onOpenListing = { onOpenListing(it.id) },
        onEdit = { onEdit(it.id) },
        onToggleSold = { viewModel.setSold(it, sold = it.status != Listing.STATUS_SOLD) },
        onAskDelete = viewModel::askToDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onRefresh = viewModel::refresh,
        onPostItem = onPostItem,
        onActionErrorShown = viewModel::consumeActionError,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MyListingsScreenPreview() {
    CampusKartTheme {
        MyListingsScreen(
            state = MyListingsUiState(loading = false, listings = PreviewMine),
            onOpenListing = {},
            onEdit = {},
            onToggleSold = {},
            onAskDelete = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onRefresh = {},
            onPostItem = {},
            onActionErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MyListingsEmptyPreview() {
    CampusKartTheme {
        MyListingsScreen(
            state = MyListingsUiState(loading = false),
            onOpenListing = {},
            onEdit = {},
            onToggleSold = {},
            onAskDelete = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onRefresh = {},
            onPostItem = {},
            onActionErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun MyListingsDeletingPreview() {
    CampusKartTheme {
        MyListingsScreen(
            state = MyListingsUiState(
                loading = false,
                listings = PreviewMine,
                pendingDelete = PreviewMine.first(),
            ),
            onOpenListing = {},
            onEdit = {},
            onToggleSold = {},
            onAskDelete = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onRefresh = {},
            onPostItem = {},
            onActionErrorShown = {},
        )
    }
}

/** Preview-only rows: one still for sale, one already sold. */
private val PreviewMine = listOf(
    Listing(
        id = "1",
        title = "Engineering Drawing drafter set",
        price = 350,
        category = "Drafter",
        condition = "Good",
        status = Listing.STATUS_AVAILABLE,
    ),
    Listing(
        id = "2",
        title = "Lab coat, size M, barely used",
        price = 200,
        category = "Lab Coat",
        condition = "New",
        status = Listing.STATUS_SOLD,
    ),
)
