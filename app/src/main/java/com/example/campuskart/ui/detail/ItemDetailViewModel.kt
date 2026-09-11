package com.example.campuskart.ui.detail

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuskart.data.AuthRepository
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingDetailOutcome
import com.example.campuskart.data.ListingRepository
import com.example.campuskart.data.UserProfile
import com.example.campuskart.data.WhatsAppContact
import com.example.campuskart.ui.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Everything the Item Detail screen draws. */
data class ItemDetailUiState(
    val loading: Boolean = true,
    val listing: Listing? = null,
    /**
     * The seller's live `users` document, not the name copied onto the listing. It is read for
     * the WhatsApp number, and while it is here the branch and semester shown on screen may as
     * well come from it too - so a seller who has since moved up a semester is not misreported.
     */
    val seller: UserProfile? = null,
    /** The listing was deleted, or the id in the route matches nothing. */
    val missing: Boolean = false,
    /** A load that failed for a reason worth retrying. */
    val error: String? = null,
    /** The listing belongs to whoever is signed in, so there is nobody to message. */
    val isOwnListing: Boolean = false,
) {
    /**
     * Whether "Chat on WhatsApp" can do anything (FR-CONTACT-002).
     *
     * A seller whose profile failed to load, or who somehow has no usable number, leaves the
     * button visible but disabled with the reason underneath - rather than hiding the app's one
     * and only contact mechanism, which would look like the feature is simply missing.
     */
    val canContactSeller: Boolean
        get() = !isOwnListing && listing != null && contactNumber != null

    /** The number the link is built from. Never shown on screen (FR-CONTACT-003). */
    val contactNumber: String?
        get() = seller?.whatsappNumber
            ?.takeIf { WhatsAppContact.internationalNumber(it) != null }

    /** Why the button is disabled, when it is. Null when it works. */
    val contactBlockedReason: String?
        get() = when {
            isOwnListing || listing == null -> null
            seller == null -> "This seller's profile could not be loaded, so there is no number " +
                "to message."

            contactNumber == null -> "This seller did not leave a usable WhatsApp number."
            else -> null
        }
}

/**
 * Loads one listing and its seller for the Item Detail screen (FR-LIST-006).
 *
 * Two reads, run one after the other because the second needs `sellerUid` out of the first. That
 * is the cost of not denormalising the seller's WhatsApp number onto the listing - and it is the
 * right cost: a number copied onto every listing would be a phone number sitting in a document
 * the whole campus can read, which is exactly what FR-CONTACT-003 exists to prevent.
 *
 * A seller read that fails is not a failed screen. The listing is already loaded by then, and a
 * buyer who can see the item but not message it is far better served than one staring at an
 * error page - so the failure is carried as [ItemDetailUiState.contactBlockedReason] and the
 * rest of the screen draws normally.
 */
class ItemDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    /**
     * The route argument, delivered by Navigation into the SavedStateHandle that the default
     * ViewModel factory hands to this constructor. Reading it here rather than taking it as a
     * parameter is what lets the screen call plain `viewModel()` with no custom factory.
     */
    private val listingId: String =
        savedStateHandle.get<String>(Routes.ARG_LISTING_ID).orEmpty()

    var uiState by mutableStateOf(ItemDetailUiState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = ItemDetailUiState(loading = true)
        viewModelScope.launch {
            val listing = when (val outcome = ListingRepository.listing(listingId)) {
                is ListingDetailOutcome.Success -> outcome.listing

                is ListingDetailOutcome.Missing -> {
                    uiState = uiState.copy(loading = false, missing = true)
                    return@launch
                }

                is ListingDetailOutcome.Failure -> {
                    uiState = uiState.copy(loading = false, error = outcome.message)
                    return@launch
                }
            }

            uiState = uiState.copy(
                loading = false,
                listing = listing,
                isOwnListing = listing.sellerUid == AuthRepository.currentUid,
            )

            // Not needed when the listing is the user's own: there is no contact button to fill
            // in, and the branch line already reads "You" rather than a seller's details.
            if (uiState.isOwnListing) return@launch

            val seller = try {
                AuthRepository.profile(listing.sellerUid)
            } catch (e: CancellationException) {
                // The screen was left mid-read. Nothing to report to a user who is already gone.
                throw e
            } catch (e: Exception) {
                // Deliberately not surfaced as an error state - see the class comment. The UI
                // reports it as "cannot message this seller", which is what it means to the
                // person reading it, and the detail stays in Logcat for whoever is debugging.
                Log.w(TAG, "Could not load the seller for listing $listingId", e)
                null
            }
            uiState = uiState.copy(seller = seller)
        }
    }

    private companion object {
        const val TAG = "CampusKartDetail"
    }
}
