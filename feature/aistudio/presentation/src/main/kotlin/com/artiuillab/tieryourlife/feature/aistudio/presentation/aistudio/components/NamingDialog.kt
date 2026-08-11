package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioTestTags

@Composable
internal fun NamingDialog(
    imageUri: String,
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val canSave = title.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .testTag(AiStudioTestTags.DIALOG)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_name_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ai_name_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.ai_name_field)) },
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .focusRequester(focusRequester)
                    .testTag(AiStudioTestTags.NAME_FIELD),
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.size(width = 52.dp, height = 76.dp),
                        contentScale = ContentScale.Crop,
                    )
                    AiBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.ai_name_photo_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(AiStudioTestTags.CANCEL),
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { onSave(title.trim()) },
                    enabled = canSave,
                    modifier = Modifier.testTag(AiStudioTestTags.ADD),
                ) {
                    Text(
                        text = stringResource(R.string.action_add),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
