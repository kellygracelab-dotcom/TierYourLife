package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.NoteAddIcon

private const val TITLE_MAX_LENGTH = 60

@Composable
internal fun TierScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
    onMoreClick: () -> Unit = {},
    onRenameList: (String) -> Unit = {},
    titleEditable: Boolean = false,
    /** Someone else's list: nothing here is editable, so nothing offers to edit. */
    readOnly: Boolean = false,
    /** A reader's overflow: who wrote this, and how to stop seeing it. */
    onReaderMoreClick: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    val manualAddDescription = stringResource(R.string.tier_detail_content_description_manual_add)
    val moreDescription = stringResource(R.string.tier_detail_content_description_more)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription },
        ) { BackIcon() }
        if (titleEditable) {
            EditableListTitle(
                title = title,
                onRename = onRenameList,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
        } else {
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!readOnly) {
            IconButton(
                onClick = onManualAdd,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = manualAddDescription }
                    .testTag(TierDetailTestTags.MANUAL_ADD_BUTTON),
            ) { NoteAddIcon() }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moreDescription },
            ) { MoreIcon() }
        } else if (onReaderMoreClick != null) {
            IconButton(
                onClick = onReaderMoreClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moreDescription }
                    .testTag(CommunityTestTags.MORE),
            ) { MoreIcon() }
        }
    }
}

@Composable
private fun EditableListTitle(
    title: String,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        var fieldValue by remember {
            mutableStateOf(TextFieldValue(text = title, selection = TextRange(title.length)))
        }
        var hasFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        fun commit() {
            if (!isEditing) return
            val trimmed = fieldValue.text.trim()
            if (trimmed.isNotEmpty()) onRename(trimmed)
            isEditing = false
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                if (newValue.text.length <= TITLE_MAX_LENGTH) fieldValue = newValue
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) hasFocused = true else if (hasFocused) commit()
                }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
        )

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        val editDescription = stringResource(R.string.tier_detail_content_description_edit_title)
        Text(
            text = title,
            modifier = modifier
                .clickable { isEditing = true }
                .semantics { contentDescription = editDescription }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
