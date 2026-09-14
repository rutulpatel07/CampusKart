package com.example.campuskart.ui.post

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.AuthRepository
import com.example.campuskart.data.CloudinaryUploader
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingOutcome
import com.example.campuskart.data.ListingPhoto
import com.example.campuskart.data.ListingRepository
import com.example.campuskart.data.CategorySuggestionOutcome
import com.example.campuskart.data.MlKitCategorySuggester
import com.example.campuskart.data.UploadOutcome
import com.example.campuskart.data.UserProfile
import com.example.campuskart.model.ListingOptions
import kotlinx.coroutines.launch

/**
 * The three waits between tapping Publish and the listing existing. They are separated because
 * they take noticeably different amounts of time and fail for entirely different reasons, and a
 * single "Publishing..." spinner would leave a student staring at a frozen screen with no idea
 * whether their photo is uploading or their connection has died.
 */
enum class PublishStep(val label: String) {
    PREPARING("Preparing the photo..."),
    UPLOADING("Uploading the photo..."),
    SAVING("Saving the listing..."),
}

sealed interface CategorySuggestionState {
    data object Idle : CategorySuggestionState
    data object Analysing : CategorySuggestionState
    data class Suggested(val suggestion: CategorySuggestion) : CategorySuggestionState
    data object NoMatch : CategorySuggestionState
    data class Failed(val message: String) : CategorySuggestionState
}

/** Everything the Post Item screen draws. */
data class PostItemUiState(
    val form: ListingForm = ListingForm(),
    val errors: ListingErrors = ListingErrors(),
    /** The photo as picked, before compression - shown as the on-screen preview. */
    val photoUri: Uri? = null,
    /** Non-null exactly while a publish is in flight. */
    val step: PublishStep? = null,
    val formError: String? = null,
    /** Set once the document exists in Firestore; the screen swaps to its success state. */
    val published: Listing? = null,
    /**
     * The poster's own profile. Their name and branch are denormalised onto every listing they
     * publish (see Listing), so it has to be loaded before anything can be posted.
     */
    val seller: UserProfile? = null,
    val sellerError: String? = null,
    /** Day 9's on-device hint. It never disables the form or blocks publishing. */
    val categorySuggestion: CategorySuggestionState = CategorySuggestionState.Idle,
) {
    val publishing: Boolean get() = step != null
    val enabled: Boolean get() = !publishing
}

/**
 * Holds the Post Item form and runs the publish sequence: compress the photo, upload it to
 * Cloudinary, then write the `listings` document (FR-LIST-001/002).
 *
 * An [AndroidViewModel] because two of those steps need a Context - the ContentResolver that
 * reads the picked photo, and the cache directory the camera writes into. The screen itself
 * stays a pure function of [PostItemUiState].
 *
 * The order matters. The photo is uploaded first and the document written second, so a failure
 * leaves at worst an orphaned image in Cloudinary - invisible to the app, and costing nothing
 * but free-tier storage. The other order would put a listing on the feed pointing at a photo
 * that does not exist, which every screen that renders it would then have to handle.
 */
class PostItemViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(PostItemUiState())
        private set

    private var suggestionGeneration = 0
    private var categoryChangedManually = false

    init {
        loadSeller()
    }

    /**
     * Reads the signed-in user's `users` document, for [Listing.sellerName] and
     * [Listing.sellerBranch].
     *
     * Done up front rather than at publish time so that a profile that cannot be read is a
     * message on an empty form, not a failure after the photo has already been uploaded.
     */
    fun loadSeller() {
        uiState = uiState.copy(sellerError = null)
        viewModelScope.launch {
            uiState = try {
                val profile = AuthRepository.profile()
                if (profile == null) {
                    uiState.copy(sellerError = "Could not find your profile. Try logging out and back in.")
                } else {
                    uiState.copy(seller = profile)
                }
            } catch (e: Exception) {
                uiState.copy(sellerError = "Could not load your profile: ${e.message}")
            }
        }
    }

    /**
     * As in SignupViewModel: the screen sends back the whole edited form and the changed field is
     * worked out here, so only that field's error is cleared rather than every message on the
     * form disappearing on the first keystroke.
     */
    fun onFormChange(form: ListingForm) {
        val old = uiState.form
        if (old.category != form.category) categoryChangedManually = true
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

    /**
     * A photo arrived from the camera or the picker. Null means the user backed out of either
     * one without taking or choosing anything, which is not a change - the photo they had
     * before, if any, stays.
     */
    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        val generation = ++suggestionGeneration
        categoryChangedManually = false
        uiState = uiState.copy(
            photoUri = uri,
            // FR-AI-004: a weak or unknown image still starts at a valid category.
            form = uiState.form.copy(category = ListingOptions.FALLBACK_CATEGORY),
            errors = uiState.errors.copy(photo = null),
            formError = null,
            categorySuggestion = CategorySuggestionState.Analysing,
        )
        viewModelScope.launch {
            when (val outcome = MlKitCategorySuggester.suggest(getApplication(), uri)) {
                is CategorySuggestionOutcome.Suggested -> {
                    if (generation != suggestionGeneration) return@launch
                    uiState = uiState.copy(
                        // A category tapped while ML Kit was working always wins.
                        form = if (categoryChangedManually) uiState.form else {
                            uiState.form.copy(category = outcome.suggestion.category)
                        },
                        categorySuggestion = CategorySuggestionState.Suggested(outcome.suggestion),
                    )
                }

                CategorySuggestionOutcome.NoMatch -> if (generation == suggestionGeneration) {
                    uiState = uiState.copy(categorySuggestion = CategorySuggestionState.NoMatch)
                }

                is CategorySuggestionOutcome.Failure -> if (generation == suggestionGeneration) {
                    uiState = uiState.copy(
                        categorySuggestion = CategorySuggestionState.Failed(outcome.message),
                    )
                }
            }
        }
    }

    fun publish() {
        if (uiState.publishing) return

        val state = uiState
        val errors = ListingValidation.validate(state.form, hasPhoto = state.photoUri != null)
        if (!errors.isValid) {
            uiState = state.copy(errors = errors, formError = null)
            return
        }

        val photoUri = state.photoUri ?: return
        val seller = state.seller
        if (seller == null || seller.name.isBlank()) {
            uiState = state.copy(
                formError = "Your profile has not loaded yet, so the listing would have no seller name.",
            )
            return
        }

        uiState = state.copy(errors = ListingErrors(), formError = null, step = PublishStep.PREPARING)
        viewModelScope.launch {
            val bytes = ListingPhoto.prepareForUpload(getApplication(), photoUri)
            if (bytes == null) {
                uiState = uiState.copy(
                    step = null,
                    errors = uiState.errors.copy(photo = "That photo could not be read"),
                    formError = "Could not read that photo. Try taking or choosing another one.",
                )
                return@launch
            }

            uiState = uiState.copy(step = PublishStep.UPLOADING)
            val photoUrl = when (val upload = CloudinaryUploader.upload(bytes)) {
                is UploadOutcome.Success -> upload.secureUrl
                is UploadOutcome.Failure -> {
                    uiState = uiState.copy(step = null, formError = upload.message)
                    return@launch
                }
            }

            uiState = uiState.copy(step = PublishStep.SAVING)
            val form = uiState.form
            val outcome = ListingRepository.publish(
                // sellerUid, status and createdAt are stamped by the repository.
                Listing(
                    title = form.title.trim(),
                    description = form.description.trim(),
                    category = form.category,
                    price = ListingValidation.parsePrice(form.price) ?: 0,
                    condition = form.condition,
                    photoUrl = photoUrl,
                    sellerName = seller.name,
                    sellerBranch = seller.branch,
                ),
            )

            uiState = when (outcome) {
                is ListingOutcome.Success -> {
                    // The full-size capture has been uploaded and is never read again.
                    ListingPhoto.clearCameraCache(getApplication())
                    uiState.copy(step = null, published = outcome.listing)
                }

                is ListingOutcome.Failure -> uiState.copy(step = null, formError = outcome.message)
            }
        }
    }

    /** "Post another item" - back to an empty form, keeping the profile already loaded. */
    fun startAnother() {
        suggestionGeneration++
        categoryChangedManually = false
        uiState = PostItemUiState(seller = uiState.seller)
    }
}
