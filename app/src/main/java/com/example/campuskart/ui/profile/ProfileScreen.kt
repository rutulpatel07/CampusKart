package com.example.campuskart.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.data.UserProfile
import com.example.campuskart.ui.auth.AccountUiState
import com.example.campuskart.ui.auth.AccountViewModel
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 6 - the signed-in student's profile (FR-AUTH-005).
 *
 * Profile editing is intentionally left out of this day: it is optional in the build plan, while
 * showing the data that is actually used for listings and moving logout to its permanent home are
 * the required work. This makes the security rules and the user-facing screen agree exactly.
 */
@Composable
fun ProfileRoute(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(),
) {
    ProfileScreen(
        state = viewModel.uiState,
        onRetry = viewModel::load,
        onLogout = {
            viewModel.signOut()
            onLoggedOut()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: AccountUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> ProfileMessage(
                    title = "Could not load your profile",
                    detail = state.error,
                    actionLabel = "Try again",
                    actionIcon = Icons.Default.Refresh,
                    onAction = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.profile == null -> ProfileMessage(
                    title = "Your profile is unavailable",
                    detail = "This account has no matching profile document. Log out and sign in " +
                        "again, or check the users collection in Firestore.",
                    actionLabel = "Try again",
                    actionIcon = Icons.Default.Refresh,
                    onAction = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> ProfileContent(
                    profile = state.profile,
                    email = state.email.orEmpty(),
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    email: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Your CampusKart account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "These details identify you to students viewing your listings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileField("Name", profile.name)
                ProfileField("Email", email)
                ProfileField("Branch", profile.branch)
                ProfileField("Semester", "Semester ${profile.semester}")
                ProfileField("WhatsApp number", "+91 ${profile.whatsappNumber}")
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Log out")
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProfileMessage(
    title: String,
    detail: String,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(20.dp))
        Button(onClick = onAction) {
            Icon(actionIcon, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(actionLabel)
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ProfileScreenPreview() {
    CampusKartTheme {
        ProfileScreen(
            state = AccountUiState(
                loading = false,
                email = "rutul@example.com",
                profile = UserProfile(
                    uid = "preview",
                    name = "Rutul Patel",
                    branch = "CE",
                    semester = "5",
                    whatsappNumber = "9876543210",
                ),
            ),
            onRetry = {},
            onLogout = {},
        )
    }
}
