package com.example.campuskart.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * A row of the `listings` collection (PRD.md Section 8).
 *
 * As with [UserProfile], every property has a default so Firestore's `toObject()` can build the
 * object through a no-argument constructor and then fill the fields in.
 */
data class Listing(
    /**
     * Filled in by Firestore from the document ID on the way out, and ignored on the way in - so
     * unlike `users.uid`, the id is not duplicated inside the document. A listing is always read
     * as a whole document, so there is nothing to gain from storing it twice.
     */
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    /**
     * Whole rupees. Firestore stores every number as a double, and `toObject` will happily read
     * one back into a Long, so nothing here needs to change - but keeping the field integral
     * means a price never renders as "350.0".
     */
    val price: Long = 0,
    val condition: String = "",
    /** Cloudinary `secure_url`, from [CloudinaryUploader]. */
    val photoUrl: String = "",
    val sellerUid: String = "",
    /**
     * Denormalised from the seller's `users` document (PRD.md Section 8) so the feed can draw a
     * card without a second read per listing. It is a snapshot: a student who later changes their
     * name keeps the old one on listings already posted, which is an acceptable trade for a feed
     * that is one query instead of one query plus N.
     */
    val sellerName: String = "",
    val status: String = STATUS_AVAILABLE,
    /**
     * Left null when writing: `@ServerTimestamp` makes Firestore stamp it with the server's clock
     * on arrival. That matters because the feed is ordered by this field (FR-LIST-003), and a
     * device with a wrong clock would otherwise pin its listings to the top of everyone's feed -
     * or bury them.
     */
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    companion object {
        const val STATUS_AVAILABLE = "available"
        const val STATUS_SOLD = "sold"
    }
}
