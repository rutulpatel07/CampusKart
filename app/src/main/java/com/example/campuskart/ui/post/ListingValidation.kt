package com.example.campuskart.ui.post

/**
 * The Post Item form in one object. [price] is a String rather than a number because that is what
 * a text field holds - an empty field and a "0" are different things, and only one of them is a
 * price. It becomes a Long on the way to Firestore (see [ListingValidation.parsePrice]).
 */
data class ListingForm(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: String = "",
    val condition: String = "",
)

/** Per-field messages for the Post Item form. A null field means that field is fine. */
data class ListingErrors(
    val photo: String? = null,
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val price: String? = null,
    val condition: String? = null,
) {
    val isValid: Boolean
        get() = listOf(photo, title, description, category, price, condition).all { it == null }
}

/**
 * Form rules for Post Item, checked on the device before anything is uploaded.
 *
 * Plain Kotlin with no Android or Firebase imports, so the rules are covered by ordinary JVM unit
 * tests (see ListingValidationTest) rather than needing an emulator - the same arrangement as
 * [com.example.campuskart.ui.auth.AuthValidation].
 *
 * Checking before upload rather than after is the point: a listing that fails validation should
 * never have spent a student's data on a photo upload that is then thrown away.
 */
object ListingValidation {

    /** Long enough to be searchable on Day 7, short enough to fit a feed card in two lines. */
    const val MIN_TITLE_LENGTH = 5
    const val MAX_TITLE_LENGTH = 70

    const val MAX_DESCRIPTION_LENGTH = 600

    /**
     * Free items are allowed - passing a textbook down for nothing is a normal thing to do here -
     * so the floor is zero. The ceiling exists to catch a slipped finger on the number pad, not
     * to price-cap anything: the most expensive item this app expects to see is a cycle.
     */
    const val MAX_PRICE = 100_000L

    fun validate(form: ListingForm, hasPhoto: Boolean) = ListingErrors(
        // FR-LIST-001: every listing needs at least one photo. Nobody buys a used drafter
        // sight-unseen, and a feed of grey placeholders is not worth browsing.
        photo = "Add a photo of the item".takeIf { !hasPhoto },
        title = titleError(form.title.trim()),
        description = descriptionError(form.description.trim()),
        category = "Pick a category".takeIf { form.category.isBlank() },
        price = priceError(form.price),
        condition = "Pick the condition".takeIf { form.condition.isBlank() },
    )

    private fun titleError(title: String): String? = when {
        title.isEmpty() -> "Give your listing a title"
        title.length < MIN_TITLE_LENGTH -> "At least $MIN_TITLE_LENGTH characters"
        title.length > MAX_TITLE_LENGTH -> "Keep it under $MAX_TITLE_LENGTH characters"
        else -> null
    }

    private fun descriptionError(description: String): String? = when {
        description.isEmpty() -> "Say what it is and what condition it is in"
        description.length > MAX_DESCRIPTION_LENGTH ->
            "Keep it under $MAX_DESCRIPTION_LENGTH characters"

        else -> null
    }

    private fun priceError(price: String): String? {
        val trimmed = price.trim()
        return when {
            trimmed.isEmpty() -> "Enter a price"
            parsePrice(trimmed) == null -> "Enter a whole number of rupees"
            parsePrice(trimmed)!! > MAX_PRICE -> "That is over the ${MAX_PRICE / 1000}k limit"
            else -> null
        }
    }

    /**
     * Whole rupees only. `toLongOrNull` also rejects an over-length string of digits that would
     * otherwise overflow, which is the other way the price field gets abused.
     */
    fun parsePrice(price: String): Long? = price.trim().toLongOrNull()?.takeIf { it >= 0 }
}
