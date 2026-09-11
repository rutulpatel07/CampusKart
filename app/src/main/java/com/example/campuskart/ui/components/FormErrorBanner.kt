package com.example.campuskart.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The banner that carries a whole-form failure: a rejected password, an email already in use, a
 * Cloudinary upload that did not go through, a dead network. Per-field problems are reported in
 * that field's own supporting text instead.
 *
 * It occupies no space when [message] is null, and animates in rather than appearing instantly,
 * because it is inserted above the submit button - a silent insertion would shift the button out
 * from under a finger that is already on its way down.
 *
 * Written on Day 4 for Login and Signup; moved here on Day 5, when Post Item turned out to need
 * exactly the same thing and "Auth" had stopped being an accurate name for it.
 */
@Composable
fun FormErrorBanner(message: String?, modifier: Modifier = Modifier) {
    // Held after the message clears, so the banner still has text to draw on the way out - the
    // exit animation outlives the state that caused it.
    var lastMessage by remember { mutableStateOf(message) }
    if (message != null) lastMessage = message

    AnimatedVisibility(visible = message != null, modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text(
                    text = lastMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
