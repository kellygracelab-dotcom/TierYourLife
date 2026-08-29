package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

private const val COVER_ASPECT = 3f / 4f
private val CANDIDATE_GRID_MAX_HEIGHT = 320.dp

/**
 * A cover taken from the list's own cards is a web address, so it travels with
 * the published snapshot. One from the gallery lives on this phone only; the
 * sheet says so rather than letting the community quietly show something else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoverSheet(
    list: TierList,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val candidates = list.tiers
        .flatMap { it.items }
        .mapNotNull { it.imageUrl }
        .filter { it.startsWith("https://") }
        .distinct()

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onSelect(uri.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .testTag(TierDetailTestTags.COVER_SHEET),
        ) {
            Text(
                text = stringResource(R.string.list_settings_cover),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (candidates.isEmpty()) {
                        R.string.list_settings_cover_no_cards
                    } else {
                        R.string.list_settings_cover_body
                    },
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (candidates.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = CANDIDATE_GRID_MAX_HEIGHT),
                ) {
                    items(candidates) { url ->
                        CoverCandidate(
                            url = url,
                            selected = url == list.coverImageUrl,
                            onClick = { onSelect(url) },
                        )
                    }
                }
            }

            TextButton(
                onClick = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag(TierDetailTestTags.COVER_FROM_GALLERY),
            ) {
                Text(stringResource(R.string.list_settings_cover_gallery))
            }
            Text(
                text = stringResource(R.string.list_settings_cover_gallery_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            if (list.coverImageUrl != null) {
                TextButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(TierDetailTestTags.COVER_CLEAR),
                ) {
                    Text(stringResource(R.string.list_settings_cover_none))
                }
            }
        }
    }
}

@Composable
private fun CoverCandidate(url: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .aspectRatio(COVER_ASPECT)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
