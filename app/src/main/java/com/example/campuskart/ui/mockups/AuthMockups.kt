package com.example.campuskart.ui.mockups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

private const val DOTS_LONG = "••••••••••"
private const val DOTS_SHORT = "••••••••"

/**
 * Screen 1a - Login. The first screen an unauthenticated user sees; the app opens straight here,
 * with no splash and no onboarding carousel, so the live demo reaches the feed in two taps.
 */
@Composable
fun LoginMockup(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(96.dp))

        Text(
            text = "CampusKart",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Buy and sell used academic items\nwithin your campus",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = MockProfile.EMAIL,
            onValueChange = {},
            readOnly = true,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = DOTS_LONG,
            onValueChange = {},
            readOnly = true,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = { TextButton(onClick = {}) { Text("Show") } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Log in", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "New to CampusKart?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {}) { Text("Create an account") }
        }
    }
}

/**
 * Screen 1b - Signup. Collects the Firebase Auth credentials and the whole `users` profile
 * document in one pass (PRD.md Section 8), because the WhatsApp number is what makes every
 * listing contactable - there is no sensible "fill this in later" state for it (FR-AUTH-003).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupMockup(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("About you")

            OutlinedTextField(
                value = MockProfile.NAME,
                onValueChange = {},
                readOnly = true,
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Branch and semester sit side by side: both are short self-declared values, and
            // pairing them keeps the form from reading like a long questionnaire.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownLookField(
                    label = "Branch",
                    value = MockProfile.BRANCH,
                    modifier = Modifier.weight(1f),
                )
                DropdownLookField(
                    label = "Semester",
                    value = MockProfile.SEMESTER,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = "98765 43210",
                onValueChange = {},
                readOnly = true,
                label = { Text("WhatsApp number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                prefix = { Text("+91 ") },
                supportingText = { Text("Required - this is how buyers reach you") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Login details")

            OutlinedTextField(
                value = MockProfile.EMAIL,
                onValueChange = {},
                readOnly = true,
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                supportingText = { Text("Any email works - no college domain needed") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = DOTS_SHORT,
                onValueChange = {},
                readOnly = true,
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = { TextButton(onClick = {}) { Text("Show") } },
                supportingText = { Text("At least 6 characters") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Create account", style = MaterialTheme.typography.titleMedium)
            }

            // States FR-CONTACT-003 at the moment the number is asked for, rather than burying
            // it in a privacy policy the app does not have.
            Text(
                text = "Your number is never shown on listings. It is only used to open " +
                    "WhatsApp when a buyer taps Chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** A read-only field that reads as a dropdown. The real menu is wired up on Day 3. */
@Composable
private fun DropdownLookField(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        singleLine = true,
        modifier = modifier,
    )
}

@ScreenMockupPreview
@Composable
private fun LoginMockupPreview() {
    CampusKartTheme { LoginMockup() }
}

@ScreenMockupPreview
@Composable
private fun SignupMockupPreview() {
    CampusKartTheme { SignupMockup() }
}
