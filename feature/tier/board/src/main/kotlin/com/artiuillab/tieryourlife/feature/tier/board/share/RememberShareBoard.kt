package com.artiuillab.tieryourlife.feature.tier.board.share

import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.ui.PhotoLibraryIcon
import com.artiuillab.tieryourlife.core.theme.ui.ShareIcon
import com.artiuillab.tieryourlife.core.usage.LocalUsage
import com.artiuillab.tieryourlife.core.usage.UsageFeature
import com.artiuillab.tieryourlife.core.usage.shared
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/**
 * Share as image, as the list's menu does it: the tap opens the sheet at
 * once, the picture fills in there as its covers come, and Share image
 * lights up when it is ready. A published list's link can go straight away.
 */
@Composable
fun rememberShareBoard(ownName: String? = null): (TierList) -> Unit {
    var sharing by remember { mutableStateOf<TierList?>(null) }
    sharing?.let { list -> ShareImageSheet(list, ownName, onDone = { sharing = null }) }
    return { sharing = it }
}

/** What the sheet shows: the picture so far, how many of its covers have come, and the file once it is made. */
private class SheetState {
    var preview by mutableStateOf<Bitmap?>(null)
    var loaded by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var ready by mutableStateOf<Uri?>(null)
    var failed by mutableStateOf(false)
}

@Composable
private fun ShareImageSheet(list: TierList, ownName: String?, onDone: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val usage = LocalUsage.current
    val names = LocalBoardLayoutMemory.current.namesOnCards
    val shareLink = rememberShareLink()
    val words = remember(list, ownName, resources) { pictureWords(resources, list, ownName) }
    val state = remember(list) { SheetState() }

    LaunchedEffect(list, names, words) {
        val key = PictureKey(list, names, words.byLine, words.footer)
        SharedPictures.get(key)?.let { kept ->
            state.ready = kept
            return@LaunchedEffect
        }
        val covers = BoardPicture.drawnItems(list).filter { it.imageUrl != null }
        state.total = covers.size
        val pictures = ConcurrentHashMap<Long, Bitmap>()
        suspend fun draw() = withContext(Dispatchers.Default) { BoardPicture.render(list, words, HashMap(pictures), names) }
        state.preview = draw()
        // Redrawn as covers come, but never more than one drawing behind.
        val arrived = Channel<Unit>(Channel.CONFLATED)
        val redraw = launch {
            for (signal in arrived) {
                state.preview = draw()
                delay(REDRAW_EVERY_MS)
            }
        }
        loadCovers(context, covers) { id, bitmap ->
            pictures[id] = bitmap
            state.loaded = pictures.size
            arrived.trySend(Unit)
        }
        redraw.cancel()
        val finished = draw()
        state.preview = finished
        val uri = writePicture(context, finished)
        if (uri == null) {
            state.failed = true
        } else {
            SharedPictures.keep(key, uri)
            state.ready = uri
        }
    }

    AdaptiveSheet(onDismiss = onDone, maxHeight = 760.dp, modifier = Modifier.testTag(ShareTestTags.SHEET)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.list_settings_share), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val preview = state.preview
                Box(
                    Modifier
                        .heightIn(max = 360.dp)
                        .aspectRatio(BoardPicture.WIDTH / BoardPicture.HEIGHT.toFloat())
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    if (preview != null) {
                        Image(
                            preview.asImageBitmap(),
                            contentDescription = list.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.testTag(ShareTestTags.PREVIEW),
                        )
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            val ready = state.ready
            if (ready == null && !state.failed) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.share_drawing, state.loaded, state.total),
                        modifier = Modifier.weight(1f).testTag(ShareTestTags.PROGRESS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onDone, modifier = Modifier.testTag(ShareTestTags.CANCEL)) {
                        Text(stringResource(ThemeR.string.action_cancel))
                    }
                }
                LinearProgressIndicator(
                    progress = { if (state.total == 0) 0f else state.loaded / state.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(14.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (list.publishedId != null) {
                    OutlinedButton(
                        onClick = {
                            shareLink(list)
                            onDone()
                        },
                        modifier = Modifier.weight(1f).testTag(ShareTestTags.SHARE_LINK),
                    ) {
                        ShareIcon(18.dp, LocalContentColor.current)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share_link), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(
                    onClick = {
                        val uri = ready ?: return@Button
                        if (sendPicture(context, uri, captionFor(resources, list))) {
                            usage.shared(UsageFeature.ShareImage)
                        } else {
                            Toast.makeText(context, resources.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
                        }
                        onDone()
                    },
                    enabled = ready != null,
                    modifier = Modifier.weight(1f).testTag(ShareTestTags.SHARE_IMAGE),
                ) {
                    PhotoLibraryIcon(18.dp, LocalContentColor.current)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_image), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.failed) {
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.share_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private const val REDRAW_EVERY_MS = 120L

/** What the sheet is made of, for tests. */
object ShareTestTags {
    const val SHEET = "share_image_sheet"
    const val PREVIEW = "share_image_preview"
    const val PROGRESS = "share_image_progress"
    const val CANCEL = "share_image_cancel"
    const val SHARE_LINK = "share_image_link"
    const val SHARE_IMAGE = "share_image_send"
}

/** The words the picture carries. The author is named only on a list published under their name. */
internal fun pictureWords(resources: Resources, list: TierList, ownName: String?): PictureWords {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val author = ownName?.takeIf { it.isNotBlank() && list.publishedId != null && !list.anonymous }
    val count = resources.getQuantityString(R.plurals.share_cards_ranked, ranked, ranked)
    val host = SHARE_LINK.removePrefix("https://")
    return PictureWords(
        byLine = listOfNotNull(author?.let { resources.getString(R.string.share_by_author, it) }, count).joinToString(" · "),
        author = author,
        footer = list.publishedId?.let { publishedListLink(it).removePrefix("https://") } ?: resources.getString(R.string.share_rank_yourself, host),
        shortFooter = host,
        nothingYet = resources.getString(R.string.share_nothing_yet),
        cards = { resources.getQuantityString(R.plurals.board_cards_count, it, it) },
        seeAll = { resources.getQuantityString(R.plurals.share_see_all, it, it) },
    )
}

/** As Share link says it, so the picture and the link speak alike; a list not out yet has the title and where to rank it. */
internal fun captionFor(resources: Resources, list: TierList): String =
    list.publishedId?.let { shareLinkText(resources, list, publishedListLink(it)) }
        ?: resources.getString(R.string.share_caption, list.title, SHARE_LINK)

/** As a published list's page is reached; the same link Copy link gives. */
fun publishedListLink(publishedId: String): String = "$SHARE_LINK/l/${Uri.encode(publishedId)}"
