package com.example.campuskart.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** The result of a sign-up or log-in attempt. [Failure.message] is written to be shown as-is. */
sealed interface AuthOutcome {
    data object Success : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/**
 * Everything CampusKart does with Firebase Auth and the `users` collection.
 *
 * A plain object rather than an injected class: the app has one auth backend, no tests that swap
 * it out, and no DI framework in the build. The suspend functions below are the only place the
 * Firebase SDK is touched - screens and ViewModels never see a Task, a FirebaseUser, or a
 * Firebase exception type.
 *
 * `FirebaseAuth.getInstance()` is resolved on each call rather than held in a property, because
 * this object can be constructed before FirebaseApp has been initialised by its content
 * provider, and a field initialiser would then throw during class loading.
 */
object AuthRepository {

    private const val TAG = "CampusKartAuth"
    private const val USERS = "users"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /**
     * Whether a user is already signed in from a previous run. Firebase persists the session to
     * disk, so this survives the app being killed - which is why the navigation graph opens on
     * the Feed rather than on Login when it returns true (see CampusKartApp).
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /** The signed-in user's UID, or null. Day 5 stamps this onto every listing as `sellerUid`. */
    val currentUid: String? get() = auth.currentUser?.uid

    /** The signed-in user's login email. Not part of the profile document - Auth already has it. */
    val currentEmail: String? get() = auth.currentUser?.email

    /**
     * Creates the Auth account and writes the matching `users` document (FR-AUTH-001/002).
     *
     * The two have to succeed together. `createUserWithEmailAndPassword` also signs the new user
     * in, so if the Firestore write then fails the app would be left holding a session for an
     * account with no name, branch or WhatsApp number - and every listing that account posted
     * would be uncontactable, which is the one thing the app cannot tolerate (FR-CONTACT-001).
     *
     * So a failed profile write rolls the account back: the Auth user is deleted and the session
     * cleared, leaving the email address free. Without that, retrying signup would hit
     * "email already in use" against an account the user cannot log into meaningfully.
     */
    suspend fun signUp(email: String, password: String, profile: UserProfile): AuthOutcome {
        val created = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Account creation failed", e)
            return AuthOutcome.Failure(signUpErrorMessage(e))
        }

        val uid = created.user?.uid
            ?: return AuthOutcome.Failure("Signup did not complete. Please try again.")

        return try {
            firestore.collection(USERS).document(uid)
                .set(profile.copy(uid = uid))
                .await()
            AuthOutcome.Success
        } catch (e: CancellationException) {
            // The coroutine was cancelled (the screen left the composition) rather than the write
            // failing. Roll back anyway - nobody is left to retry, and a half-made account is
            // worse than none - then let the cancellation continue to propagate.
            rollBack()
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Profile write failed, rolling the account back", e)
            rollBack()
            AuthOutcome.Failure(
                "Your account could not be saved. Check your connection and try again.",
            )
        }
    }

    /** Signs an existing user in (FR-AUTH-004). */
    suspend fun logIn(email: String, password: String): AuthOutcome = try {
        auth.signInWithEmailAndPassword(email, password).await()
        AuthOutcome.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Sign-in failed", e)
        AuthOutcome.Failure(logInErrorMessage(e))
    }

    /** Ends the session (FR-AUTH-005). Local only - there is no network call to fail. */
    fun signOut() = auth.signOut()

    /**
     * Reads a `users` document. Returns null when nobody is signed in, and throws if Firestore
     * is unreachable so the caller can tell "no profile" apart from "could not load it".
     */
    suspend fun profile(uid: String? = currentUid): UserProfile? {
        val id = uid ?: return null
        return firestore.collection(USERS).document(id).get().await()
            .toObject(UserProfile::class.java)
    }

    /** Best-effort undo of a half-completed signup. Failures here are logged, not surfaced. */
    private suspend fun rollBack() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (e: Exception) {
            // Leaves an orphaned Auth account. Rare, and it only costs the user the ability to
            // reuse that email; signing out below still puts them back on a clean Login screen.
            Log.e(TAG, "Could not delete the account after a failed profile write", e)
        }
        auth.signOut()
    }

    /**
     * Firebase's own exception messages are written for developers ("The email address is badly
     * formatted", with a stack trace behind it). These are the versions a student sees.
     */
    private fun signUpErrorMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException ->
            "An account already uses this email. Try logging in instead."

        is FirebaseAuthWeakPasswordException ->
            "That password is too weak. Use at least 6 characters."

        is FirebaseAuthInvalidCredentialsException ->
            "That email address does not look valid."

        else -> genericErrorMessage(e)
    }

    private fun logInErrorMessage(e: Exception): String = when (e) {
        // Firebase deliberately returns the same error for "no such user" and "wrong password"
        // so that the sign-in form cannot be used to discover which emails have accounts, and
        // the message here has to stay just as vague to keep that true.
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
            "Email or password is incorrect."

        else -> genericErrorMessage(e)
    }

    private fun genericErrorMessage(e: Exception): String = when (e) {
        is FirebaseNetworkException ->
            "No internet connection. Check your network and try again."

        is FirebaseTooManyRequestsException ->
            "Too many attempts. Wait a minute and try again."

        else -> "Something went wrong. Please try again."
    }
}
