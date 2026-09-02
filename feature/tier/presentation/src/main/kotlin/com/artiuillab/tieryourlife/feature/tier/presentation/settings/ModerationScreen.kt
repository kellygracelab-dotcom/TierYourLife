package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorFace
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.FlagIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon

@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
    viewModel: ModerationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val looking by viewModel.looking.collectAsStateWithLifecycle()
    ModerationScreenContent(
        state = state,
        looking = looking,
        onBack = onBack,
        onTakeDown = viewModel::takeDown,
        onDismiss = viewModel::dismiss,
        onRetry = viewModel::load,
        onOpenList = onOpenList,
        onLook = viewModel::look,
    )
}

@Composable
internal fun ModerationScreenContent(
    state: ModerationUiState,
    onBack: () -> Unit,
    looking: CommunityListUiState = CommunityListUiState.Loading,
    onTakeDown: (String, BanLength?) -> Unit = { _, _ -> },
    onDismiss: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenList: (String) -> Unit = {},
    onLook: (String) -> Unit = {},
) {
    // Kept for as long as the screen is open, and no longer. A queue that
    // remembers which covers you uncovered is a queue that leaks.
    var coversShown by rememberSaveable { mutableStateOf(true) }
    var takingDown by remember { mutableStateOf<ModerationReport?>(null) }

    // Beside the queue once there is room. A complaint now takes a list out of
    // the feed, so the feed is no longer where it can be looked at -- this is.
    val besideIt = currentWindowShape.holdsTwoPanes
    if (besideIt) {
        Row(Modifier.fillMaxSize().testTag(ModerationTestTags.SCREEN)) {
            Surface(
                modifier = Modifier.width(QUEUE_WIDTH).fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    Queue(
                        state, onBack, { takingDown = it }, onDismiss, onRetry, onLook, besideIt,
                        coversShown, { coversShown = !coversShown },
                    )
                }
            }
            CommunityListScreenContent(
                state = looking,
                onBack = {},
                onMoveItem = { _, _, _ -> },
                onSave = {},
                onRetry = {},
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        CenteredContent(
            max = ContentWidth.Reading,
            modifier = Modifier.fillMaxSize().testTag(ModerationTestTags.SCREEN),
        ) {
            Queue(
                state, onBack, { takingDown = it }, onDismiss, onRetry, onOpenList, besideIt,
                coversShown, { coversShown = !coversShown },
            )
        }
    }

    takingDown?.let { report ->
        TakeDownSheet(
            authorName = report.authorName,
            busy = (state as? ModerationUiState.Ready)?.settling != null,
            onTakeDown = { ban ->
                takingDown = null
                onTakeDown(report.listId, ban)
            },
            onDismiss = { takingDown = null },
        )
    }
}

/** Its own width, wider than the board index: these rows carry three lines. */
private val QUEUE_WIDTH = 360.dp

@Composable
private fun ColumnScope.Queue(
    state: ModerationUiState,
    onBack: () -> Unit,
    onTakeDown: (ModerationReport) -> Unit,
    onDismiss: (String) -> Unit,
    onRetry: () -> Unit,
    onChoose: (String) -> Unit,
    besideIt: Boolean,
    coversShown: Boolean,
    onToggleCovers: () -> Unit,
) {
    TopBar(onBack, coversShown, onToggleCovers)

            when (state) {
                ModerationUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ModerationUiState.Failed -> Message(
                    title = stringResource(R.string.moderation_failed),
                    body = stringResource(R.string.home_community_failed_body),
                    action = stringResource(R.string.action_try_again),
                    onAction = onRetry,
                )

                is ModerationUiState.Ready -> if (state.reports.isEmpty()) {
                    Message(
                        title = stringResource(R.string.moderation_empty_title),
                        body = stringResource(R.string.moderation_empty_body),
                        action = null,
                        onAction = {},
                    )
                } else {
                    LaunchedEffect(besideIt, state.reports.firstOrNull()?.listId) {
                        // The pane needs something in it the moment the queue
                        // arrives; nobody should have to tap to see the first.
                        if (besideIt) state.reports.firstOrNull()?.let { onChoose(it.listId) }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.reports, key = { it.listId }) { report ->
                            ReportCard(
                                report = report,
                                busy = state.settling != null,
                                chosen = besideIt && state.looking == report.listId,
                                onOpen = { onChoose(report.listId) },
                                coversShown = coversShown,
                                onTakeDown = { onTakeDown(report) },
                                onDismiss = { onDismiss(report.listId) },
                            )
                        }
                    }
                }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, coversShown: Boolean, onToggleCovers: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
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
                .semantics { contentDescription = backDescription }
                .testTag(ModerationTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.moderation_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // For the whole session rather than per card: covering one card at a
        // time is two gestures on every card, for ever, to protect the one
        // person who chose this work. Not tied to the reason either --
        // covering exactly what was reported as sexual would hide precisely
        // what has to be looked at.
        TextButton(
            onClick = onToggleCovers,
            modifier = Modifier.testTag(ModerationTestTags.COVERS_TOGGLE),
        ) {
            Text(
                stringResource(
                    if (coversShown) R.string.moderation_blur else R.string.moderation_unblur,
                ),
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: ModerationReport,
    busy: Boolean,
    chosen: Boolean,
    coversShown: Boolean,
    onOpen: () -> Unit,
    onTakeDown: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (chosen) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
            )
            .clickable(onClick = onOpen)
            .testTag(ModerationTestTags.reportCard(report.listId)),
    ) {
        // The whole complaint is often the picture, and making somebody open
        // the list to see it is making them decide blind. Edge to edge, at the
        // top, before a word of it is read.
        Cover(report = report, shown = coversShown)

        Column(Modifier.padding(16.dp)) {
        Text(
            text = report.listTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // The person, not only their name: the decision is about them now, and
        // a face is how a borderline case stops being an abstraction.
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthorFace(photoUrl = report.authorPhotoUrl, name = report.authorName, size = 24.dp)
            Text(
                text = report.authorName,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagIcon(18.dp, MaterialTheme.colorScheme.error)
            Text(
                // How many complained is the useful part: it is the difference
                // between one person taking offence and a queue forming.
                text = if (report.reportCount > 1) {
                    stringResource(
                        R.string.moderation_reason_and_count,
                        stringResource(report.reason.labelRes()),
                        pluralStringResource(
                            R.plurals.moderation_report_count,
                            report.reportCount,
                            report.reportCount,
                        ),
                    )
                } else {
                    stringResource(report.reason.labelRes())
                },
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        // Every note, verbatim. It is the only evidence there is, and the one
        // somebody bothered to type is rarely the first.
        report.notes.forEach { note ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (report.reviewed) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.moderation_already_kept),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Said before the buttons rather than found out afterwards: taking a
        // list out of the feed is a decision about somebody else's work, and
        // the one thing the person making it needs to know is how far it goes.
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.moderation_consequence),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag(ModerationTestTags.dismiss(report.listId)),
            ) {
                Text(
                    stringResource(
                        if (report.hidden) R.string.moderation_action_put_back else R.string.moderation_action_keep,
                    ),
                )
            }
            TextButton(
                onClick = onTakeDown,
                enabled = !busy,
                modifier = Modifier.testTag(ModerationTestTags.takeDown(report.listId)),
            ) {
                Text(
                    text = stringResource(R.string.moderation_action_take_down),
                    color = if (busy) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                )
            }
        }
        }
    }
}

/**
 * The picture the feed showed, or a stated absence of one.
 *
 * When covers are put away the picture is not drawn at all rather than
 * blurred: a blur is only a blur above Android 12, and below it the same code
 * would quietly show the thing it was meant to hide.
 */
@Composable
private fun Cover(report: ModerationReport, shown: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(COVER_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val url = report.coverImageUrl
        if (shown && url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag(ModerationTestTags.cover(report.listId)),
            )
        } else {
            Text(
                text = stringResource(
                    if (url == null) R.string.moderation_cover_none else R.string.moderation_unblur,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // On the picture rather than above the title: this is the only place
        // the sentence can be misread, so it stands where the misreading is.
        if (report.hidden) {
            Text(
                text = stringResource(R.string.moderation_hidden_pill),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PILL)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/** Tall enough to judge, short enough that four fit on a screen. */
private val COVER_HEIGHT = 140.dp

/** Dark whatever is under it, because what is under it is a photograph. */
private val PILL = Color(0xCC000000)

@Composable
private fun Message(title: String, body: String, action: String?, onAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(ModerationTestTags.MESSAGE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { FlagIcon(28.dp, MaterialTheme.colorScheme.outline) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

private fun ReportReason.labelRes(): Int = when (this) {
    ReportReason.Sexual -> R.string.report_reason_sexual
    ReportReason.Violence -> R.string.report_reason_violence
    ReportReason.Hate -> R.string.report_reason_hate
    ReportReason.Spam -> R.string.report_reason_spam
    ReportReason.Other -> R.string.report_reason_other
}

internal object ModerationTestTags {
    const val SCREEN = "moderation_screen"
    const val BACK = "moderation_back"
    const val MESSAGE = "moderation_message"
    const val COVERS_TOGGLE = "moderation_covers_toggle"
    const val TAKE_DOWN_SHEET = "moderation_take_down_sheet"
    const val TAKE_DOWN_CONFIRM = "moderation_take_down_confirm"
    const val BAN_NONE = "moderation_ban_none"
    const val BAN_FOREVER = "moderation_ban_forever"
    const val BAN_FOREVER_DIALOG = "moderation_ban_forever_dialog"
    const val BAN_FOREVER_CONFIRM = "moderation_ban_forever_confirm"

    fun cover(listId: String): String = "moderation_cover_$listId"

    fun banChoice(length: BanLength): String = "moderation_ban_${length.id}"

    fun reportCard(listId: String): String = "moderation_report_$listId"

    fun takeDown(listId: String): String = "moderation_take_down_$listId"

    fun dismiss(listId: String): String = "moderation_dismiss_$listId"
}

@TierYourLifeDevicePreviews
@Composable
private fun ModerationScreenPreview() = TierYourLifeTheme {
    ModerationScreenContent(
        state = ModerationUiState.Ready(
            listOf(
                ModerationReport(
                    listId = "1",
                    listTitle = "Every A24 film, ranked",
                    authorName = "Olena Marchuk",
                    reasons = listOf(ReportReason.Sexual, ReportReason.Spam, ReportReason.Other),
                    notes = listOf("Every card is a link to the same shop."),
                    reportCount = 3,
                    newestAtMillis = 0,
                    hidden = true,
                    reviewed = false,
                ),
                ModerationReport(
                    listId = "2",
                    listTitle = "Ramen places in Kyiv",
                    authorName = "Someone Else",
                    reasons = listOf(ReportReason.Hate),
                    notes = emptyList(),
                    reportCount = 1,
                    newestAtMillis = 0,
                    hidden = false,
                    reviewed = true,
                ),
            ),
        ),
        onBack = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun ModerationScreenEmptyPreview() = TierYourLifeTheme {
    ModerationScreenContent(state = ModerationUiState.Ready(emptyList()), onBack = {})
}
