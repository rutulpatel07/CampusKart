package com.example.campuskart.ui.feed

import com.example.campuskart.data.Listing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Home Feed's search and category filter (FR-LIST-004/005).
 *
 * [FeedFilter] has no Android or Firebase imports, so these run on the JVM in a second rather
 * than on an emulator - the same arrangement as AuthValidationTest and ListingValidationTest.
 */
class FeedFilterTest {

    private val drafter = listing("1", "Engineering Drawing drafter set", "Drafter")
    private val calculator = listing("2", "Casio FX-991EX scientific calculator", "Calculator")
    private val labCoat = listing("3", "Lab coat, size M, barely used", "Lab Coat")
    private val book = listing("4", "Data Structures textbook, 3rd edition", "Books")

    private val feed = listOf(drafter, calculator, labCoat, book)

    private fun listing(id: String, title: String, category: String) =
        Listing(id = id, title = title, category = category)

    // ---- no filters ----

    @Test
    fun `no filters returns everything`() {
        assertEquals(feed, FeedFilter.apply(feed))
    }

    @Test
    fun `blank query returns everything`() {
        assertEquals(feed, FeedFilter.apply(feed, query = "   "))
    }

    @Test
    fun `filtering preserves the order it was given`() {
        // The feed arrives newest-first from Firestore, and narrowing it must not reshuffle it.
        // "d" is in three of the four titles - Casio's is the one without it.
        assertEquals(listOf(drafter, labCoat, book), FeedFilter.apply(feed, query = "d"))
    }

    // ---- search (FR-LIST-005) ----

    @Test
    fun `search matches a word in the title`() {
        assertEquals(listOf(calculator), FeedFilter.apply(feed, query = "calculator"))
    }

    @Test
    fun `search ignores case`() {
        assertEquals(listOf(calculator), FeedFilter.apply(feed, query = "CASIO"))
    }

    @Test
    fun `search matches a partial word`() {
        // "calc" finding "calculator" is the point: students type prefixes, not whole words.
        assertEquals(listOf(calculator), FeedFilter.apply(feed, query = "calc"))
    }

    @Test
    fun `search requires every term but in any order`() {
        assertTrue(FeedFilter.matchesQuery("Engineering Drawing drafter set", "drafter set"))
        assertTrue(FeedFilter.matchesQuery("Engineering Drawing drafter set", "set drafter"))
        assertFalse(FeedFilter.matchesQuery("Engineering Drawing drafter set", "drafter compass"))
    }

    @Test
    fun `search collapses extra whitespace between terms`() {
        assertTrue(FeedFilter.matchesQuery("Lab coat, size M", "  lab    coat  "))
    }

    @Test
    fun `search that matches nothing returns an empty list`() {
        assertTrue(FeedFilter.apply(feed, query = "hovercraft").isEmpty())
    }

    @Test
    fun `search does not look at the category`() {
        // FR-LIST-005 says titles. The chips are how a category is searched, and a query that
        // quietly matched both would make the two filters impossible to reason about together.
        assertTrue(FeedFilter.apply(feed, query = "Books").isEmpty())
    }

    // ---- category chips (FR-LIST-004) ----

    @Test
    fun `category narrows to that category alone`() {
        assertEquals(listOf(labCoat), FeedFilter.apply(feed, category = "Lab Coat"))
    }

    @Test
    fun `the All chip does not narrow anything`() {
        assertEquals(feed, FeedFilter.apply(feed, category = FeedFilter.ALL_CATEGORIES))
    }

    @Test
    fun `category matching is exact`() {
        // The chips come from the same fixed list a listing is posted under, so there is no case
        // or partial matching to do here - and doing it would make "Books" match "Lab Coat" the
        // moment someone added a category containing another one's name.
        assertTrue(FeedFilter.apply(feed, category = "books").isEmpty())
    }

    // ---- the two together ----

    @Test
    fun `search and category both have to match`() {
        assertEquals(
            listOf(calculator),
            FeedFilter.apply(feed, query = "casio", category = "Calculator"),
        )
        assertTrue(FeedFilter.apply(feed, query = "casio", category = "Books").isEmpty())
    }
}
