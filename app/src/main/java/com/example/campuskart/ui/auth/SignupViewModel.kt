package com.example.campuskart.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.AuthOutcome
import com.example.campuskart.data.AuthRepository
import com.example.campuskart.data.UserProfile
import kotlinx.coroutines.launch

/** Everything the Signup screen draws. [signedIn] flips once the account and profile both exist. */
data class SignupUiState(
    val form: SignupForm = SignupForm(),
    val errors: SignupErrors = SignupErrors(),
    val formError: String? = null,
    val submitting: Boolean = false,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = !submitting
}

/**
 * Holds the Signup form, validates it, and creates the account (FR-AUTH-001/002/003).
 *
 * The six fields travel together as one [SignupForm] rather than as six pieces of state with six
 * callbacks, because they are submitted together and half of them - name, branch, semester,
 * WhatsApp number - are the profile document itself (PRD.md Section 8).
 */
class SignupViewModel : ViewModel() {

    var uiState by mutableStateOf(SignupUiState())
        private set

    /**
     * The screen sends back the whole edited form. Which field changed is worked out here by
     * comparing against the previous value, so that only that field's error is cleared - the
     * alternative, clearing every error on any keystroke, would wipe the messages on the fields
     * the user has not fixed yet.
     */
    fun onFormChange(form: SignupForm) {
        val old = uiState.form
        uiState = uiState.copy(
            form = form,
            errors = uiState.errors.copy(
                name = uiState.errors.name.keepUnless(old.name != form.name),
                branch = uiState.errors.branch.keepUnless(old.branch != form.branch),
                semester = uiState.errors.semester.keepUnless(old.semester != form.semester),
                whatsappNumber = uiState.errors.whatsappNumber
                    .keepUnless(old.whatsappNumber != form.whatsappNumber),
                email = uiState.errors.email.keepUnless(old.email != form.email),
                password = uiState.errors.password.keepUnless(old.password != form.password),
            ),
            formError = null,
        )
    }

    private fun String?.keepUnless(changed: Boolean): String? = if (changed) null else this

    fun createAccount() {
        if (uiState.submitting) return

        val form = uiState.form
        val errors = AuthValidation.validateSignup(form)
        if (!errors.isValid) {
            uiState = uiState.copy(errors = errors, formError = null)
            return
        }

        uiState = uiState.copy(errors = SignupErrors(), formError = null, submitting = true)
        viewModelScope.launch {
            val outcome = AuthRepository.signUp(
                email = form.email.trim(),
                password = form.password,
                // uid is filled in by the repository once Firebase has issued one.
                profile = UserProfile(
                    name = form.name.trim(),
                    branch = form.branch,
                    semester = form.semester,
                    whatsappNumber = form.whatsappNumber,
                ),
            )
            uiState = when (outcome) {
                AuthOutcome.Success -> uiState.copy(submitting = false, signedIn = true)
                is AuthOutcome.Failure ->
                    uiState.copy(submitting = false, formError = outcome.message)
            }
        }
    }
}
