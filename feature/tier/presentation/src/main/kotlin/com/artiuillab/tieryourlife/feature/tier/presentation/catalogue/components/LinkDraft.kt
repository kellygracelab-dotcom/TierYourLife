package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.isPageAddress
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.LinkUiState
import java.util.Date

/** What the field's address and the chip above the scopes do. */
class LinkChoice(
    val state: LinkUiState?,
    /** Something that may be an address is on the clipboard; it is read only when the chip is tapped. */
    val pasteOffered: Boolean = false,
    val onPaste: () -> Unit = {},
    /** [page] is the page somebody says the picture is from, already checked; null when they gave none. */
    val onAdd: (title: String, picture: LinkPicture.Found, page: String?) -> Unit = { _, _, _ -> },
    val onAddWithoutPicture: (title: String) -> Unit = {},
    val onUpload: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
internal fun PasteLinkChip(onPaste: () -> Unit) {
    FilledTonalButton(
        onClick = onPaste,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_PASTE_LINK),
    ) { Text(stringResource(R.string.item_search_link_paste)) }
}

/**
 * The card a picture's address becomes, in the same place the results go.
 * Whatever goes wrong is said on the card, with the way that still works,
 * and the name typed stays.
 */
@Composable
internal fun LinkDraft(state: LinkUiState, choice: LinkChoice) {
    var title by rememberSaveable(state.url) { mutableStateOf("") }
    var pageOpen by rememberSaveable(state.url) { mutableStateOf(false) }
    var page by rememberSaveable(state.url) { mutableStateOf("") }
    val answer = state.answer
    val pageWrong = page.isNotBlank() && !isPageAddress(page)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LinkPreview(answer)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.item_search_link_from, state.host),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinkStatus(answer)
            }
        }
        if (answer != LinkPicture.SignInNeeded) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.item_search_link_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_NAME),
            )
        }
        if (answer is LinkPicture.Found && answer.attribution.isFromAnySite()) {
            if (pageOpen) {
                OutlinedTextField(
                    value = page,
                    onValueChange = { page = it },
                    label = { Text(stringResource(R.string.item_search_link_page)) },
                    singleLine = true,
                    isError = pageWrong,
                    supportingText = if (pageWrong) {
                        { Text(stringResource(R.string.item_search_link_page_bad)) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_PAGE),
                )
            } else {
                TextButton(onClick = { pageOpen = true }, modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_PAGE_OPEN)) {
                    Text(stringResource(R.string.item_search_link_page_open))
                }
            }
        }
        LinkActions(
            answer = answer,
            choice = choice,
            canAdd = !pageWrong,
            hasTitle = title.isNotBlank(),
            onAdd = { found -> choice.onAdd(title.trim(), found, page.trim().takeIf { it.isNotEmpty() }) },
            onAddWithoutPicture = { choice.onAddWithoutPicture(title.trim()) },
        )
    }
}

@Composable
private fun LinkPreview(answer: LinkPicture?) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        when (answer) {
            null -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            is LinkPicture.Found -> AsyncImage(
                model = answer.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Unit
        }
    }
}

@Composable
internal fun LinkStatus(answer: LinkPicture?) {
    val warning = answer is LinkPicture.Refused
    val text = when (answer) {
        null -> stringResource(R.string.item_search_link_checking)
        is LinkPicture.Found -> stringResource(R.string.item_search_link_ready)
        is LinkPicture.Refused -> stringResource(answer.reason.message())
        is LinkPicture.Limited -> stringResource(
            R.string.item_search_link_limit,
            DateFormat.getTimeFormat(LocalContext.current).format(Date(answer.until)),
        )
        LinkPicture.SignInNeeded -> stringResource(R.string.item_search_link_sign_in_text)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_STATUS),
    )
}

@Composable
private fun LinkActions(
    answer: LinkPicture?,
    choice: LinkChoice,
    canAdd: Boolean,
    hasTitle: Boolean,
    onAdd: (LinkPicture.Found) -> Unit,
    onAddWithoutPicture: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp),
    ) {
        when (answer) {
            null -> Unit
            is LinkPicture.Found -> Button(
                onClick = { onAdd(answer) },
                enabled = canAdd,
                modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_ADD),
            ) { Text(stringResource(R.string.item_search_link_add)) }
            is LinkPicture.Refused -> {
                if (answer.reason == LinkRefusal.Failed) {
                    OutlinedButton(onClick = choice.onRetry) { Text(stringResource(R.string.item_search_try_again)) }
                }
                WithoutPicture(hasTitle, onAddWithoutPicture)
            }
            is LinkPicture.Limited -> {
                OutlinedButton(
                    onClick = choice.onUpload,
                    modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_UPLOAD),
                ) { Text(stringResource(R.string.item_search_link_upload)) }
                WithoutPicture(hasTitle, onAddWithoutPicture)
            }
            LinkPicture.SignInNeeded -> Button(
                onClick = choice.onSignIn,
                modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_SIGN_IN),
            ) { Text(stringResource(R.string.item_search_link_sign_in)) }
        }
    }
}

@Composable
private fun WithoutPicture(hasTitle: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = hasTitle,
        modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_LINK_WITHOUT_PICTURE),
    ) { Text(stringResource(R.string.item_search_link_add_without_picture)) }
}

private fun LinkRefusal.message(): Int = when (this) {
    LinkRefusal.NotAPicture -> R.string.item_search_link_not_a_picture
    LinkRefusal.Unreachable -> R.string.item_search_link_unreachable
    LinkRefusal.TooBig -> R.string.item_search_link_too_big
    LinkRefusal.Refused -> R.string.item_search_link_refused
    LinkRefusal.Failed -> R.string.item_search_link_failed
}

/** A catalogue names its own page; only a picture from any other site asks which page it is from. */
private fun PictureAttribution?.isFromAnySite(): Boolean = this == null || source == WEB_SOURCE

internal const val WEB_SOURCE = "web"

/** The card a picture from a link becomes; the page somebody names goes with it as where it is from. */
fun LinkPicture.Found.attributionWith(page: String?): PictureAttribution? {
    val known = attribution
    return when {
        page == null -> known
        known == null -> PictureAttribution(WEB_SOURCE, link = page)
        known.source == WEB_SOURCE -> known.copy(link = page)
        else -> known
    }
}
