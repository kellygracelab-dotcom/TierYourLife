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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.domain.naming.cardTitleFromPrompt
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioTestTags

@Composable
internal fun NamingDialog(
    imageUri: String,
    prompt: String,
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NamingDialogContent(imageUri = imageUri, prompt = prompt, onDismiss = onDismiss, onSave = onSave)
    }
}

@Composable
internal fun NamingDialogContent(
    imageUri: String,
    prompt: String,
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit,
) {
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val suggestedTitle = cardTitleFromPrompt(prompt)
        mutableStateOf(TextFieldValue(text = suggestedTitle, selection = TextRange(suggestedTitle.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val canSave = fieldValue.text.isNotBlank()
    val clearNameDescription = stringResource(R.string.cd_clear_name)

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
            value = fieldValue,
            onValueChange = { fieldValue = it },
            label = { Text(stringResource(R.string.ai_name_field)) },
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            trailingIcon = {
                if (fieldValue.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            fieldValue = TextFieldValue()
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = clearNameDescription }
                            .testTag(AiStudioTestTags.CLEAR_NAME),
                    ) { ClearIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
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
                onClick = { onSave(fieldValue.text.trim()) },
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

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Preview(showBackground = true)
@Composable
private fun NamingDialogLightPreview() = TierYourLifeTheme(false) {
    NamingDialogContent(
        imageUri = "",
        prompt = "A retro VHS cover with bold type",
        onDismiss = {},
        onSave = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun NamingDialogDarkPreview() = TierYourLifeTheme(true) {
    NamingDialogContent(
        imageUri = "",
        prompt = "A retro VHS cover with bold type",
        onDismiss = {},
        onSave = {},
    )
}
