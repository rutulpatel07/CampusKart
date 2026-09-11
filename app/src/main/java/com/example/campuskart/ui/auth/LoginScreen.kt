package com.example.campuskart.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.ui.components.FormErrorBanner
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 1a - Login. The first screen an unauthenticated user sees: the app opens straight here,
 * with no splash and no onboarding carousel, so a live demo reaches the feed in two taps.
 *
 * Day 4 connected it to Firebase Auth (FR-AUTH-004). The screen itself stayed a pure function of
 * [LoginUiState] - it holds no state and makes no calls, so its @Preview still renders without a
 * Firebase project. [LoginRoute] below is the piece that owns the ViewModel.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenDevTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            supportingText = state.errors.email?.let { { Text(it) } },
            isError = state.errors.email != null,
            enabled = !state.submitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Password",
            supportingText = state.errors.password,
            isError = state.errors.password != null,
            enabled = !state.submitting,
        )

        // Whole-form failures - a wrong password, no network - rather than one bad field.
        FormErrorBanner(
            message = state.formError,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.submitting) {
                // Sized down and given the button's own content colour, so the button keeps its
                // height and the layout does not jump when the request starts.
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text("Log in", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(8.dp))

        AuthSwitchPrompt(
            question = "New to CampusKart?",
            action = "Create an account",
            onClick = onCreateAccount,
            enabled = !state.submitting,
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

/**
 * The stateful half: owns the [LoginViewModel] and turns a successful sign-in into navigation.
 *
 * Splitting the route from the screen is what lets the screen above stay previewable. It also
 * keeps the navigation graph free of ViewModel wiring - CampusKartApp just names a destination.
 */
@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenDevTools: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
) {
    val state = viewModel.uiState

    // Navigation is a side effect of state, not something the ViewModel does: it flips signedIn
    // and this reacts. Keyed on the flag, so it runs on the transition and not on every
    // recomposition that happens to follow it.
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::logIn,
        onCreateAccount = onCreateAccount,
        onOpenDevTools = onOpenDevTools,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun LoginScreenPreview() {
    CampusKartTheme {
        LoginScreen(
            state = LoginUiState(email = "rutul@example.com", password = "secret1"),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onCreateAccount = {},
            onOpenDevTools = {},
        )
    }
}

/** The two states that are easy to break and hard to reach by hand: a rejection, and a request. */
@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun LoginScreenErrorPreview() {
    CampusKartTheme {
        LoginScreen(
            state = LoginUiState(
                email = "rutul@example.com",
                password = "wrong",
                formError = "Email or password is incorrect.",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onCreateAccount = {},
            onOpenDevTools = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun LoginScreenSubmittingPreview() {
    CampusKartTheme {
        LoginScreen(
            state = LoginUiState(
                email = "rutul@example.com",
                password = "secret1",
                submitting = true,
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onCreateAccount = {},
            onOpenDevTools = {},
        )
    }
}
