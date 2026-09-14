package com.example.campuskart.data

import android.content.Context
import android.net.Uri
import com.example.campuskart.ui.post.CategorySuggestion
import com.example.campuskart.ui.post.CategorySuggestionMapper
import com.example.campuskart.ui.post.DetectedImageLabel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

sealed interface CategorySuggestionOutcome {
    data class Suggested(val suggestion: CategorySuggestion) : CategorySuggestionOutcome
    data object NoMatch : CategorySuggestionOutcome
    data class Failure(val message: String) : CategorySuggestionOutcome
}

/** One-shot on-device image labeling for the Post Item photo (FR-AI-001). */
object MlKitCategorySuggester {

    suspend fun suggest(context: Context, uri: Uri): CategorySuggestionOutcome {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: Exception) {
            return CategorySuggestionOutcome.Failure("This photo could not be analysed.")
        }

        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(CategorySuggestionMapper.MIN_CONFIDENCE)
                .build(),
        )
        return try {
            val labels = labeler.process(image).await().map {
                DetectedImageLabel(text = it.text, confidence = it.confidence)
            }
            CategorySuggestionMapper.suggest(labels)?.let(CategorySuggestionOutcome::Suggested)
                ?: CategorySuggestionOutcome.NoMatch
        } catch (_: Exception) {
            // AI is a convenience only. The seller can still post normally after a failure.
            CategorySuggestionOutcome.Failure("Could not suggest a category for this photo.")
        } finally {
            labeler.close()
        }
    }
}
