package com.example.campuskart.ui.feed

import com.example.campuskart.data.Listing

/**
 * The Home Feed's category filter and title search (FR-LIST-004/005), as plain Kotlin.
 *
 * Both run over the list the feed has *already* loaded rather than going back to Firestore, and
 * that is a limitation rather than a shortcut. Firestore has no full-text search at all - the
 * documented workaround is to pay for a third-party search service such as Algolia - so a
 * keyword search against titles can only be done on the device. Filtering by category could be a
 * real query, but making it one would mean a network round trip every time a chip is tapped, on
 * a feed small enough to filter instantly in memory. So both are done the same way, here.
 *
 * What that costs: the search only ever sees listings that are already downloaded. For a campus
 * marketplace of tens to low hundreds of items that is the whole feed, so it is invisible - but
 * it would not hold at ten thousand listings, and README "Challenges faced" says so.
 *
 * No Android or Firebase imports, so the rules are covered by ordinary JVM unit tests (see
 * FeedFilterTest) - the same arrangement as [com.example.campuskart.ui.post.ListingValidation].
 */
object FeedFilter {

    /** The "All" chip: no category narrowing at all. Not one of `ListingOptions.CATEGORIES`. */
    const val ALL_CATEGORIES = ""

    /**
     * Narrows [listings] by category and then by title keywords.
     *
     * Order does not matter to the result - both are `filter` calls - but doing the category
     * first means the string matching runs over fewer rows.
     */
    fun apply(
        listings: List<Listing>,
        query: String = "",
        category: String = ALL_CATEGORIES,
    ): List<Listing> = listings
        .filter { category == ALL_CATEGORIES || it.category == category }
        .filter { matchesQuery(it.title, query) }

    /**
     * Whether a title matches what was typed.
     *
     * Every whitespace-separated word in the query has to appear somewhere in the title, in any
     * order, ignoring case. That is deliberately looser than a straight `contains`: a student
     * looking for a drafter set types "drafter set", "set drafter" or "drafter  set" more or
     * less at random, and only the first of those would match a title of "Drafter set (mini)"
     * under a single-substring test.
     *
     * It is also looser than word-boundary matching, so "calc" finds "Casio calculator". For a
     * list this size, finding too much is a far smaller problem than finding nothing.
     *
     * A blank query matches everything, which is what makes an empty search box mean "no filter"
     * rather than "no results".
     */
    fun matchesQuery(title: String, query: String): Boolean {
        val terms = query.trim().split(WHITESPACE).filter(String::isNotEmpty)
        if (terms.isEmpty()) return true
        return terms.all { term -> title.contains(term, ignoreCase = true) }
    }

    private val WHITESPACE = Regex("\\s+")
}
