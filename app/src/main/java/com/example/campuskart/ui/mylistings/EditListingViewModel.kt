package com.example.campuskart.ui.mylistings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingActionOutcome
import com.example.campuskart.data.ListingDetailOutcome
import com.example.campuskart.data.ListingEdit
import com.example.campuskart.data.ListingRepository
import com.example.campuskart.ui.navigation.Routes
import com.example.campuskart.ui.post.ListingErrors
import com.example.campuskart.ui.post.ListingForm
import com.example.campuskart.ui.post.ListingValidation
import kotlinx.coroutines.launch

/** Everything the Edit Listing screen draws. */
data class EditListingUiState(
    val loading: Boolean = true,
    /** The listing as it was loaded - the photo and the original values to compare against. */
    val listing: Listing? = null,
    val form: ListingForm = ListingForm(),
    val errors: ListingErrors = ListingErrors(),
    val saving: Boolean = false,
    /** A whole-form failure: the write was refused, or the network is down. */
    val formError: String? = null,
    /** The listing could not be read at all - the screen has nothing to show. */
    val loadError: String? = null,
    /** Deleted from another device between opening My Listings and tapping Edit. */
    val missing: Boolean = false,
    /** Set once the update has landed; the screen navigates back. */
    val saved: Boolean = false,
) {
    val enabled: Boolean get() = !saving && listing != null

    /**
     * Whether anything has actually been typed.
     *
     * Save is disabled until something changes, which does two useful things: it makes an
     * accidental Edit tap a no-op rather than a pointless write, and it turns the button itself
     * into the answer to "did that take?".
     */
    val dirty: Boolean get() = listing != null && form != listing.toForm()
}

/** The listing's stored values as the form holds them. */
private fun Listing.toForm() = ListingForm(
    title = title,
    description = description,
    category = category,
    // The form keeps the price as text, because an empty field and a "0" are different things.
    price = price.toString(),
    condition = condition,
)

/**
 * Loads one listing the signed-in user owns and saves changes to its five editable fields
 * (FR-LIST-007).
 *
 * The form, its rules and its error type are the same ones Post Item uses, imported from
 * `ui.post` rather than copied. That is what guarantees a title that was too short to post is
 * also too short to edit down to - the two screens cannot drift apart, because there is only one
 * set of rules.
 *
 * The photo is deliberately not editable. Replacing it would mean a second Cloudinary upload and
 * an orphaned original that the app has no credentials to delete (README, "Challenges faced"), so
 * a seller who photographed the wrong thing deletes the listing and posts it again.
 */
class EditListingViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    /** The route argument, put into the SavedStateHandle by Navigation. */
    private val listingId: String = savedStateHandle[Routes.ARG_LISTING_ID] ?: ""

    var uiState by mutableStateOf(EditListingUiState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = uiState.copy(loading = true, loadError = null, missing = false)
        viewModelScope.launch {
            uiState = when (val outcome = ListingRepository.listing(listingId)) {
                is ListingDetailOutcome.Success -> uiState.copy(
                    loading = false,
                    listing = outcome.listing,
                    form = outcome.listing.toForm(),
                )

                is ListingDetailOutcome.Missing -> uiState.copy(loading = false, missing = true)

                is ListingDetailOutcome.Failure ->
                    uiState.copy(loading = false, loadError = outcome.message)
            }
        }
    }

    /**
     * As on Post Item: the screen sends back the whole edited form and the changed field is
     * worked out here, so only that field's message is cleared rather than every message on the
     * form vanishing on the first keystroke.
     */
    fun onFormChange(form: ListingForm) {
        val old = uiState.form
        uiState = uiState.copy(
            form = form,
            errors = uiState.errors.copy(
                title = uiState.errors.title.keepUnless(old.title != form.title),
                description = uiState.errors.description
                    .keepUnless(old.description != form.description),
                category = uiState.errors.category.keepUnless(old.category != form.category),
                price = uiState.errors.price.keepUnless(old.price != form.price),
                condition = uiState.errors.condition.keepUnless(old.condition != form.condition),
            ),
            formError = null,
        )
    }

    private fun String?.keepUnless(changed: Boolean): String? = if (changed) null else this

    fun save() {
        if (uiState.saving) return
        val state = uiState
        val listing = state.listing ?: return

        // hasPhoto is always true here: an existing listing has one, and this screen cannot
        // change it. Passing the flag anyway keeps one shared set of rules rather than a second
        // "edit" variant of them that could drift.
        val errors = ListingValidation.validate(state.form, hasPhoto = true)
        if (!errors.isValid) {
            uiState = state.copy(errors = errors, formError = null)
            return
        }

        val form = state.form
        uiState = state.copy(saving = true, errors = ListingErrors(), formError = null)
        viewModelScope.launch {
            val outcome = ListingRepository.edit(
                id = listing.id,
                edit = ListingEdit(
                    title = form.title.trim(),
                    description = form.description.trim(),
                    category = form.category,
                    price = ListingValidation.parsePrice(form.price) ?: 0,
                    condition = form.condition,
                ),
            )

            uiState = when (outcome) {
                is ListingActionOutcome.Success -> uiState.copy(saving = false, saved = true)
                is ListingActionOutcome.Failure ->
                    uiState.copy(saving = false, formError = outcome.message)
            }
        }
    }
}
