package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScheduleIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.TileStyle
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

internal object WaitingSectionTags {
    const val SHEET = "waiting_section_sheet"
    const val WITHDRAW = "waiting_section_withdraw"

    fun tile(id: String) = "waiting_section_$id"
    fun notice(id: String) = "proposal_notice_$id"
    fun dismiss(id: String) = "proposal_notice_dismiss_$id"
}

private const val PICTURE_ALPHA = 0.6f
private val RING = Color.White.copy(alpha = 0.6f)
private val PillLabel = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

/**
 * A proposal still waiting, as only its proposer sees it (design-spec-parity,
 * "Waiting section"): its picture at 60 % over the tint with the scrim, a
 * dashed ring, and a Waiting pill. Tapping it says what happens next.
 */
@Composable
internal fun WaitingSectionTile(section: Section, onOpen: () -> Unit, modifier: Modifier = Modifier, style: TileStyle = TileStyle()) {
    val name = section.label()
    val waiting = stringResource(R.string.section_waiting_pill)
    Box(
        modifier
            .clip(RoundedCornerShape(style.corner))
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "$name, $waiting" }
            .testTag(WaitingSectionTags.tile(section.id)),
    ) {
        WaitingArt(section, Modifier.fillMaxSize())
        DashedRing(Modifier.fillMaxSize().padding(4.dp), corner = 12.dp)
        WaitingPill(Modifier.align(Alignment.TopStart).padding(8.dp))
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = style.start, end = style.end, bottom = style.bottom),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SectionIcon(section, style.icon, SectionLabel)
            Text(name, style = style.label, color = SectionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The strip's square for a waiting proposal: the same picture at 60 % and the dashed ring, no words. */
@Composable
internal fun WaitingSectionSquare(section: Section, size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        WaitingArt(section, Modifier.fillMaxSize())
        DashedRing(Modifier.fillMaxSize().padding(3.dp), corner = 11.dp)
        ScheduleIcon(size * ICON_SHARE, Color.White)
    }
}

private const val ICON_SHARE = 0.4f

@Composable
private fun WaitingArt(section: Section, modifier: Modifier) {
    Box(modifier.background(lookOf(section).tintNow)) {
        section.pictureUrl?.let { url ->
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(PICTURE_ALPHA))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(TileScrimTop, TileScrimBottom))))
    }
}

@Composable
private fun DashedRing(modifier: Modifier, corner: Dp) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        drawRoundRect(
            color = RING,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(corner.toPx()),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
    }
}

@Composable
private fun WaitingPill(modifier: Modifier) {
    Row(
        modifier
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScheduleIcon(14.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.section_waiting_pill), style = PillLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** What a waiting proposal's proposer is told: a moderator will look, and where the lists are until then. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WaitingSectionSheet(section: Section, onWithdraw: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag(WaitingSectionTags.SHEET),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.label(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            WaitingPill(Modifier)
            Text(
                stringResource(R.string.section_waiting_body, sectionLabel(section.fileUnder ?: ListCategory.Other.id)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(onClick = onWithdraw, modifier = Modifier.testTag(WaitingSectionTags.WITHDRAW)) {
                    Text(stringResource(R.string.section_proposal_withdraw))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.section_ok)) }
            }
        }
    }
}

/** A proposal decided against: one line each, with the moderator's note, until its proposer puts it away. */
@Composable
internal fun ProposalNotices(notices: List<Section>, onDismiss: (String) -> Unit, modifier: Modifier = Modifier) {
    if (notices.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notices.forEach { notice -> ProposalNotice(notice) { onDismiss(notice.id) } }
    }
}

@Composable
private fun ProposalNotice(section: Section, onDismiss: () -> Unit) {
    val name = section.label()
    val dismiss = stringResource(R.string.section_proposal_dismiss)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp)
            .testTag(WaitingSectionTags.notice(section.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (section.status == SectionStatus.Merged) {
                    stringResource(R.string.section_proposal_merged, name, sectionLabel(section.mergedInto ?: ListCategory.Other.id))
                } else {
                    stringResource(R.string.section_proposal_declined, name, sectionLabel(section.fileUnder ?: ListCategory.Other.id))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            section.declineNote?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = dismiss }.testTag(WaitingSectionTags.dismiss(section.id))) {
            ClearIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
