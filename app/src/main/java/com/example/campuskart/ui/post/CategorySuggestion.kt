package com.example.campuskart.ui.post

/** A model label kept free of Android and ML Kit so the mapping has ordinary JVM tests. */
data class DetectedImageLabel(val text: String, val confidence: Float)

/** The one category hint shown to a seller after their photo has been analysed. */
data class CategorySuggestion(
    val category: String,
    val sourceLabel: String,
    val confidence: Float,
)

/** Converts ML Kit's broad vocabulary into CampusKart's fixed categories. */
object CategorySuggestionMapper {

    const val MIN_CONFIDENCE = 0.70f

    fun suggest(
        labels: List<DetectedImageLabel>,
        minConfidence: Float = MIN_CONFIDENCE,
    ): CategorySuggestion? = labels
        .asSequence()
        .filter { it.confidence >= minConfidence }
        .sortedByDescending { it.confidence }
        .mapNotNull { label ->
            categoryFor(label.text)?.let { category ->
                CategorySuggestion(category, label.text, label.confidence)
            }
        }
        .firstOrNull()

    private fun categoryFor(rawLabel: String): String? {
        val label = rawLabel.trim().lowercase()
        return when {
            label.containsAny("book", "notebook", "publication", "reading") -> "Books"
            label.containsAny("calculator", "abacus") -> "Calculator"
            label.containsAny("bicycle", "bike", "cycle") -> "Cycle"
            label.containsAny("lab coat", "coat", "jacket") -> "Lab Coat"
            label.containsAny("drafter", "drafting", "ruler", "compass", "set square") -> "Drafter"
            label.containsAny(
                "electronics", "electronic", "computer", "laptop", "keyboard", "mouse",
                "monitor", "mobile phone", "cell phone", "headphones", "camera", "television",
            ) -> "Electronics"
            else -> null
        }
    }

    private fun String.containsAny(vararg words: String): Boolean = words.any(::contains)
}
