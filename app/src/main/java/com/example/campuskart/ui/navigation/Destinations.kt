package com.example.campuskart.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every destination in the app, as plain string routes.
 *
 * Navigation Compose also supports type-safe routes built from `@Serializable` classes. This
 * project stays on string routes on purpose: there are eight destinations and one argument
 * between them, so the type-safe version would add the kotlinx-serialization plugin to the build
 * for no practical gain.
 */
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    const val FEED = "feed"
    const val POST = "post"
    const val MY_LISTINGS = "my_listings"
    const val PROFILE = "profile"

    /** Item Detail is pushed over the feed, so it carries the id of the listing to show. */
    const val ITEM_DETAIL = "item_detail/{listingId}"

    /** Edit Listing is pushed over My Listings, and carries the id the same way. */
    const val EDIT_LISTING = "edit_listing/{listingId}"

    /**
     * Shared by both routes above. They never appear together, and both ViewModels read the id
     * out of their own destination's SavedStateHandle - so one name is enough, and two would
     * only be two things to keep in step.
     */
    const val ARG_LISTING_ID = "listingId"

    fun itemDetail(listingId: String) = "item_detail/$listingId"

    fun editListing(listingId: String) = "edit_listing/$listingId"

}

/**
 * The four destinations on the bottom navigation bar, in bar order.
 *
 * Day 2 decision (PRD.md Section 6, README "Screen design"): CampusKart uses a persistent bottom
 * bar so every top-level screen is one tap from every other, which matters in a live demo. Item
 * Detail is not here - it is pushed over the Feed and hides the bar, because it exists to lead to
 * the single "Chat on WhatsApp" action.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    FEED(Routes.FEED, "Feed", Icons.Default.Home),
    POST(Routes.POST, "Post", Icons.Default.Add),
    MY_LISTINGS(Routes.MY_LISTINGS, "My items", Icons.AutoMirrored.Filled.List),
    PROFILE(Routes.PROFILE, "Profile", Icons.Default.Person),
}
