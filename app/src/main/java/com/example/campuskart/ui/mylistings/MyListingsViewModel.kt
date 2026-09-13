package com.example.campuskart.ui.mylistings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.FeedOutcome
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingActionOutcome
import com.example.campuskart.data.ListingRepository
import kotlinx.coroutines.launch

/** Everything the My Listings screen draws. */
data class MyListingsUiState(
    val listings: List<Listing> = emptyList(),
    /** The first load, which owns the whole screen. */
    val loading: Boolean = true,
    /** A reload on top of cards already drawn - shown in the app bar, not over the list. */
    val refreshing: Boolean = false,
    val error: String? = null,
    /**
     * The listing whose write is in flight, if any.
     *
     * Per-card rather than one screen-wide flag, because the actions live on the cards: a
     * spinner over the whole list would hide which of five listings is being marked sold.
     */
    val busyId: String? = null,
    /** A failed edit/sold/delete, shown once as a snackbar and then dropped. */
    val actionError: String? = null,
    /** The listing the "Delete this listing?" dialog is currently asking about. */
    val pendingDelete: Listing? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && listings.isEmpty()
}

/**
 * Loads the signed-in user's own listings and runs the three owner-only actions on them
 * (FR-LIST-007/008/009/010).
 *
 * After a successful write the affected row is updated in place rather than the whole list being
 * read back from Firestore. The server has already acknowledged the write by the time the
 * coroutine resumes, so the new state is known for certain - and re-reading would cost a
 * document read per listing and blank the screen for a moment to redraw rows that did not
 * change. Refresh, and the reload whenever the screen comes back to the front, cover the two
 * cases this cannot: a change made on another device, and a save on the Edit screen - which is
 * pushed over this one, so popping back off it resumes this screen and reloads the list anyway.
 */
class MyListingsViewModel : ViewModel() {

    var uiState by mutableStateOf(MyListingsUiState())
        private set

    init {
        load()
    }

    fun load(refresh: Boolean = false) {
        if (uiState.loading && refresh) return
        uiState = uiState.copy(loading = !refresh, refreshing = refresh, error = null)
        viewModelScope.launch {
            uiState = when (val outcome = ListingRepository.myListings()) {
                is FeedOutcome.Success -> uiState.copy(
                    listings = outcome.listings.sortedForDisplay(),
                    loading = false,
                    refreshing = false,
                    error = null,
                )

                is FeedOutcome.Failure -> uiState.copy(
                    loading = false,
                    refreshing = false,
                    error = outcome.message,
                )
            }
        }
    }

    fun refresh() = load(refresh = true)

    /** FR-LIST-008, both ways: sold takes it off the feed, available puts it back on. */
    fun setSold(listing: Listing, sold: Boolean) {
        val status = if (sold) Listing.STATUS_SOLD else Listing.STATUS_AVAILABLE
        act(
            listing = listing,
            write = { ListingRepository.setStatus(listing.id, sold) },
            onSuccess = { state ->
                state.copy(
                    listings = state.listings
                        .map { if (it.id == listing.id) it.copy(status = status) else it }
                        .sortedForDisplay(),
                )
            },
        )
    }

    /** Puts up the confirmation dialog. Nothing is deleted until [confirmDelete]. */
    fun askToDelete(listing: Listing) {
        uiState = uiState.copy(pendingDelete = listing)
    }

    fun dismissDelete() {
        uiState = uiState.copy(pendingDelete = null)
    }

    /** FR-LIST-009, once the dialog has been agreed to. */
    fun confirmDelete() {
        val listing = uiState.pendingDelete ?: return
        uiState = uiState.copy(pendingDelete = null)
        act(
            listing = listing,
            write = { ListingRepository.delete(listing.id) },
            onSuccess = { state ->
                state.copy(listings = state.listings.filterNot { it.id == listing.id })
            },
        )
    }

    fun consumeActionError() {
        uiState = uiState.copy(actionError = null)
    }

    /**
     * The shape all three card actions share: mark that one row busy, run the write, then either
     * fold the result into the state with [onSuccess] or surface the message - clearing busy
     * either way.
     *
     * One write at a time across the whole screen. Two overlapping writes would be harmless in
     * Firestore, but the second would land on a list the first has already rearranged.
     */
    private fun act(
        listing: Listing,
        write: suspend () -> ListingActionOutcome,
        onSuccess: (MyListingsUiState) -> MyListingsUiState,
    ) {
        if (uiState.busyId != null) return
        uiState = uiState.copy(busyId = listing.id, actionError = null)
        viewModelScope.launch {
            uiState = when (val outcome = write()) {
                is ListingActionOutcome.Success -> onSuccess(uiState).copy(busyId = null)
                is ListingActionOutcome.Failure ->
                    uiState.copy(busyId = null, actionError = outcome.message)
            }
        }
    }

    /**
     * Available listings first, sold ones after, each group still newest-first.
     *
     * Done here rather than in the query because ordering by status and then by date would need
     * a third composite index for a list that is rarely more than a handful of rows. `sortedBy`
     * is a stable sort, so the query's date ordering survives inside each group.
     */
    private fun List<Listing>.sortedForDisplay(): List<Listing> =
        sortedBy { it.status == Listing.STATUS_SOLD }
}
