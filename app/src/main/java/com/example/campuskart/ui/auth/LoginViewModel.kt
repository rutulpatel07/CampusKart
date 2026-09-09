package com.example.campuskart.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.AuthOutcome
import com.example.campuskart.data.AuthRepository
import kotlinx.coroutines.launch

/**
 * Everything the Login screen draws. [signedIn] flips once, when the sign-in succeeds, and the
 * route wrapper watches it to navigate away.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errors: LoginErrors = LoginErrors(),
    /** A whole-form message - a rejected password, no network - shown above the button. */
    val formError: String? = null,
    val submitting: Boolean = false,
    val signedIn: Boolean = false,
) {
    /** The button is only dead while a request is in flight; empty fields are caught on submit. */
    val canSubmit: Boolean get() = !submitting
}

/**
 * Holds the Login form and runs the sign-in (FR-AUTH-004).
 *
 * The form state lives here rather than in `remember` inside the composable so that it survives a
 * rotation mid-request, and so the screen itself stays a pure function of [uiState] - which keeps
 * its @Preview working without a Firebase project behind it.
 *
 * State is a Compose `mutableStateOf` rather than a StateFlow: this screen has exactly one
 * consumer, the composable, so a flow would add a collector and a lifecycle-aware collection
 * helper without changing what is displayed.
 */
class LoginViewModel : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    /**
     * Email is trimmed as it is typed, because a leading space copied in from another app is
     * invisible in the field but makes the address a different one to Firebase.
     */
    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value.trim(),
            // Errors are cleared the moment the user starts fixing the field they refer to,
            // rather than being left on screen until the next submit.
            errors = uiState.errors.copy(email = null),
            formError = null,
        )
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(
            password = value,
            errors = uiState.errors.copy(password = null),
            formError = null,
        )
    }

    fun logIn() {
        if (uiState.submitting) return

        val errors = AuthValidation.validateLogin(uiState.email, uiState.password)
        if (!errors.isValid) {
            uiState = uiState.copy(errors = errors, formError = null)
            return
        }

        uiState = uiState.copy(errors = LoginErrors(), formError = null, submitting = true)
        viewModelScope.launch {
            uiState = when (val outcome = AuthRepository.logIn(uiState.email, uiState.password)) {
                AuthOutcome.Success -> uiState.copy(submitting = false, signedIn = true)
                is AuthOutcome.Failure ->
                    uiState.copy(submitting = false, formError = outcome.message)
            }
        }
    }
}
