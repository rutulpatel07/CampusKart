package com.example.campuskart.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.campuskart.data.Listing
import com.example.campuskart.data.ListingPhoto
import com.example.campuskart.data.UserProfile
import com.example.campuskart.model.ListingOptions
import com.example.campuskart.ui.components.ChipGroup
import com.example.campuskart.ui.components.FormErrorBanner
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Screen 4 - Post Item, the first half of the app's core flow (PRD.md Section 10, Layer 1).
 *
 * Field order follows the Day 2 mockup: the photo comes before any text, because it is the field
 * most likely to be abandoned if left until last, and from Day 9 it is what ML Kit reads to
 * pre-select the category (FR-AI-001/002).
 *
 * The screen is a pure function of [PostItemUiState]; [PostItemRoute] below owns the ViewModel
 * and the two photo launchers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(
    state: PostItemUiState,
    onFormChange: (ListingForm) -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onPublish: () -> Unit,
    onPostAnother: () -> Unit,
    onGoToFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.published != null) "Listing published" else "Post an item") },
            )
        },
    ) { inner ->
        val published = state.published
        if (published != null) {
            PublishedCard(
                listing = published,
                onPostAnother = onPostAnother,
                onGoToFeed = onGoToFeed,
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
            )
            return@Scaffold
        }

        val form = state.form
        val errors = state.errors
        val enabled = state.enabled

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PhotoField(
                photoUri = state.photoUri,
                error = errors.photo,
                enabled = enabled,
                onTakePhoto = onTakePhoto,
                onChoosePhoto = onChoosePhoto,
            )

            OutlinedTextField(
                value = form.title,
                onValueChange = { onFormChange(form.copy(title = it)) },
                label = { Text("Title") },
                placeholder = { Text("e.g. Engineering Drawing drafter set") },
                supportingText = errors.title?.let { { Text(it) } },
                isError = errors.title != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = { onFormChange(form.copy(description = it)) },
                label = { Text("Description") },
                placeholder = { Text("Condition, what is included, where you can hand it over") },
                supportingText = errors.description?.let { { Text(it) } },
                isError = errors.description != null,
                enabled = enabled,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            CategorySuggestionBanner(state = state.categorySuggestion)

            ChipGroup(
                label = "Category",
                options = ListingOptions.CATEGORIES,
                selected = form.category,
                error = errors.category,
                enabled = enabled,
                onSelect = { onFormChange(form.copy(category = it)) },
            )

            OutlinedTextField(
                value = form.price,
                // Filtered here rather than validated later: a number pad still offers a decimal
                // point and a minus sign on some keyboards, and there is no such thing as a
                // half-rupee listing price.
                onValueChange = { input ->
                    onFormChange(form.copy(price = input.filter(Char::isDigit).take(6)))
                },
                label = { Text("Price") },
                prefix = { Text("Rs ") },
                supportingText = errors.price?.let { { Text(it) } },
                isError = errors.price != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ChipGroup(
                label = "Condition",
                options = ListingOptions.CONDITIONS,
                selected = form.condition,
                error = errors.condition,
                enabled = enabled,
                onSelect = { onFormChange(form.copy(condition = it)) },
            )

            // The profile read that supplies the seller name and branch failing is not a form
            // problem, so it gets
            // its own line rather than the error banner - the form is still fine, it just cannot
            // be submitted yet.
            state.sellerError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FormErrorBanner(message = state.formError)

            Button(
                onClick = onPublish,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.step != null) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(state.step.label, style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Publish listing", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** The non-blocking result of the one-shot ML Kit scan triggered by photo selection. */
@Composable
private fun CategorySuggestionBanner(state: CategorySuggestionState) {
    when (state) {
        CategorySuggestionState.Idle -> Unit

        CategorySuggestionState.Analysing -> Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
                Text("Suggesting a category from your photo...")
            }
        }

        is CategorySuggestionState.Suggested -> Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = "AI suggestion: ${state.suggestion.category}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Based on “${state.suggestion.sourceLabel}” " +
                        "(${(state.suggestion.confidence * 100).toInt()}% confidence). " +
                        "You can choose any category below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        CategorySuggestionState.NoMatch -> SuggestionFallbackBanner(
            "No confident category match. We selected Other; you can change it below.",
        )

        is CategorySuggestionState.Failed -> SuggestionFallbackBanner(
            "${state.message} Other is selected; you can choose any category below.",
        )
    }
}

@Composable
private fun SuggestionFallbackBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/**
 * The photo field, in both of its states. A listing cannot be published without one
 * (FR-LIST-001), so the empty state is a large target rather than a small "attach" affordance.
 */
@Composable
private fun PhotoField(
    photoUri: Uri?,
    error: String?,
    enabled: Boolean,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    if (photoUri == null) {
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
                    OutlinedButton(onClick = onTakePhoto, enabled = enabled) { Text("Camera") }
                    OutlinedButton(onClick = onChoosePhoto, enabled = enabled) { Text("Gallery") }
                }
            }
        }
    } else {
        AsyncImage(
            model = photoUri,
            contentDescription = "The photo you picked for this listing",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onTakePhoto, enabled = enabled) { Text("Retake") }
            TextButton(onClick = onChoosePhoto, enabled = enabled) { Text("Choose another") }
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

/**
 * What replaces the form once the document exists in Firestore.
 *
 * The photo is deliberately loaded back from its Cloudinary URL rather than from the local file
 * that was just uploaded. If it draws, then the upload, the URL that came back, the Firestore
 * write and Coil's network loading have all worked - the entire Day 5 path, confirmed on the
 * device instead of in two web consoles.
 */
@Composable
private fun PublishedCard(
    listing: Listing,
    onPostAnother: () -> Unit,
    onGoToFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoFailed by rememberSaveable(listing.photoUrl) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = listing.photoUrl,
            contentDescription = listing.title,
            contentScale = ContentScale.Crop,
            onError = { photoFailed = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Listing published",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(listing.title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Rs ${listing.price}  -  ${listing.category}  -  ${listing.condition}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (photoFailed) {
            // Worth saying out loud rather than showing a blank box: the listing itself is saved
            // and fine, and this narrows the problem to loading the image back.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "The listing saved, but its photo could not be loaded back.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = listing.photoUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onGoToFeed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("View on Feed", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onPostAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Post another item", style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = "Your listing is live on the Feed and in My items, where you can edit it, " +
                "mark it sold or delete it.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Owns the ViewModel and the two ways a photo gets in.
 *
 * Neither needs a runtime permission, which is the reason this pairing was chosen over an in-app
 * CameraX capture screen:
 *
 * - `PickVisualMedia` is the system photo picker. It runs in its own process and hands back a URI
 *   for exactly the image the user chose, so the app never asks for - or gets - access to the
 *   whole gallery.
 * - `TakePicture` is an implicit intent to whatever camera app is installed. CAMERA permission is
 *   only required of an app that declares it in its manifest, and this one deliberately does not:
 *   it never touches the camera itself, it asks another app to. (This is the same implicit-intent
 *   mechanism as the WhatsApp hand-off on Day 6 - PRD.md Section 11.)
 */
@Composable
fun PostItemRoute(
    onGoToFeed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PostItemViewModel = viewModel(),
) {
    val context = LocalContext.current

    // Survives configuration changes: TakePicture only reports whether the capture succeeded, so
    // the URI it was told to write to has to still be here when the result comes back - and a
    // rotation while the camera app is open would otherwise lose it.
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved -> viewModel.onPhotoPicked(pendingCameraUri.takeIf { saved }) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPhotoPicked(uri) }

    PostItemScreen(
        state = viewModel.uiState,
        onFormChange = viewModel::onFormChange,
        onTakePhoto = {
            val uri = ListingPhoto.newCameraOutputUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        },
        onChoosePhoto = {
            pickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onPublish = viewModel::publish,
        onPostAnother = viewModel::startAnother,
        onGoToFeed = onGoToFeed,
        modifier = modifier,
    )
}

private val filledForm = ListingForm(
    title = "Engineering Drawing drafter set",
    description = "Mini-drafter, set squares and compass box. Used for one semester, all parts " +
        "present. Can hand over on campus.",
    category = "Drafter",
    price = "350",
    condition = "Good",
)

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun PostItemEmptyPreview() {
    CampusKartTheme {
        PostItemScreen(
            state = PostItemUiState(seller = PreviewSeller),
            onFormChange = {},
            onTakePhoto = {},
            onChoosePhoto = {},
            onPublish = {},
            onPostAnother = {},
            onGoToFeed = {},
        )
    }
}

/** Every field failing at once - the layout that is hardest to get right and rarest to hit. */
@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun PostItemErrorPreview() {
    CampusKartTheme {
        PostItemScreen(
            state = PostItemUiState(
                form = ListingForm(title = "Book"),
                errors = ListingValidation.validate(ListingForm(title = "Book"), hasPhoto = false),
                formError = "Could not upload the photo. Check your connection and try again.",
            ),
            onFormChange = {},
            onTakePhoto = {},
            onChoosePhoto = {},
            onPublish = {},
            onPostAnother = {},
            onGoToFeed = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun PostItemPublishingPreview() {
    CampusKartTheme {
        PostItemScreen(
            state = PostItemUiState(
                form = filledForm,
                photoUri = Uri.EMPTY,
                step = PublishStep.UPLOADING,
                seller = PreviewSeller,
            ),
            onFormChange = {},
            onTakePhoto = {},
            onChoosePhoto = {},
            onPublish = {},
            onPostAnother = {},
            onGoToFeed = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun PostItemPublishedPreview() {
    CampusKartTheme {
        PostItemScreen(
            state = PostItemUiState(
                published = Listing(
                    id = "aB3xY9",
                    title = "Engineering Drawing drafter set",
                    category = "Drafter",
                    price = 350,
                    condition = "Good",
                    sellerName = PreviewSeller.name,
                    sellerBranch = PreviewSeller.branch,
                ),
            ),
            onFormChange = {},
            onTakePhoto = {},
            onChoosePhoto = {},
            onPublish = {},
            onPostAnother = {},
            onGoToFeed = {},
        )
    }
}

/** Preview-only stand-in for the signed-in user's `users` document. */
private val PreviewSeller = UserProfile(
    name = "Rutul Patel",
    branch = "CE",
    semester = "5",
    whatsappNumber = "9876543210",
)
