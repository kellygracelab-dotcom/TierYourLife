package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/**
 * A section's picture: the bundled illustration once it exists, and until
 * then its tint with a quiet hatch. The illustrations are generated from the
 * prompts in docs/design-spec-community-explore.md §12.2 and dropped in as
 * res/drawable-nodpi/section_<id>.webp, then listed in [sectionArtRes].
 * Bundled, never fetched: the tiles paint on the first frame.
 */
@Composable
internal fun SectionArt(section: ListCategory, modifier: Modifier = Modifier, scrim: Boolean = true) {
    val look = lookOf(section)
    Box(modifier.background(look.tintNow)) {
        val art = sectionArtRes(section)
        if (art != null) {
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
            )
        } else {
            Hatch(Modifier.fillMaxSize())
        }
        if (scrim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(TileScrimTop, TileScrimBottom))),
            )
        }
    }
}

/** None yet: the art is still to be generated, and the tint stands in. */
@DrawableRes
@Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
internal fun sectionArtRes(section: ListCategory): Int? = null

/** 45° stripes of white at 7%, 4dp wide, every 10dp. */
@Composable
private fun Hatch(modifier: Modifier) {
    Canvas(modifier) {
        val stripe = 4.dp.toPx()
        val step = 10.dp.toPx()
        val span = size.width + size.height
        clipRect {
            var x = -size.height
            while (x < span) {
                drawLine(
                    color = HATCH,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = stripe,
                )
                x += step
            }
        }
    }
}

private val HATCH = Color.White.copy(alpha = 0.07f)
