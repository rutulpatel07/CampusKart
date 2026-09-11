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
import com.example.campuskart.ui.mockups.MockupGallery
import com.example.campuskart.ui.post.PostItemRoute
import com.example.campuskart.ui.screens.FeedPlaceholder
import com.example.campuskart.ui.screens.ItemDetailPlaceholder
import com.example.campuskart.ui.screens.MyListingsPlaceholder
import com.example.campuskart.ui.screens.ProfilePlaceholderRoute

/**
 * The whole app: one navigation graph, plus the bottom bar that appears on the four top-level
 * destinations (see [TopLevelDestination]).
 *
 * The graph is deliberately flat rather than split into nested "auth" and "main" sub-graphs. With
 * seven destinations, nesting would buy separate back stacks the app has no use for and make the
 * one thing that does matter - Login and the feed being on the same stack, so signing out really
 * clears it - harder to read.
 *
 * Day 4 put Firebase Auth behind Login, Signup and Log out, and Day 5 replaced the Post tab's
 * sketch with the real screen. Feed, Item Detail, My items and Profile are still Day 2 sketches
 * until Days 6-8.
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
            // consumeWindowInsets matters here: the mockup-backed placeholders each carry their
            // own Scaffold and TopAppBar, and those add status-bar padding of their own unless
            // they are told the outer Scaffold has already applied it. Without this the sketch
            // screens sit a status bar's height too low.
            modifier = Modifier
                .padding(inner)
                .consumeWindowInsets(inner)
                .fillMaxSize(),
        ) {
            composable(Routes.LOGIN) {
                LoginRoute(
                    onSignedIn = { navController.enterApp() },
                    onCreateAccount = { navController.navigate(Routes.SIGNUP) },
                    onOpenDevTools = { navController.navigate(Routes.DEV_TOOLS) },
                )
            }

            composable(Routes.SIGNUP) {
                SignupRoute(
                    onSignedIn = { navController.enterApp() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.FEED) {
                FeedPlaceholder(
                    onOpenSampleItem = { navController.navigate(Routes.itemDetail("sample")) },
                )
            }

            composable(Routes.POST) { PostItemRoute() }

            composable(Routes.MY_LISTINGS) { MyListingsPlaceholder() }

            composable(Routes.PROFILE) {
                // The route clears the Firebase session itself (FR-AUTH-005); this only has to
                // deal with the back stack afterwards.
                ProfilePlaceholderRoute(onLoggedOut = { navController.signOut() })
            }

            composable(
                route = Routes.ITEM_DETAIL,
                arguments = listOf(
                    navArgument(Routes.ARG_LISTING_ID) { type = NavType.StringType },
                ),
            ) { entry ->
                ItemDetailPlaceholder(
                    listingId = entry.arguments?.getString(Routes.ARG_LISTING_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }

            // Scaffolding, removed on Day 10 with the packages it opens.
            composable(Routes.DEV_TOOLS) { MockupGallery() }
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
