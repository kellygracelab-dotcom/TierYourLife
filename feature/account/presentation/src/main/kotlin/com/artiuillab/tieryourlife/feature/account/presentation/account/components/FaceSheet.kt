package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags
import com.artiuillab.tieryourlife.feature.account.presentation.account.face.sameFace

private val CHOICE_SIZE = 64.dp
private val TILE_WIDTH = 76.dp
private val CHECK_SIZE = 22.dp

/**
 * Letter · Your photo · Upload · the cards, one row, the check on the face worn
 * (FACE-PHOTO-2026-11-19). The uploaded photo keeps its tile whatever is worn,
 * so it is put back with a tap; only a new upload or "Remove your photo" takes it away.
 */
@Composable
internal fun FaceSheet(
    current: String?,
    ownPhoto: String?,
    cardImages: List<String>,
    name: String?,
    onDismiss: () -> Unit,
    onChoose: (String?) -> Unit,
    onPickPhoto: () -> Unit,
    onWearOwnPhoto: () -> Unit = {},
    onRemoveOwnPhoto: () -> Unit = {},
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        width = SheetWidth.Choosing,
        maxHeight = 560.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .testTag(AccountTestTags.FACE_SHEET),
        ) {
            Text(
                text = stringResource(R.string.account_face_title),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (cardImages.isEmpty()) R.string.account_face_no_cards else R.string.account_face_body,
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val letter = stringResource(R.string.account_face_letter)
            val yourPhoto = stringResource(R.string.account_face_your_photo)
            val upload = stringResource(R.string.account_face_gallery)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    FaceTile(letter, selected = current == null, onClick = { onChoose(null) }, testTag = AccountTestTags.FACE_LETTER) {
                        Avatar(photoUrl = null, name = name, size = CHOICE_SIZE)
                    }
                }
                if (ownPhoto != null) {
                    item {
                        FaceTile(yourPhoto, selected = sameFace(current, ownPhoto), onClick = onWearOwnPhoto, testTag = AccountTestTags.FACE_OWN_PHOTO) {
                            FacePicture(ownPhoto)
                        }
                    }
                }
                item {
                    FaceTile(upload, selected = null, onClick = onPickPhoto, testTag = AccountTestTags.FACE_GALLERY) {
                        UploadIcon(28.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(cardImages) { index, url ->
                    FaceTile(
                        caption = null,
                        selected = sameFace(url, current),
                        onClick = { onChoose(url) },
                        testTag = AccountTestTags.faceChoice(url),
                        // QA D17: a card had nothing for a screen reader to say.
                        description = stringResource(R.string.account_face_card, index + 1),
                    ) {
                        FacePicture(url)
                    }
                }
            }

            if (ownPhoto != null) {
                TextButton(
                    onClick = onRemoveOwnPhoto,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 8.dp)
                        .testTag(AccountTestTags.FACE_REMOVE_PHOTO),
                ) {
                    Text(
                        text = stringResource(R.string.account_face_remove_photo),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.account_face_note),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = if (ownPhoto != null) 4.dp else 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun FacePicture(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(CHOICE_SIZE).clip(CircleShape),
    )
}

/** [selected] null: a tile that does something rather than being worn, so never checked. */
@Composable
private fun FaceTile(
    caption: String?,
    selected: Boolean?,
    onClick: () -> Unit,
    testTag: String,
    description: String? = caption,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .width(TILE_WIDTH)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = if (selected == null) Role.Button else Role.RadioButton, onClick = onClick)
            .semantics {
                if (selected != null) this.selected = selected
                if (description != null) contentDescription = description
            }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier
                    .size(CHOICE_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (selected == true) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
            if (selected == true) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(CHECK_SIZE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    CheckIcon(16.dp, MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
