package com.example.campuskart.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campuskart.data.AuthRepository
import com.example.campuskart.ui.auth.LoginRoute
import com.example.campuskart.ui.auth.SignupRoute
import com.example.campuskart.ui.detail.ItemDetailRoute
import com.example.campuskart.ui.feed.HomeFeedRoute
import com.example.campuskart.ui.mylistings.EditListingRoute
import com.example.campuskart.ui.mylistings.MyListingsRoute
import com.example.campuskart.ui.post.PostItemRoute
import com.example.campuskart.ui.profile.ProfileRoute

/**
 * The whole app: one navigation graph, plus the bottom bar that appears on the four top-level
 * destinations (see [TopLevelDestination]).
 *
 * The graph is deliberately flat rather than split into nested "auth" and "main" sub-graphs. With
 * seven destinations, nesting would buy separate back stacks the app has no use for and make the
 * one thing that does matter - Login and the feed being on the same stack, so signing out really
 * clears it - harder to read.
 *
 * All eight destinations are real screens. Day 9 added ML Kit category suggestion to Post Item;
 * Day 10 removed the scaffolding (mockup gallery and setup check).
 */
@Composable
fun CampusKartApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Firebase keeps a signed-in session on disk across app restarts, so a returning user should
    // not be asked to log in again - the graph simply starts on the Feed instead of on Login.
    //
    // remember matters here. Without it, signing out would flip this to Login and swap the
    // NavHost's start destination underneath a graph that is mid-navigation; the start
    // destination is fixed for the life of the composition, and signOut() below does the moving.
    val startDestination = remember {
        if (AuthRepository.isSignedIn()) Routes.FEED else Routes.LOGIN
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Item Detail is excluded on purpose: it is pushed over the Feed and hides the bar, because
    // it exists to lead to the single "Chat on WhatsApp" action (README, "Screen design").
    val bottomBarTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (bottomBarTab != null) {
                CampusKartBottomBar(
                    selected = bottomBarTab,
                    onSelect = { navController.switchTab(it) },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // consumeWindowInsets: without this, nested Scaffolds inside tab destinations
            // double-count the status bar inset.
            modifier = Modifier
                .padding(inner)
                .consumeWindowInsets(inner)
                .fillMaxSize(),
        ) {
            composable(Routes.LOGIN) {
                LoginRoute(
                    onSignedIn = { navController.enterApp() },
                    onCreateAccount = { navController.navigate(Routes.SIGNUP) },
                )
            }

            composable(Routes.SIGNUP) {
                SignupRoute(
                    onSignedIn = { navController.enterApp() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.FEED) {
                HomeFeedRoute(
                    onOpenListing = { navController.navigate(Routes.itemDetail(it)) },
                    // The empty feed's only call to action. switchTab rather than a plain
                    // navigate, so it lands on the Post tab properly - with the bottom bar
                    // showing Post as selected - instead of stacking Post on top of the Feed.
                    onPostItem = { navController.switchTab(TopLevelDestination.POST) },
                )
            }

            composable(Routes.POST) {
                PostItemRoute(
                    onGoToFeed = { navController.switchTab(TopLevelDestination.FEED) },
                )
            }

            composable(Routes.MY_LISTINGS) {
                MyListingsRoute(
                    // Item Detail is reached from here as well as from the Feed, so a seller can
                    // see their own listing exactly as a buyer does.
                    onOpenListing = { navController.navigate(Routes.itemDetail(it)) },
                    onEdit = { navController.navigate(Routes.editListing(it)) },
                    onPostItem = { navController.switchTab(TopLevelDestination.POST) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileRoute(onLoggedOut = { navController.signOut() })
            }

            composable(
                route = Routes.ITEM_DETAIL,
                arguments = listOf(
                    navArgument(Routes.ARG_LISTING_ID) { type = NavType.StringType },
                ),
            ) {
                // listingId is not read here: Navigation puts the route arguments into the
                // destination's SavedStateHandle, and ItemDetailViewModel takes it from there.
                ItemDetailRoute(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.EDIT_LISTING,
                arguments = listOf(
                    navArgument(Routes.ARG_LISTING_ID) { type = NavType.StringType },
                ),
            ) {
                // One callback for both leaving and finishing: a save pops back to My Listings,
                // which is the same thing the back arrow does. The list reloads as it resumes,
                // so the edited card is already updated when it appears.
                EditListingRoute(onDone = { navController.popBackStack() })
            }

        }
    }
}

@Composable
private fun CampusKartBottomBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * Standard bottom-bar behaviour: tapping a tab pops back to the Feed rather than stacking tabs on
 * top of each other, and re-tapping the current tab does nothing. Each tab keeps its own scroll
 * position and form state via `restoreState`.
 */
private fun NavHostController.switchTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        // Popped back to the Feed rather than to the graph's start destination, which is Login -
        // enterApp() has already dropped Login off the stack, so popping to it would match
        // nothing and every tab tap would pile another entry on instead.
        popUpTo(Routes.FEED) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Login and Signup both land on the Feed, and both drop the auth screens from the back stack so
 * the system back button exits the app instead of walking back into a login form the user has
 * already passed.
 */
private fun NavHostController.enterApp() {
    navigate(Routes.FEED) {
        popUpTo(Routes.LOGIN) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * The reverse: clear everything and go back to Login. The Firebase session is already gone by the
 * time this runs - popping the whole graph is what stops the back button from walking into the
 * feed of an account that is no longer signed in.
 */
private fun NavHostController.signOut() {
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
