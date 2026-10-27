package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.R

/** The cover screen's own sizes: a 352 x 339dp display reads at a smaller scale than a phone. */
object CoverType {
    val Header = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val Count = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val RowTitle = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
    val RowMeta = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
    val SectionRowTitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val Button = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
}

/**
 * The cover's frame: its surface, with everything kept between the status
 * bar above and Samsung's dots and navigation bar below (§1). The bottom
 * keeps at least the 50dp the spec gives those two, whatever the system
 * reports for them. The sides are padded already, above every screen.
 */
@Composable
fun CoverSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(bottom = max(WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(), CoverBottomZone)),
        content = content,
    )
}

/** Samsung's page dots (14dp) and the navigation bar (36dp) under the cover's content. */
val CoverBottomZone: Dp = 50.dp

/** A cover header's touch targets: under the 48dp guideline on purpose, with at least 4dp between them. */
val CoverTarget: Dp = 36.dp

/**
 * The one header every cover screen has (§3): back when there is somewhere
 * to go back to, the title, a count, at most two actions. It scrolls away
 * with what it heads, so it is drawn as the first item of the content.
 */
@Composable
fun CoverHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    count: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CoverTarget)
            .padding(start = if (onBack != null) 8.dp else 18.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (onBack != null) {
            val back = stringResource(R.string.tier_detail_content_description_back)
            CoverIconButton(onClick = onBack, description = back, testTag = COVER_BACK_TAG) {
                CoverBackIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = CoverType.Header,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Text(
                text = count,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = CoverType.Count,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        actions()
    }
}

/** A 36dp target for a 20dp icon, round so the ripple stays inside the cover's curve. */
@Composable
fun CoverIconButton(onClick: () -> Unit, description: String, testTag: String? = null, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(CoverTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .let { if (testTag != null) it.testTag(testTag) else it },
        contentAlignment = Alignment.Center,
    ) { icon() }
}

const val COVER_BACK_TAG = "cover_back"

@Composable
fun CoverBackIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    fun at(x: Float, y: Float) = Offset(x * scale, y * scale)
    val stroke = 1.8f * scale
    drawLine(color, at(19f, 12f), at(5f, 12f), stroke, StrokeCap.Round)
    drawLine(color, at(11f, 6f), at(5f, 12f), stroke, StrokeCap.Round)
    drawLine(color, at(5f, 12f), at(11f, 18f), stroke, StrokeCap.Round)
}

/** Out of the corner: what the cover offers whenever the main screen is the place for it. */
@Composable
fun CoverOpenIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    fun at(x: Float, y: Float) = Offset(x * scale, y * scale)
    val stroke = 1.8f * scale
    drawLine(color, at(14f, 4f), at(20f, 4f), stroke, StrokeCap.Round)
    drawLine(color, at(20f, 4f), at(20f, 10f), stroke, StrokeCap.Round)
    drawLine(color, at(20f, 4f), at(13.5f, 10.5f), stroke, StrokeCap.Round)
    drawLine(color, at(10f, 20f), at(4f, 20f), stroke, StrokeCap.Round)
    drawLine(color, at(4f, 20f), at(4f, 14f), stroke, StrokeCap.Round)
    drawLine(color, at(4f, 20f), at(10.5f, 13.5f), stroke, StrokeCap.Round)
}
