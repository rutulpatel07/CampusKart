package com.example.campuskart.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** The result of writing a listing. [Failure.message] is written to be shown as-is. */
sealed interface ListingOutcome {
    data class Success(val listing: Listing) : ListingOutcome
    data class Failure(val message: String) : ListingOutcome
}

/** The result of reading the feed (FR-LIST-003). */
sealed interface FeedOutcome {
    data class Success(val listings: List<Listing>) : FeedOutcome
    data class Failure(val message: String) : FeedOutcome
}

/**
 * The result of reading one listing (FR-LIST-006).
 *
 * [Missing] is deliberately separate from [Failure]: a listing the seller has since deleted is a
 * normal thing to arrive at from a stale feed, and telling the user "this listing is no longer
 * available" is a different - and far less alarming - message than "something went wrong".
 */
sealed interface ListingDetailOutcome {
    data class Success(val listing: Listing) : ListingDetailOutcome
    data object Missing : ListingDetailOutcome
    data class Failure(val message: String) : ListingDetailOutcome
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

    // Field names as Firestore stores them. Referred to as strings because that is the only
    // thing the query builder takes, and a typo here fails at runtime rather than at compile
    // time - so they are written once, here, instead of at every call site.
    private const val FIELD_STATUS = "status"
    private const val FIELD_CREATED_AT = "createdAt"

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

    /**
     * The Home Feed: every available listing, newest first (FR-LIST-003).
     *
     * A one-shot `get()` rather than a `snapshots()` listener. A live listener would keep the
     * feed updating by itself, but it also keeps a socket open for as long as the tab is on
     * screen, and CampusKart's feed changes a few times a day - not a few times a minute. The
     * Refresh action in the app bar covers the gap, and the screen reloads itself whenever the
     * tab is resumed, so a listing posted a moment ago is there when the user walks back to it.
     *
     * Sold listings are filtered out by the query rather than in Kotlin, so a feed of twenty
     * available items costs twenty document reads even once a semester of sold items has piled
     * up behind them.
     *
     * Note this pairs an equality filter on `status` with an ordering on `createdAt`, which
     * Firestore can only serve from a composite index - see [errorMessage] and README "Setup".
     */
    suspend fun availableListings(): FeedOutcome = try {
        val snapshot = firestore.collection(LISTINGS)
            .whereEqualTo(FIELD_STATUS, Listing.STATUS_AVAILABLE)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .get()
            .await()
        FeedOutcome.Success(snapshot.toObjects(Listing::class.java))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Could not load the feed", e)
        FeedOutcome.Failure(errorMessage(e, "Could not load listings. Please try again."))
    }

    /** One listing, for the Item Detail screen (FR-LIST-006). */
    suspend fun listing(id: String): ListingDetailOutcome {
        if (id.isBlank()) return ListingDetailOutcome.Missing
        return try {
            val document = firestore.collection(LISTINGS).document(id).get().await()
            val listing = document.toObject(Listing::class.java)
            if (listing == null) ListingDetailOutcome.Missing
            else ListingDetailOutcome.Success(listing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not load listing $id", e)
            ListingDetailOutcome.Failure(
                errorMessage(e, "Could not load this listing. Please try again."),
            )
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

    /**
     * The same translation for the read paths, with [fallback] as the generic ending.
     *
     * FAILED_PRECONDITION gets its own message because it means one specific, one-time thing:
     * the composite index the feed query needs has not been created yet. Firestore's own
     * exception carries a ready-made console link to create it, but that link only ever appears
     * in Logcat - on screen the user would otherwise see "something went wrong" for a problem
     * that is fixed by a single click.
     */
    private fun errorMessage(e: Exception, fallback: String): String = when {
        e !is FirebaseFirestoreException -> fallback

        e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore needs a one-time index for the feed. Open Logcat, find the " +
                "\"FAILED_PRECONDITION\" line from Firestore and tap the console link in it - " +
                "or see README \"Setup\". This only has to be done once."

        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You do not have permission to read listings. Check the Firestore rules " +
                "(README - Setup)."

        e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Could not reach the database. Check your connection and try again."

        else -> fallback
    }
}
