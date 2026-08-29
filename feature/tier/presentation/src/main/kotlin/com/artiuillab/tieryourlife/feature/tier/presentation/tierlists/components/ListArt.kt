package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage

private const val MOSAIC_COLUMNS = 3
private const val MOSAIC_ROWS = 2
private const val MOSAIC_CELLS = MOSAIC_COLUMNS * MOSAIC_ROWS

/**
 * A published list is shown by whichever of three things it actually has. The
 * mosaic is not a placeholder: it is the author's own cards, which is the most
 * honest picture of what a reader is about to get.
 */
@Composable
internal fun ListArt(
    coverImageUrl: String?,
    previewImages: List<String>,
    tierColors: List<String>,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tagged = if (testTag == null) modifier else modifier.testTag(testTag)
    Box(tagged.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        when {
            coverImageUrl != null -> AsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            previewImages.isNotEmpty() -> Mosaic(previewImages)
            else -> TierBands(tierColors)
        }
    }
}

@Composable
private fun Mosaic(images: List<String>) {
    // Repeat rather than leave holes: a half-filled grid reads as a failed load.
    val cells = List(MOSAIC_CELLS) { images[it % images.size] }
    Column(Modifier.fillMaxSize()) {
        repeat(MOSAIC_ROWS) { row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(MOSAIC_COLUMNS) { column ->
                    AsyncImage(
                        model = cells[row * MOSAIC_COLUMNS + column],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TierBands(tierColors: List<String>) {
    val colors = tierColors.mapNotNull { it.toColorOrNull() }
        .ifEmpty { listOf(MaterialTheme.colorScheme.surfaceVariant) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        colors.forEach { color ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(color),
            )
        }
    }
}

private fun String.toColorOrNull(): Color? = runCatching {
    Color(this.toColorInt())
}.getOrNull()
