package com.example.campuskart.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.FeedOutcome
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingRepository
import kotlinx.coroutines.launch

/** Everything the Home Feed draws. */
data class HomeFeedUiState(
    val listings: List<Listing> = emptyList(),
    /** The first load, which owns the whole screen. */
    val loading: Boolean = true,
    /** A reload on top of listings already on screen - shown in the app bar, not over the list. */
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    /** Told apart from "loading" so the empty state never flashes before the first read lands. */
    val isEmpty: Boolean get() = !loading && error == null && listings.isEmpty()
}

/**
 * Loads the feed (FR-LIST-003).
 *
 * Day 7 adds the category chips and the title search on top of this (FR-LIST-004/005). Both
 * filter [HomeFeedUiState.listings] rather than re-querying, so they belong here as extra state
 * over the same loaded list - which is why the list is held whole rather than as a query result
 * that is thrown away after drawing.
 */
class HomeFeedViewModel : ViewModel() {

    var uiState by mutableStateOf(HomeFeedUiState())
        private set

    init {
        load()
    }

    /**
     * Reads the feed. [refresh] tells the two cases apart: the first load has nothing on screen
     * yet and shows a spinner in the middle of it, while a refresh has a perfectly good list
     * already drawn and should not blank it out to redraw the same rows a moment later.
     */
    fun load(refresh: Boolean = false) {
        if (uiState.loading && refresh) return
        uiState = uiState.copy(
            loading = !refresh,
            refreshing = refresh,
            error = null,
        )
        viewModelScope.launch {
            uiState = when (val outcome = ListingRepository.availableListings()) {
                is FeedOutcome.Success -> uiState.copy(
                    listings = outcome.listings,
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
}
