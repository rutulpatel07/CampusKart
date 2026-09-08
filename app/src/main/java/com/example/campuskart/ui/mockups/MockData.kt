package com.example.campuskart.ui.mockups

/**
 * Day 2 mockup package - throwaway.
 *
 * These are low/mid-fidelity Compose sketches of all six screens, built so the layout can be
 * reviewed before any real screen is written. Nothing here talks to Firebase, Cloudinary or the
 * navigation graph: every value is hard-coded and every button does nothing. The real screens
 * replace these from Day 3 onwards, and this package is deleted on Day 10.
 */

/** Stand-in for a `listings` document (PRD.md Section 8). Not the real model class. */
data class MockListing(
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val sellerName: String,
    val sellerBranch: String,
    val sellerSemester: String,
    val sold: Boolean = false,
)

/** The app's fixed category list, per PRD.md Section 8 and SRS FR-AI-002. */
val MockCategories = listOf(
    "Books", "Drafter", "Lab Coat", "Calculator", "Cycle", "Electronics", "Other",
)

/** The app's fixed condition list, per PRD.md Section 8. */
val MockConditions = listOf("New", "Good", "Used")

val MockFeed = listOf(
    MockListing(
        title = "Engineering Drawing drafter set",
        price = "350",
        category = "Drafter",
        condition = "Good",
        sellerName = "Aarav Shah",
        sellerBranch = "CE",
        sellerSemester = "5",
    ),
    MockListing(
        title = "Casio FX-991EX scientific calculator",
        price = "800",
        category = "Calculator",
        condition = "Good",
        sellerName = "Priya Mehta",
        sellerBranch = "IT",
        sellerSemester = "3",
    ),
    MockListing(
        title = "Lab coat, size M, barely used",
        price = "200",
        category = "Lab Coat",
        condition = "New",
        sellerName = "Devansh Patel",
        sellerBranch = "CE-AI",
        sellerSemester = "5",
    ),
    MockListing(
        title = "Data Structures textbook (Sem 3)",
        price = "180",
        category = "Books",
        condition = "Used",
        sellerName = "Nidhi Joshi",
        sellerBranch = "CE",
        sellerSemester = "7",
    ),
)

/** The listing opened on the Item Detail mockup. */
val MockDetailListing = MockFeed.first()

const val MockDetailDescription =
    "Full drafter set from Engineering Drawing - mini-drafter, set squares, compass box and a " +
        "roll of drawing sheets. Used for one semester only, all parts present and the scale " +
        "markings are still clean. Selling because the subject is done. Can hand over on campus."

/** The signed-in user shown on the Profile and Signup mockups. */
object MockProfile {
    const val NAME = "Rutul Patel"
    const val INITIALS = "RP"
    const val EMAIL = "rutul.patel@example.com"
    const val BRANCH = "CE"
    const val SEMESTER = "5"
    const val WHATSAPP = "+91 98765 43210"
}
