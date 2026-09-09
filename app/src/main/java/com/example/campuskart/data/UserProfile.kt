package com.example.campuskart.data

/**
 * A row of the `users` collection (PRD.md Section 8), written once at signup.
 *
 * Every property has a default because Firestore's `toObject()` builds the object through a
 * no-argument constructor and then sets the fields; a data class with no defaults gives it no
 * such constructor and deserialisation fails at runtime rather than at compile time.
 *
 * [uid] is stored inside the document as well as being its ID. The duplication is what PRD.md
 * Section 8 specifies, and it means a profile stays self-describing once it has been read out of
 * a query result, where the document ID is no longer attached.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val branch: String = "",
    val semester: String = "",
    val whatsappNumber: String = "",
)
