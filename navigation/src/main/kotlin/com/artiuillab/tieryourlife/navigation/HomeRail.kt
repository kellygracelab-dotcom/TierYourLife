package com.artiuillab.tieryourlife.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val RAIL_WIDTH = 80.dp
private val ITEM_WIDTH = 76.dp
private val PILL_SIZE = 32.dp
private val FAB_SIZE = 56.dp

/**
 * Replaces the tabs rather than joining them: two navigation systems on one
 * screen is what makes tablet layouts fall apart. Settings joins the rail
 * because a rail with two items looks unfinished. The new-board button is the
 * phone's, moved.
 */
@Composable
internal fun HomeRail(
    selected: RailDestination?,
    onSelect: (RailDestination) -> Unit,
    onNewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(RAIL_WIDTH).testTag(RailTestTags.RAIL),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box {
            Column(
                modifier = Modifier
                    // The rail takes the start inset itself, so a cutout or a
                    // side navigation bar pushes the icons rather than sitting
                    // on them; the content beside it keeps the other three.
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                    )
                    .padding(vertical = 12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NewListButton(onNewList)
                Box(Modifier.height(14.dp))
                RailDestination.entries.forEach { destination ->
                    RailItem(
                        destination = destination,
                        selected = destination == selected,
                        onSelect = { onSelect(destination) },
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

internal enum class RailDestination { Lists, Community, Settings }

@Composable
private fun NewListButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(FAB_SIZE).testTag(RailTestTags.NEW_LIST),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            RailIcon(RailDestination.Lists, add = true, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RailItem(destination: RailDestination, selected: Boolean, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .width(ITEM_WIDTH)
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .padding(vertical = 4.dp)
            .testTag(RailTestTags.item(destination)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = FAB_SIZE, height = PILL_SIZE)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            RailIcon(
                destination = destination,
                add = false,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = stringResource(destination.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun RailDestination.labelRes(): Int = when (this) {
    RailDestination.Lists -> R.string.rail_lists
    RailDestination.Community -> R.string.rail_community
    RailDestination.Settings -> R.string.rail_settings
}

internal object RailTestTags {
    const val RAIL = "home_rail"
    const val NEW_LIST = "home_rail_new_list"
    fun item(destination: RailDestination): String = "home_rail_${destination.name.lowercase()}"
}

@Composable
private fun RailIcon(destination: RailDestination, add: Boolean, color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        fun at(x: Float, y: Float) = Offset(x * scale, y * scale)
        when {
            add -> {
                drawLine(color, at(12f, 5f), at(12f, 19f), 2f * scale, StrokeCap.Round)
                drawLine(color, at(5f, 12f), at(19f, 12f), 2f * scale, StrokeCap.Round)
            }

            // A list: three rows, each a bullet and a line.
            destination == RailDestination.Lists -> repeat(3) { row ->
                val y = 6f + row * 6f
                drawCircle(color, 1.3f * scale, at(5f, y))
                drawLine(color, at(9f, y), at(20f, y), 1.8f * scale, StrokeCap.Round)
            }

            // A globe: a circle, its equator, and one meridian.
            destination == RailDestination.Community -> {
                drawCircle(color, 8f * scale, at(12f, 12f), style = Stroke(1.7f * scale))
                drawLine(color, at(4f, 12f), at(20f, 12f), 1.7f * scale)
                drawOval(
                    color = color,
                    topLeft = at(8f, 4f),
                    size = Size(8f * scale, 16f * scale),
                    style = Stroke(1.7f * scale),
                )
            }

            // A gear: a hub and eight teeth.
            else -> {
                drawCircle(color, 3.2f * scale, at(12f, 12f), style = Stroke(1.7f * scale))
                repeat(8) { spoke ->
                    val angle = spoke * (PI / 4).toFloat()
                    drawLine(
                        color,
                        at(12f + 5.4f * cos(angle), 12f + 5.4f * sin(angle)),
                        at(12f + 8.2f * cos(angle), 12f + 8.2f * sin(angle)),
                        1.8f * scale,
                        StrokeCap.Round,
                    )
                }
            }
        }
    }
}
