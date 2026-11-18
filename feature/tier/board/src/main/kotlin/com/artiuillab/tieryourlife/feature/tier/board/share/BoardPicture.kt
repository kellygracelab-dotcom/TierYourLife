package com.artiuillab.tieryourlife.feature.tier.board.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.artiuillab.tieryourlife.core.settings.nextWholeCut
import com.artiuillab.tieryourlife.core.settings.wholeCutAtOrBefore
import com.artiuillab.tieryourlife.feature.tier.board.common.parseTierColor
import com.artiuillab.tieryourlife.feature.tier.board.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.BAND_CAPTION_PADDING
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.wrapWords
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlin.math.max

/** A snapshot of the theme, taken in composition and handed to a renderer that has none. */
data class BoardPalette(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val onBand: Color,
    val unrankedBand: Color,
    val isDark: Boolean,
)

/**
 * A board as a picture, drawn with a Canvas rather than photographed: the
 * screen scrolls and is cut by the pool sheet, a picture wants the whole
 * board at one width, and drawing it is the only way a test can check it.
 */
object BoardPicture {

    const val WIDTH = 1080
    private const val PADDING = 48f
    private const val MIN_BAND_WIDTH = 150f
    private const val MAX_BAND_WIDTH = 320f
    private const val MIN_LABEL_SIZE = 30f
    private const val BAND_CAPTION_PADDING = 14f
    private const val TILE_WIDTH = 132f
    private const val TILE_HEIGHT = 192f
    private const val TILE_GAP = 18f
    private const val ROW_PADDING = 24f
    private const val ROW_GAP = 18f
    private const val ROW_RADIUS = 28f
    private const val TILE_RADIUS = 14f
    private const val TITLE_SIZE = 56f
    private const val SUBTITLE_SIZE = 34f
    private const val LABEL_SIZE = 60f
    private const val CAPTION_SIZE = 26f
    private const val TILE_TEXT_SIZE = 24f
    private const val NAME_TEXT_SIZE = 20f
    private const val NAME_PADDING = 8f
    private const val NAME_SHADE_RISE = 28f
    private const val NAME_SHADE = 0xC7000000.toInt()
    private const val TITLE_SMALLEST = 40f
    private const val TITLE_LINE = 1.15f
    private const val NOT_PLACED_SIZE = 30f
    private const val NOT_PLACED_HEIGHT = 64f
    private const val FOOTER_HEIGHT = 120f
    private const val FOOTER_TEXT_SIZE = 32f
    private const val MARK_SIZE = 54f
    private const val MARK_RADIUS = 14f
    private const val MARK_GAP = 22f
    private const val PILL_TEXT_SIZE = 27f
    private const val PILL_SMALLEST_TEXT = 20f
    private const val PILL_HEIGHT = 56f
    private const val PILL_PADDING = 26f
    private const val MAX_ROWS_PER_TIER = 6
    private const val BRAND = "TierYourLife"

    /** The mark's stripes, top to bottom: the default tiers' colours, as on the site's icon. */
    private val MARK_STRIPES = listOf(
        0.25f to 0xFFB03A32.toInt(),
        0.45f to 0xFFC06A25.toInt(),
        0.62f to 0xFFA98B1F.toInt(),
        0.80f to 0xFF3F7F55.toInt(),
        1f to 0xFF3C6E99.toInt(),
    )

    /**
     * Pictures by item id. A card without one is drawn as its title; with [names], a picture carries its name along its foot.
     * [footer] is the pill at the foot (rank it yourself, and where), [counts] the line under the title, and
     * [notPlaced] the one line that stands for the cards still in the deck: the picture shows the ranking, not the deck.
     */
    fun render(
        list: TierList,
        palette: BoardPalette,
        pictures: Map<Long, Bitmap>,
        footer: String,
        names: Boolean = false,
        counts: String = "",
        notPlaced: String? = null,
    ): Bitmap {
        val ranked = list.tiers.filterNot { it.isPool }
        // One band width for the whole board, from the longest caption: a
        // caption never moves the column of tier letters, on screen or here.
        val bandWidth = bandWidthFor(ranked)
        val perLine = tilesPerLine(bandWidth)
        val rows = ranked.map { tier -> Row(tier, perLine) }

        val title = titlePaint(palette)
        val titleLines = titleLines(list.title, title)
        val headerHeight = PADDING + title.textSize * (1 + TITLE_LINE * (titleLines.size - 1)) + 12f + SUBTITLE_SIZE + PADDING
        val boardHeight = rows.sumOf { it.height.toDouble() }.toFloat() + ROW_GAP * max(0, rows.size - 1)
        val notPlacedHeight = if (notPlaced != null) NOT_PLACED_HEIGHT else 0f
        val height = (headerHeight + boardHeight + notPlacedHeight + PADDING + FOOTER_HEIGHT).toInt()

        val bitmap = createBitmap(WIDTH, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.surface.toArgb())

        drawHeader(canvas, titleLines, title, counts, palette)

        var y = headerHeight
        for (row in rows) {
            drawRow(canvas, row, y, bandWidth, palette, pictures, names)
            y += row.height + ROW_GAP
        }
        if (notPlaced != null) {
            val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.onSurfaceVariant.toArgb()
                textSize = NOT_PLACED_SIZE
                typeface = Typeface.SANS_SERIF
            }
            canvas.drawText(notPlaced, PADDING, y - ROW_GAP + NOT_PLACED_HEIGHT / 2 + NOT_PLACED_SIZE / 2, text)
        }

        drawFooter(canvas, height.toFloat(), footer, palette)
        return bitmap
    }

    private fun titlePaint(palette: BoardPalette) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.onSurface.toArgb()
        textSize = TITLE_SIZE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    /**
     * The whole name in two lines at most, broken between words: a long one takes a smaller size first,
     * and only what still does not fit ends in an ellipsis. Leaves [paint] at the size chosen.
     */
    private fun titleLines(title: String, paint: TextPaint): List<String> {
        val width = WIDTH - 2 * PADDING
        val words = title.split(' ', '\n', '\t').filter { it.isNotEmpty() }
        fun wrap() = wrapWords(words, width.toInt()) { paint.measureText(it).toInt() }
        var wrapped = wrap()
        while (paint.textSize > TITLE_SMALLEST && (wrapped.size > 2 || wrapped.any { paint.measureText(it) > width })) {
            paint.textSize -= 2f
            wrapped = wrap()
        }
        return listOf(wrapped.firstOrNull().orEmpty()) + listOfNotNull(wrapped.drop(1).joinToString(" ").takeIf { it.isNotEmpty() })
            .map { TextUtils.ellipsize(it, paint, width, TextUtils.TruncateAt.END).toString() }
    }

    private fun tilesPerLine(bandWidth: Float): Int =
        ((WIDTH - 2 * PADDING - bandWidth - 2 * ROW_PADDING + TILE_GAP) / (TILE_WIDTH + TILE_GAP)).toInt()

    private fun bandWidthFor(tiers: List<Tier>): Float {
        val caption = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = CAPTION_SIZE
            typeface = Typeface.SANS_SERIF
        }
        val widest = tiers.mapNotNull { it.caption?.takeIf { c -> c.isNotBlank() } }.maxOfOrNull { caption.measureText(it) } ?: 0f
        return (widest + 2 * BAND_CAPTION_PADDING).coerceIn(MIN_BAND_WIDTH, MAX_BAND_WIDTH)
    }

    /** The largest size at which a label fits its band, down to a floor. A letter never shrinks; a word can. */
    private fun labelSizeFor(label: String, bandWidth: Float): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        var size = LABEL_SIZE
        while (size > MIN_LABEL_SIZE) {
            paint.textSize = size
            if (paint.measureText(label) <= bandWidth - 2 * BAND_CAPTION_PADDING) break
            size -= 2f
        }
        return size
    }

    private class Row(val tier: Tier, perLine: Int) {
        val lines: Int = max(1, (tier.items.size + perLine - 1) / perLine).coerceAtMost(MAX_ROWS_PER_TIER)
        val shown: Int = (lines * perLine).coerceAtMost(tier.items.size)
        val hidden: Int = tier.items.size - shown
        val perLine: Int = perLine
        val height: Float = ROW_PADDING * 2 + lines * TILE_HEIGHT + (lines - 1) * TILE_GAP
    }

    private fun drawHeader(canvas: Canvas, titleLines: List<String>, title: TextPaint, counts: String, palette: BoardPalette) {
        var baseline = PADDING + title.textSize
        titleLines.forEachIndexed { index, line ->
            if (index > 0) baseline += title.textSize * TITLE_LINE
            canvas.drawText(line, PADDING, baseline, title)
        }
        val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurfaceVariant.toArgb()
            textSize = SUBTITLE_SIZE
            typeface = Typeface.SANS_SERIF
        }
        val width = WIDTH - 2 * PADDING
        canvas.drawText(
            TextUtils.ellipsize(counts, subtitle, width, TextUtils.TruncateAt.END).toString(),
            PADDING,
            baseline + 12f + SUBTITLE_SIZE,
            subtitle,
        )
    }

    private fun drawRow(canvas: Canvas, row: Row, top: Float, bandWidth: Float, palette: BoardPalette, pictures: Map<Long, Bitmap>, names: Boolean) {
        val tier = row.tier
        val band = if (tier.isPool) {
            palette.unrankedBand
        } else {
            parseTierColor(if (palette.isDark) tier.colorDark else tier.colorLight, fallback = palette.unrankedBand)
        }
        val left = PADDING
        val right = WIDTH - PADDING
        val bottom = top + row.height

        // Band colour at a whisper over the surface, the screen's own rule.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowTintFor(band, palette.surface).toArgb() }
        canvas.drawRoundRect(RectF(left, top, right, bottom), ROW_RADIUS, ROW_RADIUS, fill)

        // The band, squared off where it meets the row.
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = band.toArgb() }
        canvas.withClip(left, top, left + bandWidth, bottom) {
            drawRoundRect(RectF(left, top, left + bandWidth + ROW_RADIUS, bottom), ROW_RADIUS, ROW_RADIUS, bandPaint)
        }

        val onBand = if (tier.isPool) palette.onSurfaceVariant else palette.onBand
        val labelSize = labelSizeFor(tier.label, bandWidth)
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onBand.toArgb()
            textSize = labelSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val caption = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onBand.toArgb()
            textSize = CAPTION_SIZE
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
        }
        val bandCentre = left + bandWidth / 2
        val textWidth = bandWidth - 2 * BAND_CAPTION_PADDING
        val labelText = TextUtils.ellipsize(tier.label, label, textWidth, TextUtils.TruncateAt.END).toString()
        val captionText = tier.caption?.takeIf { it.isNotBlank() }
            ?.let { TextUtils.ellipsize(it, caption, textWidth, TextUtils.TruncateAt.END).toString() }
        // Top-aligned like the screen: on a tall row the letter stays beside
        // the first line of cards rather than drifting to the middle.
        val labelBaseline = top + ROW_PADDING + labelSize * 0.8f + (TILE_HEIGHT - labelSize - (if (captionText != null) CAPTION_SIZE + 6f else 0f)) / 2
        canvas.drawText(labelText, bandCentre, labelBaseline, label)
        if (captionText != null) canvas.drawText(captionText, bandCentre, labelBaseline + CAPTION_SIZE + 6f, caption)

        val tilesLeft = left + bandWidth + ROW_PADDING
        val tileText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurface.toArgb()
            textSize = TILE_TEXT_SIZE
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
        }
        val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.outlineVariant.toArgb() }
        for (index in 0 until row.shown) {
            val item = tier.items[index]
            val x = tilesLeft + (index % row.perLine) * (TILE_WIDTH + TILE_GAP)
            val y = top + ROW_PADDING + (index / row.perLine) * (TILE_HEIGHT + TILE_GAP)
            val frame = RectF(x, y, x + TILE_WIDTH, y + TILE_HEIGHT)
            val picture = pictures[item.id]
            if (picture != null) {
                val rounded = android.graphics.Path().apply { addRoundRect(frame, TILE_RADIUS, TILE_RADIUS, android.graphics.Path.Direction.CW) }
                canvas.withClip(rounded) {
                    drawBitmap(picture, cover(picture, frame), frame, null)
                    if (names) drawNameStrip(this, item.title, frame)
                }
            } else {
                canvas.drawRoundRect(frame, TILE_RADIUS, TILE_RADIUS, placeholder)
                drawTitle(canvas, item.title, frame, tileText)
            }
        }
        if (row.hidden > 0) {
            val more = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.onSurfaceVariant.toArgb()
                textSize = CAPTION_SIZE
                typeface = Typeface.SANS_SERIF
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("+${row.hidden} more", right - ROW_PADDING, bottom - 10f, more)
        }
    }

    /** The part of a picture that fills the frame edge to edge, centred. */
    private fun cover(picture: Bitmap, frame: RectF): Rect {
        val frameRatio = frame.width() / frame.height()
        val pictureRatio = picture.width.toFloat() / picture.height
        return if (pictureRatio > frameRatio) {
            val w = (picture.height * frameRatio).toInt()
            val x = (picture.width - w) / 2
            Rect(x, 0, x + w, picture.height)
        } else {
            val h = (picture.width / frameRatio).toInt()
            val y = (picture.height - h) / 2
            Rect(0, y, picture.width, y + h)
        }
    }

    /** As the board draws it: white, two lines at most, on a shade that darkens toward the foot. */
    private fun drawNameStrip(canvas: Canvas, title: String, frame: RectF) {
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = NAME_TEXT_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val maxWidth = frame.width() - 2 * NAME_PADDING
        val words = title.split(' ', '\n', '\t').filter { it.isNotEmpty() }
        val wrapped = wrapWords(words, maxWidth.toInt()) { text.measureText(it).toInt() }
        // Between words, as on the board; what does not fit in two lines ends in an ellipsis.
        val lines = listOfNotNull(wrapped.firstOrNull(), wrapped.drop(1).joinToString(" ").takeIf { it.isNotEmpty() })
            .map { TextUtils.ellipsize(it, text, maxWidth, TextUtils.TruncateAt.END).toString() }
        val lineHeight = NAME_TEXT_SIZE * 1.15f
        val stripTop = frame.bottom - lines.size * lineHeight - NAME_SHADE_RISE
        val shade = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f,
                stripTop,
                0f,
                frame.bottom,
                android.graphics.Color.TRANSPARENT,
                NAME_SHADE,
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(frame.left, stripTop, frame.right, frame.bottom, shade)
        var y = frame.bottom - NAME_PADDING - (lines.size - 1) * lineHeight
        for (line in lines) {
            canvas.drawText(line, frame.centerX(), y, text)
            y += lineHeight
        }
    }

    private fun drawTitle(canvas: Canvas, title: String, frame: RectF, paint: TextPaint) {
        val maxWidth = frame.width() - 12f
        val lines = mutableListOf<String>()
        var rest = title.trim()
        while (rest.isNotEmpty() && lines.size < 3) {
            val cut = lineEnd(rest, paint.breakText(rest, true, maxWidth, null))
            lines += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty() && lines.isNotEmpty()) {
            lines[lines.lastIndex] = TextUtils.ellipsize(lines.last() + " " + rest, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        }
        val lineHeight = paint.textSize * 1.2f
        var y = frame.centerY() - (lines.size - 1) * lineHeight / 2 + paint.textSize / 3
        for (line in lines) {
            canvas.drawText(line, frame.centerX(), y, paint)
            y += lineHeight
        }
    }

    /** The mark and name on the left; on the right a pill saying where to rank it, so the picture leads to the list. */
    private fun drawFooter(canvas: Canvas, height: Float, footer: String, palette: BoardPalette) {
        val middle = height - FOOTER_HEIGHT / 2
        drawMark(canvas, RectF(PADDING, middle - MARK_SIZE / 2, PADDING + MARK_SIZE, middle + MARK_SIZE / 2))
        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurface.toArgb()
            textSize = FOOTER_TEXT_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val brandLeft = PADDING + MARK_SIZE + MARK_GAP
        canvas.drawText(BRAND, brandLeft, middle + FOOTER_TEXT_SIZE / 3, brand)

        val pill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surface.toArgb()
            typeface = Typeface.SANS_SERIF
        }
        val room = WIDTH - PADDING - (brandLeft + brand.measureText(BRAND) + MARK_GAP) - 2 * PILL_PADDING
        var size = PILL_TEXT_SIZE
        pill.textSize = size
        while (size > PILL_SMALLEST_TEXT && pill.measureText(footer) > room) {
            size -= 1f
            pill.textSize = size
        }
        val text = TextUtils.ellipsize(footer, pill, room, TextUtils.TruncateAt.END).toString()
        val pillWidth = pill.measureText(text) + 2 * PILL_PADDING
        val frame = RectF(WIDTH - PADDING - pillWidth, middle - PILL_HEIGHT / 2, WIDTH - PADDING, middle + PILL_HEIGHT / 2)
        canvas.drawRoundRect(frame, PILL_HEIGHT / 2, PILL_HEIGHT / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.onSurface.toArgb() })
        canvas.drawText(text, frame.left + PILL_PADDING, middle + size / 3, pill)
    }

    /** A rounded square in the default tiers' stripes with an S on it. */
    private fun drawMark(canvas: Canvas, frame: RectF) {
        val clip = android.graphics.Path().apply { addRoundRect(frame, MARK_RADIUS, MARK_RADIUS, android.graphics.Path.Direction.CW) }
        canvas.withClip(clip) {
            var top = frame.top
            for ((end, color) in MARK_STRIPES) {
                val bottom = frame.top + frame.height() * end
                drawRect(frame.left, top, frame.right, bottom, Paint().apply { this.color = color })
                top = bottom
            }
        }
        val letter = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = MARK_SIZE * 0.55f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("S", frame.centerX(), frame.centerY() + letter.textSize / 3, letter)
    }
}

/** Where a line of [text] with room for [fits] chars ends: at a space if it can, never inside an emoji. */
internal fun lineEnd(text: String, fits: Int): Int {
    if (fits >= text.length) return text.length
    val space = text.lastIndexOf(' ', fits)
    if (space > 0) return space
    return text.wholeCutAtOrBefore(fits).takeIf { it > 0 } ?: text.nextWholeCut(0)
}
