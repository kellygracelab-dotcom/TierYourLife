package com.artiuillab.tieryourlife.feature.tier.presentation.share

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
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.common.parseTierColor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.rowTintFor
import kotlin.math.max

/**
 * The colours a picture of a board is drawn in. A snapshot of the theme,
 * taken in composition and handed to a renderer that has no composition.
 */
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
 * A board as a picture: the rows, the cards, and a line at the foot saying
 * where it came from.
 *
 * Drawn with a Canvas rather than by photographing the screen. The screen
 * scrolls, is cut by the pool sheet and the status bar, and shows whatever
 * width the phone has; a picture sent to a friend wants the whole board at
 * one width, top to bottom, and nothing of the phone around it. Drawing it
 * is also the only way the result can be the same on every device and be
 * checked by a test.
 *
 * The geometry echoes the screen -- the same band beside the same tiles --
 * scaled to a width that reads well in a chat.
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
    private const val FOOTER_HEIGHT = 120f
    private const val FOOTER_TEXT_SIZE = 32f
    private const val MAX_ROWS_PER_TIER = 6

    /** Pictures by item id. A card without one is drawn as its title. */
    fun render(list: TierList, palette: BoardPalette, pictures: Map<Long, Bitmap>, footer: String): Bitmap {
        val ranked = list.tiers.filterNot { it.isPool }
        val pool = list.tiers.filter { it.isPool }
        val poolRow = pool.firstOrNull()?.let { first ->
            val items = pool.flatMap { it.items }
            items.takeIf { it.isNotEmpty() }?.let { Tier(-1, first.label, "", "", it, isPool = true) }
        }
        // One band width for the whole board, from the longest caption: a
        // caption never moves the column of tier letters, on screen or here.
        val bandWidth = bandWidthFor(ranked + listOfNotNull(poolRow))
        val perLine = tilesPerLine(bandWidth)
        val rows = ranked.map { tier -> Row(tier, perLine) } + listOfNotNull(poolRow?.let { Row(it, perLine) })

        val headerHeight = PADDING + TITLE_SIZE + 12f + SUBTITLE_SIZE + PADDING
        val boardHeight = rows.sumOf { it.height.toDouble() }.toFloat() + ROW_GAP * max(0, rows.size - 1)
        val height = (headerHeight + boardHeight + PADDING + FOOTER_HEIGHT).toInt()

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.surface.toArgb())

        drawHeader(canvas, list, ranked, palette)

        var y = headerHeight
        for (row in rows) {
            drawRow(canvas, row, y, bandWidth, palette, pictures)
            y += row.height + ROW_GAP
        }

        drawFooter(canvas, height.toFloat(), footer, palette)
        return bitmap
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

    private fun drawHeader(canvas: Canvas, list: TierList, ranked: List<Tier>, palette: BoardPalette) {
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurface.toArgb()
            textSize = TITLE_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val width = WIDTH - 2 * PADDING
        canvas.drawText(
            TextUtils.ellipsize(list.title, title, width, TextUtils.TruncateAt.END).toString(),
            PADDING,
            PADDING + TITLE_SIZE,
            title,
        )
        val rankedCount = ranked.sumOf { it.items.size }
        val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurfaceVariant.toArgb()
            textSize = SUBTITLE_SIZE
            typeface = Typeface.SANS_SERIF
        }
        canvas.drawText("$rankedCount ranked · ${ranked.size} tiers", PADDING, PADDING + TITLE_SIZE + 12f + SUBTITLE_SIZE, subtitle)
    }

    private fun drawRow(canvas: Canvas, row: Row, top: Float, bandWidth: Float, palette: BoardPalette, pictures: Map<Long, Bitmap>) {
        val tier = row.tier
        val band = if (tier.isPool) {
            palette.unrankedBand
        } else {
            parseTierColor(if (palette.isDark) tier.colorDark else tier.colorLight, fallback = palette.unrankedBand)
        }
        val left = PADDING
        val right = WIDTH - PADDING
        val bottom = top + row.height

        // The row: the band's own colour at a whisper over the surface, the
        // same rule the screen draws by.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowTintFor(band, palette.surface).toArgb() }
        canvas.drawRoundRect(RectF(left, top, right, bottom), ROW_RADIUS, ROW_RADIUS, fill)

        // The band, squared off where it meets the row.
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = band.toArgb() }
        canvas.save()
        canvas.clipRect(left, top, left + bandWidth, bottom)
        canvas.drawRoundRect(RectF(left, top, left + bandWidth + ROW_RADIUS, bottom), ROW_RADIUS, ROW_RADIUS, bandPaint)
        canvas.restore()

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
                canvas.save()
                canvas.clipPath(android.graphics.Path().apply { addRoundRect(frame, TILE_RADIUS, TILE_RADIUS, android.graphics.Path.Direction.CW) })
                canvas.drawBitmap(picture, cover(picture, frame), frame, null)
                canvas.restore()
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

    private fun drawTitle(canvas: Canvas, title: String, frame: RectF, paint: TextPaint) {
        val maxWidth = frame.width() - 12f
        val lines = mutableListOf<String>()
        var rest = title.trim()
        while (rest.isNotEmpty() && lines.size < 3) {
            val fits = paint.breakText(rest, true, maxWidth, null)
            val cut = if (fits < rest.length) rest.lastIndexOf(' ', fits).takeIf { it > 0 } ?: fits else fits
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

    private fun drawFooter(canvas: Canvas, height: Float, footer: String, palette: BoardPalette) {
        val rule = Paint().apply { color = palette.outlineVariant.toArgb() }
        canvas.drawRect(PADDING, height - FOOTER_HEIGHT, WIDTH - PADDING, height - FOOTER_HEIGHT + 2f, rule)
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onSurfaceVariant.toArgb()
            textSize = FOOTER_TEXT_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(footer, PADDING, height - FOOTER_HEIGHT / 2 + FOOTER_TEXT_SIZE / 3, text)
    }
}
