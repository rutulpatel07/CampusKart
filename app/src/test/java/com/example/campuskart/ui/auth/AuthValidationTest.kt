package com.example.campuskart.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the signup and login form rules.
 *
 * These run on the JVM with no emulator and no Firebase project, which is the whole reason
 * AuthValidation avoids Android and Firebase imports. The Firebase calls themselves are not
 * covered here - they need a real backend and are checked by hand against the running app.
 *
 * FR-AUTH-003 (signup must reject an empty WhatsApp number) is the requirement most worth
 * pinning down in a test: it is the one rule the app cannot afford to lose, because a listing
 * from a user with no number cannot be contacted at all.
 */
class AuthValidationTest {

    private val validForm = SignupForm(
        name = "Rutul Patel",
        branch = "CE",
        semester = "5",
        whatsappNumber = "9876543210",
        email = "rutul@example.com",
        password = "secret1",
    )

    @Test
    fun `a fully filled form passes`() {
        assertTrue(AuthValidation.validateSignup(validForm).isValid)
    }

    // --- FR-AUTH-003 -------------------------------------------------------------------------

    @Test
    fun `signup is rejected when the WhatsApp number is empty`() {
        val errors = AuthValidation.validateSignup(validForm.copy(whatsappNumber = ""))

        assertNotNull(errors.whatsappNumber)
        assertTrue(!errors.isValid)
    }

    @Test
    fun `signup is rejected when the WhatsApp number is only whitespace`() {
        assertNotNull(AuthValidation.validateSignup(validForm.copy(whatsappNumber = "   ")).whatsappNumber)
    }

    @Test
    fun `signup is rejected when the WhatsApp number is short`() {
        assertNotNull(AuthValidation.validateSignup(validForm.copy(whatsappNumber = "98765")).whatsappNumber)
    }

    @Test
    fun `signup is rejected when the WhatsApp number cannot be an Indian mobile`() {
        // Ten digits, but no Indian mobile number starts with 1.
        assertNotNull(AuthValidation.validateSignup(validForm.copy(whatsappNumber = "1234567890")).whatsappNumber)
    }

    @Test
    fun `every valid Indian mobile prefix is accepted`() {
        listOf("6", "7", "8", "9").forEach { prefix ->
            val number = prefix + "876543210"
            assertNull(
                "$number should be accepted",
                AuthValidation.validateSignup(validForm.copy(whatsappNumber = number)).whatsappNumber,
            )
        }
    }

    // --- FR-AUTH-002: the profile fields are all required ------------------------------------

    @Test
    fun `name branch and semester are each required`() {
        assertNotNull(AuthValidation.validateSignup(validForm.copy(name = "")).name)
        assertNotNull(AuthValidation.validateSignup(validForm.copy(branch = "")).branch)
        assertNotNull(AuthValidation.validateSignup(validForm.copy(semester = "")).semester)
    }

    @Test
    fun `only the field that is wrong is flagged`() {
        val errors = AuthValidation.validateSignup(validForm.copy(name = ""))

        assertNotNull(errors.name)
        assertNull(errors.branch)
        assertNull(errors.semester)
        assertNull(errors.whatsappNumber)
        assertNull(errors.email)
        assertNull(errors.password)
    }

    // --- Email and password ------------------------------------------------------------------

    @Test
    fun `obviously malformed emails are rejected`() {
        listOf("", "rutul", "rutul@", "@example.com", "rutul@example", "rut ul@example.com")
            .forEach { email ->
                assertNotNull(
                    "$email should be rejected",
                    AuthValidation.validateSignup(validForm.copy(email = email)).email,
                )
            }
    }

    @Test
    fun `ordinary student emails are accepted`() {
        listOf(
            "rutul@gmail.com",
            "rutul.patel@ganpatuniversity.ac.in",
            "22ce001+kart@example.co.uk",
        ).forEach { email ->
            assertNull(
                "$email should be accepted",
                AuthValidation.validateSignup(validForm.copy(email = email)).email,
            )
        }
    }

    @Test
    fun `passwords shorter than Firebase's minimum are rejected before the network call`() {
        val short = "x".repeat(AuthValidation.MIN_PASSWORD_LENGTH - 1)
        assertNotNull(AuthValidation.validateSignup(validForm.copy(password = short)).password)

        val exact = "x".repeat(AuthValidation.MIN_PASSWORD_LENGTH)
        assertNull(AuthValidation.validateSignup(validForm.copy(password = exact)).password)
    }

    // --- Login --------------------------------------------------------------------------------

    @Test
    fun `login needs an email and a password`() {
        assertTrue(AuthValidation.validateLogin("rutul@example.com", "secret1").isValid)
        assertNotNull(AuthValidation.validateLogin("", "secret1").email)
        assertNotNull(AuthValidation.validateLogin("rutul@example.com", "").password)
    }

    /**
     * A returning user's password is Firebase's business, not the form's - see the comment in
     * AuthValidation.validateLogin.
     */
    @Test
    fun `login does not apply the length rule to an existing password`() {
        assertNull(AuthValidation.validateLogin("rutul@example.com", "abc").password)
    }

    @Test
    fun `error messages are written for a student, not a developer`() {
        assertEquals(
            "WhatsApp number is required - buyers contact you here",
            AuthValidation.validateSignup(validForm.copy(whatsappNumber = "")).whatsappNumber,
        )
    }
}
