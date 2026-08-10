package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

@Composable
internal fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, photoUris: List<String>) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val pickPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) photoUris = uris.map { it.toString() } }

    ManualEntryDialogContent(
        title = title,
        onTitleChange = { title = it },
        photoUris = photoUris,
        onChoosePhotoClick = {
            pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhotos = { photoUris = emptyList() },
        onDismiss = onDismiss,
        onSave = { onSave(title.trim(), photoUris) },
    )
}

@Composable
internal fun ManualEntryDialogContent(
    title: String,
    onTitleChange: (String) -> Unit,
    photoUris: List<String>,
    onChoosePhotoClick: () -> Unit,
    onRemovePhotos: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isBatch = photoUris.size > 1
    val canSave = title.isNotBlank() || photoUris.isNotEmpty()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TierDetailTestTags.MANUAL_ENTRY_DIALOG),
        title = {
            Text(
                text = stringResource(R.string.manual_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.manual_dialog_body),
                    style = TierYourLifeType.current.captionUnderTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                if (!isBatch) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(R.string.manual_dialog_field_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag(TierDetailTestTags.MANUAL_ENTRY_NAME_FIELD),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PhotoFrame(
                        photoUri = photoUris.firstOrNull(),
                        onRemove = onRemovePhotos,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatch) {
                                pluralStringResource(
                                    R.plurals.manual_dialog_photos_chosen,
                                    photoUris.size,
                                    photoUris.size,
                                )
                            } else {
                                stringResource(R.string.manual_dialog_photo_optional)
                            },
                            style = TierYourLifeType.current.captionUnderTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        AssistChip(
                            onClick = onChoosePhotoClick,
                            label = {
                                Text(
                                    text = stringResource(
                                        if (photoUris.isEmpty()) {
                                            R.string.manual_dialog_choose_photo
                                        } else {
                                            R.string.manual_dialog_replace_photo
                                        },
                                    ),
                                    style = TierYourLifeType.current.chipText,
                                )
                            },
                            leadingIcon = {
                                PhotoLibraryIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag(TierDetailTestTags.MANUAL_ENTRY_CHOOSE_PHOTO),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.testTag(TierDetailTestTags.MANUAL_ENTRY_SAVE),
            ) {
                Text(
                    text = if (isBatch) {
                        pluralStringResource(
                            R.plurals.manual_dialog_add_many,
                            photoUris.size,
                            photoUris.size,
                        )
                    } else {
                        stringResource(R.string.manual_dialog_add)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TierDetailTestTags.MANUAL_ENTRY_CANCEL),
            ) { Text(stringResource(R.string.manual_dialog_cancel)) }
        },
    )

    LaunchedEffect(isBatch) { if (!isBatch) focusRequester.requestFocus() }
}

@Composable
private fun PhotoFrame(photoUri: String?, onRemove: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    val chosenDescription = stringResource(R.string.manual_dialog_photo_frame_chosen)
    val emptyDescription = stringResource(R.string.manual_dialog_photo_frame_empty)

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 76.dp)
            .testTag(TierDetailTestTags.MANUAL_ENTRY_PHOTO_FRAME)
            .semantics { contentDescription = if (photoUri != null) chosenDescription else emptyDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 52.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            val removeDescription = stringResource(R.string.manual_dialog_remove_photo)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onRemove)
                    .semantics { contentDescription = removeDescription }
                    .testTag(TierDetailTestTags.MANUAL_ENTRY_REMOVE_PHOTO),
                contentAlignment = Alignment.Center,
            ) {
                ClearIcon(12.dp, Color.White)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .dashedBorder(width = 1.dp, color = outline, cornerRadius = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                ImagePlaceholderIcon(20.dp, outline)
            }
        }
    }
}
