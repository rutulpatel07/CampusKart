package com.example.campuskart.ui.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggestionMapperTest {

    @Test
    fun `recognised labels map to the fixed campus categories`() {
        assertEquals("Books", suggest("book")?.category)
        assertEquals("Drafter", suggest("ruler")?.category)
        assertEquals("Lab Coat", suggest("coat")?.category)
        assertEquals("Calculator", suggest("calculator")?.category)
        assertEquals("Cycle", suggest("bicycle")?.category)
        assertEquals("Electronics", suggest("laptop")?.category)
    }

    @Test
    fun `highest-confidence recognised label wins`() {
        val result = CategorySuggestionMapper.suggest(
            listOf(
                DetectedImageLabel("book", 0.78f),
                DetectedImageLabel("bicycle", 0.92f),
            ),
        )
        assertEquals("Cycle", result?.category)
        assertEquals("bicycle", result?.sourceLabel)
    }

    @Test
    fun `unknown or weak labels yield no suggestion`() {
        assertNull(CategorySuggestionMapper.suggest(listOf(DetectedImageLabel("table", 0.99f))))
        assertNull(CategorySuggestionMapper.suggest(listOf(DetectedImageLabel("book", 0.69f))))
    }

    private fun suggest(label: String) = CategorySuggestionMapper.suggest(
        listOf(DetectedImageLabel(label, 0.90f)),
    )
}
