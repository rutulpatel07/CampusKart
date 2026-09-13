package com.example.campuskart.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
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
 * The result of the three owner-only actions on My Listings: edit, mark sold/available, delete
 * (FR-LIST-007/008/009).
 *
 * [Success] carries nothing because the caller already knows what it asked for. It also means
 * something: Firestore completes a write Task only once the *server* has acknowledged it, so a
 * Success here is a change that has really landed - not one sitting in the local cache.
 */
sealed interface ListingActionOutcome {
    data object Success : ListingActionOutcome
    data class Failure(val message: String) : ListingActionOutcome
}

/**
 * The five fields a seller is allowed to change after publishing (FR-LIST-007).
 *
 * Deliberately not a [Listing]. Passing a whole listing to an update would invite overwriting
 * `sellerUid`, `status` or `createdAt` by accident - and this type makes it impossible to even
 * express that, rather than relying on the repository to strip them back out.
 *
 * The photo is not here: changing it would mean a second Cloudinary upload and an orphaned
 * original, so a seller who photographed the wrong thing deletes the listing and posts again.
 */
data class ListingEdit(
    val title: String,
    val description: String,
    val category: String,
    val price: Long,
    val condition: String,
)

/**
 * Everything CampusKart does with the `listings` collection.
 *
 * Same shape as [AuthRepository] and for the same reasons: a plain object, no DI, and the only
 * place in the app that touches the Firestore SDK for listings. Day 6 added the feed and detail
 * reads; Day 7 added the per-seller query and the three owner-only writes below.
 */
object ListingRepository {

    private const val TAG = "CampusKartListings"
    private const val LISTINGS = "listings"

    // Field names as Firestore stores them. Referred to as strings because that is the only
    // thing the query builder takes, and a typo here fails at runtime rather than at compile
    // time - so they are written once, here, instead of at every call site.
    private const val FIELD_STATUS = "status"
    private const val FIELD_CREATED_AT = "createdAt"
    private const val FIELD_SELLER_UID = "sellerUid"

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
            ListingOutcome.Failure(
                errorMessage(e, "Could not save your listing. Please try again."),
            )
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

    /**
     * My Listings: everything the signed-in user has posted, newest first (FR-LIST-010).
     *
     * Unlike the feed this does *not* filter on status - a seller needs to see what they have
     * already sold, both to confirm it happened and to be able to put it back up if the deal
     * fell through. Sorting the sold ones to the bottom is left to the screen, because it is a
     * presentation choice and doing it in the query would need a third index.
     *
     * Pairs an equality filter with an ordering on a different field, so like the feed it needs
     * a composite index - `sellerUid ASC, createdAt DESC`, recorded in `firestore.indexes.json`.
     */
    suspend fun myListings(): FeedOutcome {
        val uid = AuthRepository.currentUid
            ?: return FeedOutcome.Failure("You are signed out. Log in and try again.")

        return try {
            val snapshot = firestore.collection(LISTINGS)
                .whereEqualTo(FIELD_SELLER_UID, uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .await()
            FeedOutcome.Success(snapshot.toObjects(Listing::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not load the user's listings", e)
            FeedOutcome.Failure(errorMessage(e, "Could not load your listings. Please try again."))
        }
    }

    /**
     * Rewrites the five editable fields of a listing the caller owns (FR-LIST-007).
     *
     * `update` with an explicit field map rather than `set`: a `set` would write the whole
     * document, so any field not named here - the photo, the seller, the timestamp - would be
     * erased by an edit that never intended to touch them.
     */
    suspend fun edit(id: String, edit: ListingEdit): ListingActionOutcome = write(id) { document ->
        document.update(
            mapOf(
                "title" to edit.title,
                "description" to edit.description,
                "category" to edit.category,
                "price" to edit.price,
                "condition" to edit.condition,
            ),
        )
    }

    /**
     * Flips a listing between available and sold (FR-LIST-008).
     *
     * Marking sold takes the listing off the feed, since the feed query filters on this exact
     * field - so this one write is the whole feature. It is reversible on purpose: a deal that
     * falls through should not cost the seller a re-upload of the photo.
     */
    suspend fun setStatus(id: String, sold: Boolean): ListingActionOutcome = write(id) { document ->
        val status = if (sold) Listing.STATUS_SOLD else Listing.STATUS_AVAILABLE
        document.update(FIELD_STATUS, status)
    }

    /**
     * Removes a listing for good (FR-LIST-009).
     *
     * The Cloudinary image is deliberately left behind. Deleting it would need the API secret,
     * which cannot live in an APK (README, "Challenges faced") - so the orphan stays, invisible
     * to the app and costing nothing but free-tier storage. That is the same trade the unsigned
     * upload already makes.
     */
    suspend fun delete(id: String): ListingActionOutcome = write(id) { document -> document.delete() }

    /**
     * The shared front half of the three writes above: check the caller is signed in, check the
     * listing is actually theirs, run the write, translate whatever went wrong.
     *
     * The ownership read is not security - a determined user runs their own code and skips it
     * entirely. Day 8's Firestore rules are what actually enforce it, on the server. This check
     * exists so that the *honest* failure - a listing deleted from another device, or a stale My
     * Listings screen - is reported as "this listing is no longer there" instead of arriving as
     * a bare PERMISSION_DENIED once those rules land.
     */
    private suspend fun write(
        id: String,
        action: (DocumentReference) -> Task<Void>,
    ): ListingActionOutcome {
        val uid = AuthRepository.currentUid
            ?: return ListingActionOutcome.Failure("You are signed out. Log in and try again.")
        if (id.isBlank()) return ListingActionOutcome.Failure("That listing no longer exists.")

        return try {
            val document = firestore.collection(LISTINGS).document(id)
            val existing = document.get().await().toObject(Listing::class.java)
                ?: return ListingActionOutcome.Failure("That listing no longer exists.")
            if (existing.sellerUid != uid) {
                return ListingActionOutcome.Failure("You can only change your own listings.")
            }

            action(document).await()
            ListingActionOutcome.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not change listing $id", e)
            ListingActionOutcome.Failure(
                errorMessage(e, "Could not save that change. Please try again."),
            )
        }
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

    /**
     * Turns a Firestore exception into something a student can act on, with [fallback] as the
     * generic ending for the cases that are not worth naming.
     *
     * FAILED_PRECONDITION gets its own message because it means one specific, one-time thing:
     * the composite index this query needs has not been created yet. Firestore's own
     * exception carries a ready-made console link to create it, but that link only ever appears
     * in Logcat - on screen the user would otherwise see "something went wrong" for a problem
     * that is fixed by a single click.
     */
    private fun errorMessage(e: Exception, fallback: String): String = when {
        e !is FirebaseFirestoreException -> fallback

        e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore needs a one-time index for this query. Open Logcat, find the " +
                "\"FAILED_PRECONDITION\" line from Firestore and tap the console link in it - " +
                "or see README \"Setup\". This only has to be done once."

        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore refused that. Check the rules for the listings collection " +
                "(README - Setup)."

        e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Could not reach the database. Check your connection and try again."

        else -> fallback
    }
}
