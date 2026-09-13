package com.example.campuskart.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.data.Listing
import com.example.campuskart.data.UserProfile
import com.example.campuskart.data.WhatsAppContact
import com.example.campuskart.ui.components.ListingImage
import com.example.campuskart.ui.theme.CampusKartTheme
import com.example.campuskart.ui.theme.OnWhatsAppGreen
import com.example.campuskart.ui.theme.WhatsAppGreen
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Screen 3 - Item Detail (FR-LIST-006), the second half of the app's core flow.
 *
 * Pushed on top of the Feed rather than living in a tab, so it replaces the bottom navigation
 * bar: the whole screen exists to lead to one action, and that action - "Chat on WhatsApp"
 * (FR-CONTACT-001) - is pinned to the bottom in WhatsApp's own green, reachable with a thumb
 * without scrolling.
 *
 * The seller's number is nowhere on this screen (FR-CONTACT-003). It only ever goes into the
 * `wa.me` link at the moment the button is tapped.
 *
 * The screen is a pure function of [ItemDetailUiState]; [ItemDetailRoute] owns the ViewModel and
 * the handoff to WhatsApp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onBack: () -> Unit,
    onContactSeller: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Only once there is a listing: a bar offering to message the seller of a listing
            // that failed to load would be a button with nothing behind it.
            state.listing?.let {
                ContactBar(state = state, onContactSeller = onContactSeller)
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
        ) {
            val listing = state.listing
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.missing -> DetailMessage(
                    title = "This listing is gone",
                    detail = "The seller removed it, or it has already been sold.",
                    actionLabel = "Back to the feed",
                    onAction = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.error != null || listing == null -> DetailMessage(
                    title = "Could not load this listing",
                    detail = state.error ?: "Please try again.",
                    actionLabel = "Try again",
                    onAction = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> ListingBody(listing = listing, seller = state.seller)
            }
        }
    }
}

@Composable
private fun ListingBody(listing: Listing, seller: UserProfile?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ListingImage(
            url = listing.photoUrl,
            contentDescription = "Photo of ${listing.title}",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = listing.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "₹${listing.price}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (listing.category.isNotBlank()) DetailChip(listing.category)
                if (listing.condition.isNotBlank()) DetailChip(listing.condition)
                DetailChip(
                    text = if (listing.status == Listing.STATUS_SOLD) "Sold" else "Available",
                    highlighted = listing.status != Listing.STATUS_SOLD,
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Description", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = listing.description.ifBlank { "The seller did not add a description." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Seller", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            SellerRow(listing = listing, seller = seller)

            Spacer(Modifier.height(12.dp))
            Text(
                text = postedLabel(listing.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The seller's identity line.
 *
 * The name comes off the listing and is there the instant the screen draws; branch and semester
 * come from the seller's live `users` document and arrive a moment later, so the second line
 * falls back to the branch copied onto the listing while that read is in flight - the row never
 * appears empty and never jumps in height.
 */
@Composable
private fun SellerRow(listing: Listing, seller: UserProfile?, modifier: Modifier = Modifier) {
    val name = seller?.name?.takeIf(String::isNotBlank) ?: listing.sellerName
    val detail = when {
        seller != null && seller.branch.isNotBlank() ->
            "${seller.branch} · Semester ${seller.semester}"

        listing.sellerBranch.isNotBlank() -> listing.sellerBranch
        else -> "Branch not given"
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(name = name)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = name.ifBlank { "Unknown seller" },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Circular avatar with the seller's initials - the app stores no profile photos. */
@Composable
private fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim().split(Regex("\\s+"))
        .filter(String::isNotEmpty)
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = initials, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * The pinned bottom bar: the "Chat on WhatsApp" button, or what replaces it.
 *
 * On the user's own listing the button is gone entirely rather than disabled, because there is
 * nothing wrong to fix - messaging yourself is simply not a thing the screen should offer.
 */
@Composable
private fun ContactBar(
    state: ItemDetailUiState,
    onContactSeller: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, modifier = modifier) {
        Column(
            modifier = Modifier
                // Scaffold does not inset an arbitrary bottomBar, so without this the helper line
                // sits underneath the system gesture bar.
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            if (state.isOwnListing) {
                Text(
                    text = "This is your listing",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    // There is nobody to message, so the space the WhatsApp button would take
                    // points at where the listing is actually managed (FR-LIST-007/008/009).
                    text = "Edit it, mark it sold or delete it from My items.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            Button(
                onClick = onContactSeller,
                enabled = state.canContactSeller,
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
                text = state.contactBlockedReason
                    ?: "Opens WhatsApp with a message about this listing",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.contactBlockedReason != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A read-only fact pill. [highlighted] marks a listing that is still available. */
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

@Composable
private fun DetailMessage(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/**
 * "Posted 2 days ago", from the server timestamp.
 *
 * Rounded to the coarsest unit that still says something useful, because the exact minute a
 * drafter set went on sale is not information anyone acts on - whereas "3 weeks ago" versus
 * "today" changes whether a buyer bothers messaging at all.
 */
private fun postedLabel(createdAt: Timestamp?): String {
    // Null while a just-published listing is still being read back from the local cache, before
    // the server has stamped it.
    createdAt ?: return "Just posted"

    val elapsed = System.currentTimeMillis() - createdAt.toDate().time
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)

    return when {
        // A clock that is slightly behind the server's would otherwise produce "in 1 minute".
        elapsed < 0 -> "Posted just now"
        minutes < 1 -> "Posted just now"
        minutes < 60 -> "Posted $minutes ${plural(minutes, "minute")} ago"
        hours < 24 -> "Posted $hours ${plural(hours, "hour")} ago"
        days < 30 -> "Posted $days ${plural(days, "day")} ago"
        else -> "Posted ${days / 30} ${plural(days / 30, "month")} ago"
    }
}

private fun plural(count: Long, unit: String) = if (count == 1L) unit else "${unit}s"

/**
 * Owns the ViewModel and performs the actual handoff to WhatsApp.
 *
 * The handoff is here rather than in the ViewModel because it needs an Activity context and
 * because it is navigation, not state: nothing about the screen changes when WhatsApp opens. The
 * only thing that comes back is whether anything on the device took the intent, and that is
 * reported as a snackbar.
 */
@Composable
fun ItemDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemDetailViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ItemDetailScreen(
        state = viewModel.uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        snackbarHostState = snackbarHostState,
        onContactSeller = {
            val state = viewModel.uiState
            val number = state.contactNumber
            val title = state.listing?.title
            val opened = number != null && title != null &&
                WhatsAppContact.openChat(context, number, title)

            if (!opened) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Could not open WhatsApp on this device. Is it installed?",
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun ItemDetailScreenPreview() {
    CampusKartTheme {
        ItemDetailScreen(
            state = ItemDetailUiState(
                loading = false,
                listing = PreviewListing,
                seller = PreviewSeller,
            ),
            onBack = {},
            onContactSeller = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun ItemDetailOwnListingPreview() {
    CampusKartTheme {
        ItemDetailScreen(
            state = ItemDetailUiState(
                loading = false,
                listing = PreviewListing,
                isOwnListing = true,
            ),
            onBack = {},
            onContactSeller = {},
            onRetry = {},
        )
    }
}

private val PreviewListing = Listing(
    id = "preview",
    title = "Engineering Drawing drafter set",
    description = "Full drafter set from Engineering Drawing - mini-drafter, set squares, " +
        "compass box and a roll of drawing sheets. Used for one semester only, all parts " +
        "present. Can hand over on campus.",
    category = "Drafter",
    price = 350,
    condition = "Good",
    sellerName = "Aarav Shah",
    sellerBranch = "CE",
)

private val PreviewSeller = UserProfile(
    name = "Aarav Shah",
    branch = "CE",
    semester = "5",
    whatsappNumber = "9876543210",
)
