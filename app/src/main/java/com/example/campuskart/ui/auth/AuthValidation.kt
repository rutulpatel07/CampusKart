package com.example.campuskart.ui.auth

/** The whole signup form in one object, so it can be validated and submitted as a unit. */
data class SignupForm(
    val name: String = "",
    val branch: String = "",
    val semester: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val password: String = "",
)

/** Per-field messages for the signup form. A null field means that field is fine. */
data class SignupErrors(
    val name: String? = null,
    val branch: String? = null,
    val semester: String? = null,
    val whatsappNumber: String? = null,
    val email: String? = null,
    val password: String? = null,
) {
    val isValid: Boolean
        get() = listOf(name, branch, semester, whatsappNumber, email, password).all { it == null }
}

/** Per-field messages for the login form. */
data class LoginErrors(
    val email: String? = null,
    val password: String? = null,
) {
    val isValid: Boolean get() = email == null && password == null
}

/**
 * Form rules for Login and Signup, checked on the device before anything is sent to Firebase.
 *
 * Deliberately plain Kotlin with no Android or Firebase imports, so the rules can be covered by
 * ordinary JVM unit tests (see AuthValidationTest) rather than needing an emulator.
 *
 * These checks are not a security boundary - Firebase re-validates the email and password server
 * side, and Day 8's Firestore rules do the same for data. They exist so the user is told what is
 * wrong immediately, in the field that is wrong, instead of waiting for a network round trip to
 * come back with one generic error.
 */
object AuthValidation {

    /** Firebase Auth rejects anything shorter, so matching it here avoids a pointless round trip. */
    const val MIN_PASSWORD_LENGTH = 6

    /**
     * Something before an @, something after it, and a dot in the domain. Intentionally loose:
     * signup is open to any email address (PRD.md Section 6), so this only catches obvious typos
     * like a missing @ or a trailing "gmail.com " - Firebase makes the final call.
     *
     * `android.util.Patterns.EMAIL_ADDRESS` would be the usual choice and is stricter, but it is
     * an Android framework class that returns null in a plain JVM test, which would leave the
     * most-used rule in this file untestable without an emulator.
     */
    private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /** Indian mobile numbers are ten digits and start with 6, 7, 8 or 9. */
    private val INDIAN_MOBILE = Regex("^[6-9][0-9]{9}$")

    fun validateLogin(email: String, password: String) = LoginErrors(
        email = emailError(email),
        // No length rule here. An account created before any rule change might have a shorter
        // password, and telling a returning user their own password is "too short" when it is
        // simply mistyped would be actively misleading - Firebase answers that question.
        password = "Enter your password".takeIf { password.isEmpty() },
    )

    fun validateSignup(form: SignupForm) = SignupErrors(
        name = "Enter your name".takeIf { form.name.isBlank() },
        branch = "Pick your branch".takeIf { form.branch.isBlank() },
        semester = "Pick your semester".takeIf { form.semester.isBlank() },
        whatsappNumber = whatsappError(form.whatsappNumber),
        email = emailError(form.email),
        password = passwordError(form.password),
    )

    private fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email"
        !EMAIL_SHAPE.matches(email.trim()) -> "That does not look like an email address"
        else -> null
    }

    private fun passwordError(password: String): String? = when {
        password.isEmpty() -> "Choose a password"
        password.length < MIN_PASSWORD_LENGTH -> "At least $MIN_PASSWORD_LENGTH characters"
        else -> null
    }

    /**
     * FR-AUTH-003 only requires that the number is not empty, but an empty number and a wrong
     * number fail the same way: the "Chat on WhatsApp" button on every listing that user posts
     * opens a conversation with nobody. Since the field already normalises what is typed to ten
     * national digits (see SignupScreen), checking the shape costs nothing and catches the
     * common slip of leaving a digit off.
     */
    private fun whatsappError(number: String): String? = when {
        number.isBlank() -> "WhatsApp number is required - buyers contact you here"
        !INDIAN_MOBILE.matches(number) -> "Enter a 10-digit mobile number"
        else -> null
    }
}
