package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

private val TMDB = Regex("""^(https://image\.tmdb\.org/t/p/)w(\d+)(/.+)$""")
private val COMMONS = Regex("""^(https://commons\.wikimedia\.org/wiki/Special:FilePath/[^?]+)\?width=(\d+)$""")
private val TMDB_WIDTHS = listOf(92, 154, 185, 342, 500, 780)

/** Wikimedia's own steps: a width between them is drawn anew for every phone that asks. */
private val COMMONS_WIDTHS = listOf(120, 250, 330, 500, 960)

/**
 * A found picture at the size its tile shows it, as the site asks for it: a
 * tile 300 pixels wide does not need the 500-pixel poster, nor Commons' card
 * size. TMDB and Commons draw a picture at the width named in its address,
 * never wider than the address already asks; any other address stays.
 */
internal fun sizedFor(url: String, widthPx: Int): String {
    val tmdb = TMDB.matchEntire(url)
    val commons = COMMONS.matchEntire(url)
    return when {
        tmdb != null -> {
            val (base, named, path) = tmdb.destructured
            "${base}w${atStep(TMDB_WIDTHS, widthPx, named.toInt())}$path"
        }
        commons != null -> {
            val (base, named) = commons.destructured
            "$base?width=${atStep(COMMONS_WIDTHS, widthPx, named.toInt())}"
        }
        else -> url
    }
}

/** The first of [steps] that fills [wanted] pixels, the largest when none does, and never past [named]. */
private fun atStep(steps: List<Int>, wanted: Int, named: Int): Int = minOf(steps.firstOrNull { it >= wanted } ?: steps.last(), named)

/** What a result's tile loads: its small copy when there is one, at the tile's size. */
internal fun pictureFor(thumbUrl: String?, imageUrl: String?, widthPx: Int): String? =
    (thumbUrl ?: imageUrl)?.takeIf { it.isNotBlank() }?.let { sizedFor(it, widthPx) }

/**
 * A result's picture over a quiet pulse until it comes (§1.5: no shimmer, no
 * spinner). One that will not load is reported to [onBroken], so the grid
 * can drop its tile rather than leave a grey square, as the site does.
 */
@Composable
internal fun SearchPicture(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onBroken: () -> Unit = {},
) {
    var loaded by remember(model) { mutableStateOf(false) }
    val block = MaterialTheme.colorScheme.surfaceVariant
    val pulse = if (loaded) Still else rememberPulse()
    Box(modifier.drawBehind { drawRect(block, alpha = pulse()) }) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true },
            onError = { onBroken() },
            modifier = Modifier.matchParentSize(),
        )
    }
}

/** Read while drawing, so a pulsing tile redraws without composing again. */
@Composable
private fun rememberPulse(): () -> Float {
    val transition = rememberInfiniteTransition(label = "picture pulse")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = keyframes {
                durationMillis = PULSE_MILLIS
                1f at 0
                PULSE_LOW at PULSE_MILLIS / 2
                1f at PULSE_MILLIS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "picture pulse alpha",
    )
    return remember(alpha) { { alpha.value } }
}

private val Still: () -> Float = { 1f }
private const val PULSE_MILLIS = 1400
private const val PULSE_LOW = 0.55f
