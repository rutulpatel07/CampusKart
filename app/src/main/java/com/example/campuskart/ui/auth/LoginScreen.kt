package com.example.campuskart.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 1a - Login. The first screen an unauthenticated user sees: the app opens straight here,
 * with no splash and no onboarding carousel, so a live demo reaches the feed in two taps.
 *
 * Day 3 builds the UI and the navigation only. [onLogin] currently just moves to the feed - the
 * Firebase Auth sign-in behind it lands on Day 4 (FR-AUTH-004), at which point this screen also
 * grows a loading state and an error message. The fields are already hoisted out through
 * [onLogin] so that wiring is a change in the caller, not a rewrite of this file.
 */
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onCreateAccount: () -> Unit,
    onOpenDevTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Not validation - that is Day 4's job (FR-AUTH-003 and friends). This only stops the button
    // from looking tappable while the form is visibly empty.
    val canSubmit = email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
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
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onLogin(email, password) },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Log in", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(8.dp))

        AuthSwitchPrompt(
            question = "New to CampusKart?",
            action = "Create an account",
            onClick = onCreateAccount,
        )

        // A weight() spacer would push this to the bottom of the screen, but a weight inside a
        // verticalScroll column measures against an infinite height and crashes. Fixed gap it is.
        Spacer(Modifier.height(64.dp))

        // Scaffolding, removed on Day 10: the Day 1 service check and the Day 2 mockups are no
        // longer the launch screen, and this is the only way back to them while they are still
        // useful for debugging a Firebase or Cloudinary problem.
        TextButton(onClick = onOpenDevTools) {
            Text(
                text = "Dev tools",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun LoginScreenPreview() {
    CampusKartTheme {
        LoginScreen(onLogin = { _, _ -> }, onCreateAccount = {}, onOpenDevTools = {})
    }
}
