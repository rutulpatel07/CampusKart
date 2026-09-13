package com.example.campuskart.ui.mylistings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campuskart.data.Listing
import com.example.campuskart.model.ListingOptions
import com.example.campuskart.ui.components.ChipGroup
import com.example.campuskart.ui.components.FormErrorBanner
import com.example.campuskart.ui.components.ListingImage
import com.example.campuskart.ui.post.ListingForm
import com.example.campuskart.ui.post.ListingValidation
import com.example.campuskart.ui.theme.CampusKartTheme

/**
 * Editing a listing already posted (FR-LIST-007), reached from the Edit action on My Listings.
 *
 * The same five fields as Post Item, in the same order, with the same rules - so a seller who
 * has posted once already knows this screen. What is missing is the photo picker: the photo sits
 * at the top, read-only, with a line saying why.
 *
 * Pushed over My Listings as its own destination rather than opened as a bottom sheet, because
 * five fields and two chip rows do not fit above a keyboard on a phone.
 *
 * The screen is a pure function of [EditListingUiState]; [EditListingRoute] owns the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    state: EditListingUiState,
    onFormChange: (ListingForm) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Edit listing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.missing -> LoadFailure(
                    title = "This listing is gone",
                    detail = "It was deleted, so there is nothing left to edit.",
                    actionLabel = "Back to my listings",
                    onAction = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.loadError != null -> LoadFailure(
                    title = "Could not open this listing",
                    detail = state.loadError,
                    actionLabel = "Try again",
                    onAction = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.listing != null -> EditForm(
                    state = state,
                    listing = state.listing,
                    onFormChange = onFormChange,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun EditForm(
    state: EditListingUiState,
    listing: Listing,
    onFormChange: (ListingForm) -> Unit,
    onSave: () -> Unit,
) {
    val form = state.form
    val errors = state.errors
    val enabled = state.enabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ListingImage(
            url = listing.photoUrl,
            contentDescription = "The photo on this listing",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            text = "The photo cannot be changed. To use a different one, delete this listing and " +
                "post it again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = form.title,
            onValueChange = { onFormChange(form.copy(title = it)) },
            label = { Text("Title") },
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
            supportingText = errors.description?.let { { Text(it) } },
            isError = errors.description != null,
            enabled = enabled,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

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
            // Filtered exactly as on Post Item: a number pad still offers a decimal point and a
            // minus sign on some keyboards, and there is no such thing as a half-rupee price.
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

        FormErrorBanner(message = state.formError)

        Button(
            // Disabled until something has actually changed, so an Edit tapped by mistake costs
            // a Back press rather than a pointless write.
            onClick = onSave,
            enabled = enabled && state.dirty,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(12.dp))
                Text("Saving changes...", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("Save changes", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** The two ways this screen can have nothing to edit, with the one thing to do about each. */
@Composable
private fun LoadFailure(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) { Text(actionLabel) }
    }
}

/**
 * Owns the ViewModel and leaves the screen once the write has landed.
 *
 * There is no "saved!" confirmation here on purpose. Popping straight back to My Listings shows
 * the edited card with the new title and price on it, which is a better confirmation than any
 * message would be - and it is one tap fewer.
 */
@Composable
fun EditListingRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditListingViewModel = viewModel(),
) {
    val state = viewModel.uiState

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    EditListingScreen(
        state = state,
        onFormChange = viewModel::onFormChange,
        onSave = viewModel::save,
        onBack = onDone,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

private val PreviewListing = Listing(
    id = "1",
    title = "Engineering Drawing drafter set",
    description = "Mini-drafter, set squares and compass box. Used for one semester, all parts " +
        "present. Can hand over on campus.",
    category = "Drafter",
    price = 350,
    condition = "Good",
)

private val PreviewForm = ListingForm(
    title = PreviewListing.title,
    description = PreviewListing.description,
    category = PreviewListing.category,
    price = PreviewListing.price.toString(),
    condition = PreviewListing.condition,
)

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun EditListingPreview() {
    CampusKartTheme {
        EditListingScreen(
            state = EditListingUiState(
                loading = false,
                listing = PreviewListing,
                form = PreviewForm,
            ),
            onFormChange = {},
            onSave = {},
            onBack = {},
            onRetry = {},
        )
    }
}

/** A price cleared to nothing and a title cut too short - the two easiest ways to break an edit. */
@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun EditListingErrorPreview() {
    CampusKartTheme {
        val broken = PreviewForm.copy(title = "Set", price = "")
        EditListingScreen(
            state = EditListingUiState(
                loading = false,
                listing = PreviewListing,
                form = broken,
                errors = ListingValidation.validate(broken, hasPhoto = true),
                formError = "Could not save that change. Please try again.",
            ),
            onFormChange = {},
            onSave = {},
            onBack = {},
            onRetry = {},
        )
    }
}
