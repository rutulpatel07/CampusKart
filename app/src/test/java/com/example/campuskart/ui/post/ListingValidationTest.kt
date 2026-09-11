package com.example.campuskart.ui.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Post Item form rules. No emulator, no Firebase, no Cloudinary - the point of
 * keeping [ListingValidation] free of Android imports is that these run in a second.
 */
class ListingValidationTest {

    private val validForm = ListingForm(
        title = "Engineering Drawing drafter set",
        description = "Mini-drafter, set squares and compass box. All parts present.",
        category = "Drafter",
        price = "350",
        condition = "Good",
    )

    @Test
    fun `a complete form with a photo passes`() {
        assertTrue(ListingValidation.validate(validForm, hasPhoto = true).isValid)
    }

    /** FR-LIST-001: every listing needs at least one photo. */
    @Test
    fun `a complete form without a photo is rejected`() {
        val errors = ListingValidation.validate(validForm, hasPhoto = false)
        assertNotNull(errors.photo)
        // Only the photo - a missing photo must not make the filled-in fields look wrong too.
        assertNull(errors.title)
        assertNull(errors.price)
    }

    @Test
    fun `every empty field reports its own error`() {
        val errors = ListingValidation.validate(ListingForm(), hasPhoto = false)
        assertNotNull(errors.photo)
        assertNotNull(errors.title)
        assertNotNull(errors.description)
        assertNotNull(errors.category)
        assertNotNull(errors.price)
        assertNotNull(errors.condition)
    }

    @Test
    fun `a title of only whitespace is empty, not valid`() {
        val errors = ListingValidation.validate(validForm.copy(title = "     "), hasPhoto = true)
        assertNotNull(errors.title)
    }

    @Test
    fun `a title shorter than the minimum is rejected`() {
        val errors = ListingValidation.validate(validForm.copy(title = "Book"), hasPhoto = true)
        assertNotNull(errors.title)
    }

    @Test
    fun `a title over the maximum is rejected`() {
        val long = "a".repeat(ListingValidation.MAX_TITLE_LENGTH + 1)
        assertNotNull(ListingValidation.validate(validForm.copy(title = long), hasPhoto = true).title)
    }

    /** Passing a textbook down for nothing is a normal thing to do, so zero is a real price. */
    @Test
    fun `a free item is allowed`() {
        assertNull(ListingValidation.validate(validForm.copy(price = "0"), hasPhoto = true).price)
    }

    @Test
    fun `a price over the ceiling is rejected`() {
        val over = (ListingValidation.MAX_PRICE + 1).toString()
        assertNotNull(ListingValidation.validate(validForm.copy(price = over), hasPhoto = true).price)
    }

    @Test
    fun `a non-numeric price is rejected`() {
        assertNotNull(ListingValidation.validate(validForm.copy(price = "350rs"), hasPhoto = true).price)
        assertNotNull(ListingValidation.validate(validForm.copy(price = "3.50"), hasPhoto = true).price)
        assertNotNull(ListingValidation.validate(validForm.copy(price = "-5"), hasPhoto = true).price)
    }

    @Test
    fun `parsePrice reads whole rupees and rejects anything else`() {
        assertEquals(350L, ListingValidation.parsePrice("350"))
        assertEquals(350L, ListingValidation.parsePrice(" 350 "))
        assertEquals(0L, ListingValidation.parsePrice("0"))
        assertNull(ListingValidation.parsePrice(""))
        assertNull(ListingValidation.parsePrice("-1"))
        assertNull(ListingValidation.parsePrice("3.5"))
        // Longer than a Long can hold - the price field's other failure mode.
        assertNull(ListingValidation.parsePrice("99999999999999999999"))
    }

    @Test
    fun `a description over the maximum is rejected`() {
        val long = "a".repeat(ListingValidation.MAX_DESCRIPTION_LENGTH + 1)
        val errors = ListingValidation.validate(validForm.copy(description = long), hasPhoto = true)
        assertNotNull(errors.description)
    }
}
