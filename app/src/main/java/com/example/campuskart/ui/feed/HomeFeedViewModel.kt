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
    /** Every available listing, as loaded. The filters narrow this rather than replacing it. */
    val listings: List<Listing> = emptyList(),
    /** What is typed in the search box (FR-LIST-005). */
    val query: String = "",
    /** The selected category chip, or [FeedFilter.ALL_CATEGORIES] (FR-LIST-004). */
    val category: String = FeedFilter.ALL_CATEGORIES,
    /** The first load, which owns the whole screen. */
    val loading: Boolean = true,
    /** A reload on top of listings already on screen - shown in the app bar, not over the list. */
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    /**
     * The rows actually drawn.
     *
     * Computed in the constructor rather than in a `get()`, so it runs once per state change
     * instead of once per recomposition - and, because this is a data class, `copy()` recomputes
     * it automatically whenever the query or the category changes.
     */
    val visible: List<Listing> = FeedFilter.apply(listings, query, category)

    /** Whether either filter is narrowing anything - drives the "Clear filters" affordance. */
    val filtering: Boolean get() = query.isNotBlank() || category != FeedFilter.ALL_CATEGORIES

    /** Told apart from "loading" so the empty state never flashes before the first read lands. */
    val isEmpty: Boolean get() = !loading && error == null && listings.isEmpty()

    /**
     * Nothing survived the filters, but the feed itself is not empty.
     *
     * Kept distinct from [isEmpty] because the two need opposite messages: "nobody has posted
     * anything, be the first" is wrong and slightly insulting when there are thirty listings and
     * the user simply mistyped a search term.
     */
    val hasNoMatches: Boolean get() = !isEmpty && !loading && error == null && visible.isEmpty()
}

/**
 * Loads the feed (FR-LIST-003) and holds the two filters over it (FR-LIST-004/005).
 *
 * The filters are state, not queries: [setQuery] and [setCategory] change what is drawn without
 * touching Firestore, which is why the loaded list is held whole rather than thrown away after
 * drawing. See [FeedFilter] for why that is the only option for search, and a deliberate choice
 * for category.
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

    /** FR-LIST-005. Filtering is instant, so there is no debounce and nothing to cancel. */
    fun setQuery(query: String) {
        uiState = uiState.copy(query = query)
    }

    /**
     * FR-LIST-004. Tapping the selected chip again clears it, matching how the chips behave on
     * Post Item - and giving the "All" chip a second, more discoverable way to be reached.
     */
    fun setCategory(category: String) {
        uiState = uiState.copy(
            category = if (category == uiState.category) FeedFilter.ALL_CATEGORIES else category,
        )
    }

    /** The one control on the "nothing matched" state: drop both filters at once. */
    fun clearFilters() {
        uiState = uiState.copy(query = "", category = FeedFilter.ALL_CATEGORIES)
    }
}
