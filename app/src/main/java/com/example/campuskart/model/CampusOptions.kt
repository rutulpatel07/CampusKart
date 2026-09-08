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
