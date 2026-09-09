package com.example.campuskart.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.AuthRepository
import com.example.campuskart.data.UserProfile
import kotlinx.coroutines.launch

/** The signed-in user, as far as the app has managed to load them. */
data class AccountUiState(
    val loading: Boolean = true,
    /** From Firebase Auth, so it is known immediately and without a network call. */
    val email: String? = null,
    /** From the `users` document, so it needs a Firestore read and can fail or be absent. */
    val profile: UserProfile? = null,
    val error: String? = null,
)

/**
 * Reads back the `users` document for whoever is signed in, and ends the session (FR-AUTH-005).
 *
 * Day 8 builds the real Profile screen on top of this. It exists already on Day 4 for a specific
 * reason: signup writes a profile document, and without reading one back the only way to confirm
 * that write actually happened is to open the Firebase Console. The temporary Profile placeholder
 * shows what this loads, so the whole signup path can be verified on the device itself.
 */
class AccountViewModel : ViewModel() {

    var uiState by mutableStateOf(AccountUiState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = AccountUiState(loading = true, email = AuthRepository.currentEmail)
        viewModelScope.launch {
            uiState = try {
                uiState.copy(loading = false, profile = AuthRepository.profile())
            } catch (e: Exception) {
                uiState.copy(loading = false, error = "Could not load your profile: ${e.message}")
            }
        }
    }

    /**
     * Clears the Firebase session. Navigation back to Login is the caller's job - this object
     * knows nothing about the navigation graph.
     */
    fun signOut() = AuthRepository.signOut()
}
