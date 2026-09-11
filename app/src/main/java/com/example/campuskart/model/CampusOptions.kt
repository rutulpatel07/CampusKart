package com.example.campuskart.model

/**
 * The fixed option lists a user picks from when describing themselves.
 *
 * Branch and semester are self-declared (PRD.md Section 8), but they are collected as dropdowns
 * rather than free text: the Home Feed shows the seller's branch on every card, so "CE", "ce"
 * and "Computer Engg" typed by three different students would read as three different branches.
 * A closed list keeps that display - and any later filtering - consistent.
 */
object CampusOptions {

    /**
     * The branches this course runs for (CE / IT / CE-AI, per the 2CEIT5PE18 course code), plus
     * an escape hatch so a student from anywhere else can still sign up - signup is deliberately
     * open to everyone (PRD.md Section 6).
     */
    val BRANCHES = listOf("CE", "IT", "CE-AI", "Other")

    /** A B.Tech degree is eight semesters. */
    val SEMESTERS = (1..8).map(Int::toString)
}

/**
 * The fixed lists a listing is described with (PRD.md Section 8).
 *
 * Both are closed lists rendered as chips rather than free text, for the same reason branch is:
 * the Home Feed filters by category from Day 7 (FR-LIST-004), and a filter cannot work against
 * values a hundred students typed by hand. The category list is also the exact set ML Kit's
 * labels have to be mapped onto on Day 9 (FR-AI-002), so it is fixed here once and read from
 * everywhere else.
 */
object ListingOptions {

    val CATEGORIES = listOf(
        "Books", "Drafter", "Lab Coat", "Calculator", "Cycle", "Electronics", "Other",
    )

    val CONDITIONS = listOf("New", "Good", "Used")

    /** Where an unrecognised photo lands on Day 9 (FR-AI-004). Must be one of [CATEGORIES]. */
    const val FALLBACK_CATEGORY = "Other"
}
