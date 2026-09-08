package com.example.campuskart.ui.mockups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 4 - Post Item. The photo comes first, before any text field, for two reasons: it is the
 * field most likely to be abandoned if left to last, and from Day 9 it is what ML Kit reads to
 * pre-select the category (FR-AI-001/002).
 *
 * @param hasPhoto renders the state after a photo has been chosen - including the space the
 *   Day 9 category suggestion will occupy. Laid out now so the AI feature drops into an existing
 *   slot instead of forcing the form to be redesigned later.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostItemMockup(
    hasPhoto: Boolean = false,
    modifier: Modifier = Modifier,
    showTabBar: Boolean = true,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Post an item") }) },
        // Hidden when the Day 3 navigation shell hosts this sketch, because the shell draws
        // the real bottom bar itself - otherwise the screen would show two of them.
        bottomBar = { if (showTabBar) MockBottomBar(MockTab.POST) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (hasPhoto) {
                PhotoPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {}) { Text("Retake") }
                    TextButton(onClick = {}) { Text("Choose another") }
                }
                CategorySuggestionBanner(suggested = "Drafter")
            } else {
                PhotoPicker()
            }

            OutlinedTextField(
                value = if (hasPhoto) "Engineering Drawing drafter set" else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Title") },
                placeholder = { Text("e.g. Engineering Drawing drafter set") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = if (hasPhoto) MockDetailDescription else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Description") },
                placeholder = { Text("Condition, what is included, where you can hand it over") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MockCategories.forEach { category ->
                    FilterChip(
                        selected = hasPhoto && category == "Drafter",
                        onClick = {},
                        label = { Text(category) },
                    )
                }
            }

            OutlinedTextField(
                value = if (hasPhoto) "350" else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Price") },
                prefix = { Text("₹ ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("Condition")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MockConditions.forEach { condition ->
                    FilterChip(
                        selected = hasPhoto && condition == "Good",
                        onClick = {},
                        label = { Text(condition) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {},
                enabled = hasPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Publish listing", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Empty state of the photo field. A listing cannot be published without one (FR-LIST-001). */
@Composable
private fun PhotoPicker() {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text("Add a photo", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Every listing needs one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {}) { Text("Camera") }
                OutlinedButton(onClick = {}) { Text("Gallery") }
            }
        }
    }
}

/**
 * Placeholder for the Day 9 ML Kit feature (Layer 2.5). It sits directly between the photo and
 * the category chips so the cause and the effect are visible at once, and it is only ever a
 * suggestion - the chips below stay fully editable (FR-AI-003).
 */
@Composable
private fun CategorySuggestionBanner(suggested: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Looks like $suggested",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "Suggested from the photo, on your device. Change it below if wrong.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@ScreenMockupPreview
@Composable
private fun PostItemEmptyMockupPreview() {
    CampusKartTheme { PostItemMockup(hasPhoto = false) }
}

@ScreenMockupPreview
@Composable
private fun PostItemFilledMockupPreview() {
    CampusKartTheme { PostItemMockup(hasPhoto = true) }
}
