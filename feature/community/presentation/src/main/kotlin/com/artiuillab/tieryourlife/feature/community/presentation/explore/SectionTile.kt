package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionArt
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionIcon
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionLabel
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionNewLine
import com.artiuillab.tieryourlife.feature.tier.board.common.label

/** How a tile is drawn at one window size: label and icon sizes, padding, and whether the "new" line fits. */
internal data class TileStyle(
    val label: TextStyle = TileNameStyle,
    val icon: Dp = 18.dp,
    val start: Dp = 12.dp,
    val end: Dp = 10.dp,
    val bottom: Dp = 10.dp,
    val showsNew: Boolean = true,
    val corner: Dp = 16.dp,
) {
    companion object {
        val Cover = TileStyle(label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium), icon = 15.dp, start = 9.dp, end = 6.dp, bottom = 8.dp, showsNew = false, corner = 12.dp)
        val Mini = TileStyle(label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium), icon = 16.dp, start = 10.dp, bottom = 10.dp, showsNew = false, corner = 14.dp)
    }
}

/**
 * A section's tile (§1.3): its art under a scrim, its sign and name, and how
 * many lists are new this week. A long press offers to follow it (§2.3).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SectionTile(
    section: Section,
    newThisWeek: Int?,
    following: Boolean,
    onOpen: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle(),
    centredLabel: Boolean = false,
    longPressMenu: Boolean = true,
) {
    var menuOpen by rememberSaveable(section.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val pressScale by animateFloatAsState(if (menuOpen) 0.97f else 1f, tween(120), label = "tile press")
    val name = section.label()
    val newLine = newThisWeek?.takeIf { style.showsNew && it > 0 }?.let { countText(R.plurals.explore_new_this_week, it) }
    val described = if (newLine != null) stringResource(R.string.explore_tile_description, name, newLine) else name
    val shape = RoundedCornerShape(style.corner)

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(pressScale)
                .then(
                    if (menuOpen) {
                        Modifier
                            .border(5.dp, MaterialTheme.colorScheme.surface, shape)
                            .padding(3.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = if (longPressMenu) {
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        }
                    } else {
                        null
                    },
                )
                .semantics { contentDescription = described }
                .testTag(ExploreTestTags.tile(section.id)),
        ) {
            SectionArt(section, Modifier.fillMaxSize())
            Column(
                Modifier
                    .align(if (centredLabel) Alignment.CenterStart else Alignment.BottomStart)
                    .padding(start = style.start, end = style.end, bottom = if (centredLabel) 0.dp else style.bottom),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionIcon(section, style.icon, SectionLabel)
                    Text(name, style = style.label, color = SectionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (newLine != null) {
                    Text(newLine, style = CaptionStyle, color = SectionNewLine, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.width(190.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(if (following) R.string.explore_unfollow_section else R.string.explore_follow_section, name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    if (following) {
                        RemoveIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        PlusIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                onClick = {
                    menuOpen = false
                    if (following) onUnfollow() else onFollow()
                },
                modifier = Modifier.testTag(ExploreTestTags.TILE_MENU_FOLLOW),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.explore_open_section, name), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { ArrowForwardIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    menuOpen = false
                    onOpen()
                },
            )
        }
    }
}
