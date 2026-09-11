package com.example.campuskart.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.model.CampusOptions
import com.example.campuskart.ui.components.FormErrorBanner
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 1b - Signup. Collects the Firebase Auth credentials and the whole `users` profile
 * document in one pass (PRD.md Section 8), because the WhatsApp number is what makes every
 * listing contactable - there is no sensible "fill this in later" state for it (FR-AUTH-003).
 *
 * Day 4 connected it: submitting now creates the Auth account and writes the profile document,
 * and the two succeed or fail together (see AuthRepository.signUp). The screen stays a pure
 * function of [SignupUiState]; [SignupRoute] below owns the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    state: SignupUiState,
    onFormChange: (SignupForm) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val errors = state.errors
    val enabled = !state.submitting

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    // Disabled mid-request: leaving the screen while the account is being created
                    // cancels the coroutine, and the repository then has to roll the half-made
                    // account back. Blocking the exit for the second or two it takes is kinder.
                    IconButton(onClick = onBack, enabled = enabled) {
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
                .imePadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("About you")

            OutlinedTextField(
                value = form.name,
                onValueChange = { onFormChange(form.copy(name = it)) },
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                supportingText = errors.name?.let { { Text(it) } },
                isError = errors.name != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Branch and semester sit side by side: both are short values picked from a list, and
            // pairing them keeps the form from reading like a long questionnaire.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    label = "Branch",
                    value = form.branch,
                    options = CampusOptions.BRANCHES,
                    onOptionSelected = { onFormChange(form.copy(branch = it)) },
                    supportingText = errors.branch,
                    isError = errors.branch != null,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Semester",
                    value = form.semester,
                    options = CampusOptions.SEMESTERS,
                    onOptionSelected = { onFormChange(form.copy(semester = it)) },
                    supportingText = errors.semester,
                    isError = errors.semester != null,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = form.whatsappNumber,
                onValueChange = {
                    onFormChange(form.copy(whatsappNumber = normalizeIndianMobile(it)))
                },
                label = { Text("WhatsApp number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                prefix = { Text("+91 ") },
                supportingText = {
                    Text(errors.whatsappNumber ?: "Required - this is how buyers reach you")
                },
                isError = errors.whatsappNumber != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Login details")

            OutlinedTextField(
                value = form.email,
                onValueChange = { onFormChange(form.copy(email = it.trim())) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                supportingText = {
                    Text(errors.email ?: "Any email works - no college domain needed")
                },
                isError = errors.email != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordField(
                value = form.password,
                onValueChange = { onFormChange(form.copy(password = it)) },
                label = "Password",
                supportingText = errors.password
                    ?: "At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters",
                isError = errors.password != null,
                enabled = enabled,
            )

            FormErrorBanner(message = state.formError, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text("Create account", style = MaterialTheme.typography.titleMedium)
                }
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

/** The stateful half: owns the [SignupViewModel] and navigates on a completed signup. */
@Composable
fun SignupRoute(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = viewModel(),
) {
    val state = viewModel.uiState

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    SignupScreen(
        state = state,
        onFormChange = viewModel::onFormChange,
        onSubmit = viewModel::createAccount,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Reduces whatever the user typed or pasted to the ten digits that go after the field's fixed
 * "+91 " prefix.
 *
 * Everything that is not a digit goes first, so "98765 43210" and "+91-98765-43210" both work.
 * The country code then has to go too: someone pasting their own number from WhatsApp pastes it
 * with the 91 already on the front, and simply truncating to ten digits would turn
 * "+91 98765 43210" into 9198765432 - a different, silently wrong number. A leading 91 is only
 * dropped when there are more than ten digits, so a real number that happens to start with 91
 * survives untouched.
 */
private fun normalizeIndianMobile(input: String): String {
    val digits = input.filter(Char::isDigit)
    val national = if (digits.length > 10 && digits.startsWith("91")) digits.drop(2) else digits
    return national.take(10)
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

private val filledForm = SignupForm(
    name = "Rutul Patel",
    branch = "CE",
    semester = "5",
    whatsappNumber = "9876543210",
    email = "rutul@example.com",
    password = "secret1",
)

@Preview(showBackground = true, widthDp = 400, heightDp = 1100)
@Composable
private fun SignupScreenPreview() {
    CampusKartTheme {
        SignupScreen(
            state = SignupUiState(form = filledForm),
            onFormChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}

/** Every field failing at once - the layout that is hardest to get right and rarest to hit. */
@Preview(showBackground = true, widthDp = 400, heightDp = 1100)
@Composable
private fun SignupScreenErrorPreview() {
    CampusKartTheme {
        SignupScreen(
            state = SignupUiState(
                form = SignupForm(email = "not-an-email", password = "abc"),
                errors = AuthValidation.validateSignup(
                    SignupForm(email = "not-an-email", password = "abc"),
                ),
                formError = "An account already uses this email. Try logging in instead.",
            ),
            onFormChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
