package com.example.campuskart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * A listing's Cloudinary photo, as drawn on the feed card and at the top of Item Detail.
 *
 * The two screens share this for one reason that is not "avoiding duplication": a photo that has
 * not arrived yet, or cannot be loaded at all, has to leave the layout exactly the size it will
 * be once it has. Coil draws nothing while a request is in flight, so without the tinted box
 * underneath, every feed card would resize itself as the user scrolled past it.
 *
 * There is no retry and no error text. A listing whose photo fails to load is still worth
 * showing - the title, price and seller are what the buyer decides on - so the failure is drawn
 * as a quiet placeholder rather than as an error the user is asked to do something about.
 */
@Composable
fun ListingImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Tracked so the fallback icon is drawn only on an actual failure, and not left showing
        // underneath a photo that has since loaded over the top of it.
        var failed by remember(url) { mutableStateOf(false) }

        if (url.isBlank() || failed) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onState = { state -> failed = state is AsyncImagePainter.State.Error },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
