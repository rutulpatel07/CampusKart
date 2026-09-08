package com.example.campuskart.ui.mockups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 6 - Profile. Read-only summary of the `users` document, plus logout (FR-AUTH-005).
 *
 * The WhatsApp number is shown to its owner with a line explaining where it does and does not
 * appear, since it is the one piece of personal data the app asks for and never displays
 * publicly (FR-CONTACT-003).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMockup(modifier: Modifier = Modifier, showTabBar: Boolean = true) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Profile") }) },
        // Hidden when the Day 3 navigation shell hosts this sketch, because the shell draws
        // the real bottom bar itself - otherwise the screen would show two of them.
        bottomBar = { if (showTabBar) MockBottomBar(MockTab.PROFILE) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            InitialsAvatar(initials = MockProfile.INITIALS, size = 88)
            Spacer(Modifier.height(12.dp))
            Text(MockProfile.NAME, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = MockProfile.EMAIL,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ProfileRow("Branch", MockProfile.BRANCH)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileRow("Semester", MockProfile.SEMESTER)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileRow("WhatsApp", MockProfile.WHATSAPP)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your branch and semester appear on your listings. Your number does not - " +
                    "it is only used to open WhatsApp when a buyer taps Chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Edit profile")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {},
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Log out")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@ScreenMockupPreview
@Composable
private fun ProfileMockupPreview() {
    CampusKartTheme { ProfileMockup() }
}
