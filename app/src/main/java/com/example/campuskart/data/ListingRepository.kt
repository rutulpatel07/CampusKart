package com.example.campuskart.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** The result of writing a listing. [Failure.message] is written to be shown as-is. */
sealed interface ListingOutcome {
    data class Success(val listing: Listing) : ListingOutcome
    data class Failure(val message: String) : ListingOutcome
}

/**
 * Everything CampusKart does with the `listings` collection.
 *
 * Same shape as [AuthRepository] and for the same reasons: a plain object, no DI, and the only
 * place in the app that touches the Firestore SDK for listings. Days 6 and 7 add the feed query,
 * the per-user query and the edit/sold/delete writes here.
 */
object ListingRepository {

    private const val TAG = "CampusKartListings"
    private const val LISTINGS = "listings"

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /**
     * Writes a new listing document (FR-LIST-001) and returns it with the ID Firestore assigned.
     *
     * The document ID is generated on the client *before* the write - `document()` with no
     * argument does not talk to the network. That is what lets the returned [Listing] carry a
     * real id, which the success screen shows and Day 6's Item Detail route will navigate by.
     *
     * [Listing.createdAt] is deliberately left null: the `@ServerTimestamp` annotation on it
     * makes Firestore fill it with the server's clock rather than the phone's.
     *
     * There is no timeout around the write on purpose. Firestore applies a write to its local
     * cache immediately and only completes the Task once the server has acknowledged it, so with
     * no network this call waits rather than failing - but the write is already queued, and it
     * will reach the server the moment connectivity returns. Timing out here would report a
     * failure for a listing that is going to be posted anyway, and a user who then retried would
     * end up with two copies. In practice the photo upload above fails first when the network is
     * down, which is where an offline publish actually stops.
     */
    suspend fun publish(listing: Listing): ListingOutcome {
        val sellerUid = AuthRepository.currentUid
            ?: return ListingOutcome.Failure("You are signed out. Log in and try again.")

        val document = firestore.collection(LISTINGS).document()
        val toWrite = listing.copy(
            id = document.id,
            // Never trusted from the caller: whoever is signed in right now is the seller, and
            // Day 8's security rules will enforce exactly this on the server as well.
            sellerUid = sellerUid,
            status = Listing.STATUS_AVAILABLE,
            createdAt = null,
        )

        return try {
            document.set(toWrite).await()
            ListingOutcome.Success(toWrite)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not write the listing", e)
            ListingOutcome.Failure(errorMessage(e))
        }
    }

    private fun errorMessage(e: Exception): String = when {
        e !is FirebaseFirestoreException -> "Could not save your listing. Please try again."

        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You do not have permission to post listings. Check the Firestore rules " +
                "(README - Setup)."

        e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Could not reach the database. Check your connection and try again."

        else -> "Could not save your listing. Please try again."
    }
}
