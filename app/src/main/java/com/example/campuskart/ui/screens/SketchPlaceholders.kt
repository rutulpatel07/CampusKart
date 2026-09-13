package com.example.campuskart.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.ui.auth.AccountUiState
import com.example.campuskart.ui.auth.AccountViewModel
import com.example.campuskart.ui.mockups.ProfileMockup

/**
 * TEMPORARY (Day 3). The navigation graph needs something behind every destination, and only
 * Login and Signup were real screens when this was written. Rather than leaving four blank
 * "coming soon" pages, each remaining destination rendered its Day 2 mockup under a banner
 * saying so - the shell could then be reviewed and demoed as a whole app, and each sketch was
 * swapped for the real screen on the day the build plan reached it. Profile is the last one.
 *
 * Nothing in here talks to Firebase or Cloudinary, and no control inside a sketch does anything.
 * This file, and the `ui/mockups` package it draws on, are deleted once Day 8 lands. Post Item
 * was the first to go, on Day 5; the Feed and Item Detail followed on Day 6, and My items on
 * Day 7. Profile is the last one left.
 */

/** Strip pinned above a sketch so it is never mistaken for a finished screen. */
@Composable
private fun SketchBanner(
    buildDay: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Text(
                text = "Day 2 sketch - real screen arrives on $buildDay",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            )
            action?.invoke()
        }
    }
}

@Composable
private fun Sketch(
    buildDay: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SketchBanner(buildDay = buildDay, action = action)
        content()
    }
}

/**
 * Profile placeholder, and the one screen here that shows real data.
 *
 * Day 4 gave it two working controls on top of the sketch. Log out really ends the Firebase
 * session (FR-AUTH-005), and the strip below the banner shows the `users` document that signup
 * wrote - which is what makes the signup path verifiable on the device instead of in the Firebase
 * Console. The real Profile screen replaces all of this on Day 8.
 */
@Composable
fun ProfilePlaceholderRoute(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(),
) {
    ProfilePlaceholder(
        account = viewModel.uiState,
        onLogout = {
            viewModel.signOut()
            onLoggedOut()
        },
        modifier = modifier,
    )
}

@Composable
fun ProfilePlaceholder(
    account: AccountUiState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Sketch(
        buildDay = "Day 8",
        modifier = modifier,
        action = { TextButton(onClick = onLogout) { Text("Log out") } },
    ) {
        SignedInSummary(account)
        ProfileMockup(showTabBar = false)
    }
}

/** What Firebase Auth and the `users` document actually hold for whoever is signed in. */
@Composable
private fun SignedInSummary(account: AccountUiState, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "REAL DATA - signed in as ${account.email ?: "nobody"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            val profile = account.profile
            val detail = when {
                account.loading -> "Loading your profile..."
                account.error != null -> account.error
                // Only reachable if the users document was deleted by hand, since signup rolls
                // the account back when the profile write fails (see AuthRepository.signUp).
                profile == null -> "No users document found for this account."
                else -> "${profile.name} - ${profile.branch}, semester ${profile.semester} - " +
                    "+91 ${profile.whatsappNumber}"
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
