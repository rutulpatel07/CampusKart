package com.example.campuskart.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A labelled row of single-choice chips: Category and Condition on both Post Item and Edit
 * Listing.
 *
 * Written for Post Item on Day 5 and moved here on Day 7, when the Edit Listing screen turned
 * out to need the same two rows with the same validation behaviour. Keeping one copy matters
 * beyond tidiness - the two screens edit the same five fields, and a chip row that behaved
 * differently depending on which one you reached it from would be a bug nobody would think to
 * look for.
 *
 * @param selected the chosen option, or "" for none.
 * @param error a message to show under the row; also turns the label red.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    error: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (error != null) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    // Re-tapping the selected chip clears it rather than doing nothing, so a
                    // mis-tap can be undone without there being a "none" chip in the row.
                    onClick = { onSelect(if (option == selected) "" else option) },
                    enabled = enabled,
                    label = { Text(option) },
                )
            }
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
