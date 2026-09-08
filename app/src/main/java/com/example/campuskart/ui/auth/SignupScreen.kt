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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campuskart.model.CampusOptions
import com.example.campuskart.ui.theme.CampusKartTheme

/** The whole signup form in one object, so Day 4 can hand it to Firebase Auth and Firestore. */
data class SignupForm(
    val name: String,
    val branch: String,
    val semester: String,
    val whatsappNumber: String,
    val email: String,
    val password: String,
)

/**
 * Screen 1b - Signup. Collects the Firebase Auth credentials and the whole `users` profile
 * document in one pass (PRD.md Section 8), because the WhatsApp number is what makes every
 * listing contactable - there is no sensible "fill this in later" state for it (FR-AUTH-003).
 *
 * Day 3 is UI and navigation only: [onCreateAccount] hands the filled form to the caller, which
 * currently just moves on to the feed. Day 4 replaces that with the Firebase Auth call, the
 * Firestore profile write, and the real per-field validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onCreateAccount: (SignupForm) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var whatsapp by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Again, not the real validation - Day 4 owns that, including the empty-WhatsApp rejection
    // in FR-AUTH-003. This only keeps the button from looking ready while fields are blank.
    val canSubmit = listOf(name, branch, semester, whatsapp, email, password)
        .all(String::isNotBlank)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
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
                    value = branch,
                    options = CampusOptions.BRANCHES,
                    onOptionSelected = { branch = it },
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Semester",
                    value = semester,
                    options = CampusOptions.SEMESTERS,
                    onOptionSelected = { semester = it },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { input -> whatsapp = normalizeIndianMobile(input) },
                label = { Text("WhatsApp number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                prefix = { Text("+91 ") },
                supportingText = { Text("Required - this is how buyers reach you") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Login details")

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                supportingText = { Text("Any email works - no college domain needed") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                supportingText = "At least 6 characters",
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    onCreateAccount(
                        SignupForm(
                            name = name.trim(),
                            branch = branch,
                            semester = semester,
                            whatsappNumber = whatsapp,
                            email = email,
                            password = password,
                        ),
                    )
                },
                enabled = canSubmit,
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

@Preview(showBackground = true, widthDp = 400, heightDp = 880)
@Composable
private fun SignupScreenPreview() {
    CampusKartTheme { SignupScreen(onCreateAccount = {}, onBack = {}) }
}
