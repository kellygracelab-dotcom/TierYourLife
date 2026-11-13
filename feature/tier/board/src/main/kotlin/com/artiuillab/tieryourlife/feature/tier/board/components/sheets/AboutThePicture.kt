package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.ui.TmdbNotice
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** Whether there is anything to say about a card's picture: someone's own photograph has nothing. */
internal fun TierItem.hasAboutThePicture(): Boolean =
    imageUrl != null && (attribution != null || !credit.isNullOrBlank() || PictureAttribution.isTmdb(imageUrl, null))

/**
 * Who made a card's picture and where it is from: author, licence and the
 * page at the source, each left out when unknown. A TMDB picture has none of
 * those but TMDB's own notice. A card picked before attributions keeps the
 * credit text it was given.
 */
@Composable
internal fun AboutThePicture(item: TierItem, onDismiss: () -> Unit) {
    val attribution = item.attribution
    val linkColor = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(72.dp, 96.dp).clip(RoundedCornerShape(10.dp)).background(TierYourLifeMedia.current.tilePlaceholder),
            ) {
                AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
            }
            Text(item.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (attribution != null) {
                attribution.author?.let { PictureFact(stringResource(R.string.tier_detail_picture_author), AnnotatedString(it)) }
                attribution.license?.let { PictureFact(stringResource(R.string.tier_detail_picture_license), linked(it, attribution.licenseUrl, linkColor)) }
                sourceNameOf(attribution)?.let { PictureFact(stringResource(R.string.tier_detail_picture_source), linked(it, attribution.link, linkColor)) }
            } else {
                item.credit?.takeIf { it.isNotBlank() }?.let { credit ->
                    val text = remember(credit) { linkedCredit(credit) }
                    SelectionContainer { Text(text, Modifier.testTag(TierDetailTestTags.IMAGE_CREDITS_TEXT)) }
                }
            }
            if (PictureAttribution.isTmdb(item.imageUrl, attribution)) TmdbNotice(Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.tier_detail_credits_close)) }
    }
}

@Composable
private fun PictureFact(label: String, value: AnnotatedString) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** [text], opening [url] when it is a web page; anything else stays plain text. */
internal fun linked(text: String, url: String?, color: Color) = buildAnnotatedString {
    if (url != null && url.startsWith("https://")) {
        withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = color)))) { append(text) }
    } else {
        append(text)
    }
}

private val SOURCE_NAMES = mapOf(
    "tmdb" to "TMDB",
    "anilist" to "AniList",
    // IGDB's terms name it by its address.
    "igdb" to "IGDB.com",
    "steam" to "Steam",
    "deezer" to "Deezer",
    "openlibrary" to "Open Library",
    "inaturalist" to "iNaturalist",
    "wikimedia" to "Wikimedia Commons",
)

/** The catalogue by its own name; any other site by the host of its page, and nothing without one. */
internal fun sourceNameOf(attribution: PictureAttribution): String? =
    SOURCE_NAMES[attribution.source]
        ?: attribution.link?.substringAfter("://", "")?.substringBefore('/')?.removePrefix("www.")?.ifBlank { null }
