package com.artiuillab.tieryourlife.feature.tier.board.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.artiuillab.tieryourlife.core.settings.nextWholeCut
import com.artiuillab.tieryourlife.core.settings.wholeCutAtOrBefore
import com.artiuillab.tieryourlife.feature.tier.board.common.parseTierColor
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.wrapWords
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlin.math.ceil

/**
 * The words on the picture, worded in composition: [byLine] under the title
 * with [author] in it set bold, the pill's [footer] and, when the address
 * does not fit, [shortFooter]; [nothingYet] on an empty tier, [cards] on a
 * tier drawn as a thin band, and [seeAll] when the picture leaves cards out.
 */
class PictureWords(
    val byLine: String,
    val author: String?,
    val footer: String,
    val shortFooter: String,
    val nothingYet: String,
    val cards: (Int) -> String,
    val seeAll: (Int) -> String,
)

/**
 * A board as a picture of 1080×1350, the shape a chat and a feed show whole:
 * dark whatever the phone's theme, as the list's link preview is. A long list
 * gives each tier one line of large covers ending in "+N", twelve covers in
 * all, and the tiers after them thin bands with their count; a list of up to
 * 24 cards is drawn whole in smaller ones. The deck is not drawn: the count
 * under the title already says how many are ranked.
 */
object BoardPicture {

    const val WIDTH = 1080
    const val HEIGHT = 1350
    const val COVER_BUDGET = 12
    const val SMALL_LIST = 24

    private const val SIDE = 60f
    private const val TOP = 64f
    private const val TITLE_SIZE = 58f
    private const val TITLE_SMALLEST = 42f
    private const val TITLE_LINE = 1.08f
    private const val BY_GAP = 14f
    private const val BY_SIZE = 30f
    private const val ROWS_GAP_ABOVE = 44f
    private const val ROW_GAP = 12f
    private const val ROW_RADIUS = 20f
    private const val BAND = 112f
    private const val LABEL_SIZE = 54f
    private const val THIN_LABEL_SIZE = 34f
    private const val LABEL_SMALLEST = 22f
    private const val THIN = 64f
    private const val CELLS_PADDING = 14f
    private const val BIG_W = 144f
    private const val BIG_H = 206f
    private const val BIG_GAP = 12f
    private const val SMALL_W = 100f
    private const val SMALL_H = 144f
    private const val SMALL_GAP = 10f
    private const val TILE_RADIUS = 12f
    private const val MORE_SIZE = 40f
    private const val COUNT_SIZE = 26f
    private const val EMPTY_SIZE = 24f
    private const val TEXT_INSET = 22f
    private const val SEE_GAP = 16f
    private const val SEE_SIZE = 26f
    private const val SEE_ROOM = 64f
    private const val FOOT_BOTTOM = 52f
    private const val LOGO = 56f
    private const val LOGO_GAP = 14f
    private const val BRAND_SIZE = 32f
    private const val PILL_TEXT = 28f
    private const val PILL_SMALLEST = 20f
    private const val PILL_PAD_X = 28f
    private const val PILL_HEIGHT = 64f
    private const val TILE_TEXT_SIZE = 22f
    private const val NAME_TEXT_SIZE = 18f
    private const val NAME_PADDING = 7f
    private const val NAME_SHADE_RISE = 24f
    private const val NAME_SHADE = 0xC7000000.toInt()
    private const val BRAND = "TierYourLife"

    /** How far below its middle a line's baseline sits, as a share of its size. */
    private const val BASELINE_DROP = 0.36f
    private const val LABEL_ROOM = BAND - 16f
    private const val GLOW_X = WIDTH * 0.9f
    private const val GLOW_Y = -HEIGHT * 0.05f
    private const val GLOW_RADIUS = 700f
    private const val NAME_SCALE_MIN = 0.8f
    private const val NAME_LINE = 1.15f
    private const val TILE_TITLE_INSET = 12f
    private const val TILE_TITLE_LINES = 3
    private const val TILE_TITLE_LINE = 1.2f

    private const val BACKGROUND = 0xFF12151C.toInt()
    private const val GLOW = 0x26A9C7FF
    private const val ROW_FILL = 0xFF1B2029.toInt()
    private const val CELL_FILL = 0xFF2A3140.toInt()
    private const val ON_BAND = 0xFF1B1B1F.toInt()
    private const val TEXT = 0xFFEEF0F5.toInt()
    private const val MUTED = 0xFFA3A9B8.toInt()
    private const val FAINT = 0xFF6B7385.toInt()
    private const val ON_CELL = 0xFFD6DBE6.toInt()
    private const val PILL_FILL = 0xFFA9C7FF.toInt()
    private const val ON_PILL = 0xFF0B2A55.toInt()

    /** One tier on the picture: a line (or, for a short list, lines) of covers, or a thin band. */
    sealed interface Row {
        val tier: Tier

        /** [shown] covers, then a "+[more]" cell when the tier has more than fit. */
        data class Covers(override val tier: Tier, val shown: List<TierItem>, val more: Int, val small: Boolean) : Row

        data class Thin(override val tier: Tier) : Row
    }

    /** The rows, top to bottom, and how many ranked cards are not on the picture. */
    data class Plan(val rows: List<Row>, val left: Int, val ranked: Int)

    /** The covers the picture draws, in order: only these are worth fetching. */
    fun drawnItems(list: TierList): List<TierItem> =
        plan(list, titleLines(list.title, titlePaint()).size).rows.flatMap { (it as? Row.Covers)?.shown.orEmpty() }

    /** The layout for a title of [titleLineCount] lines. A list of up to [SMALL_LIST] cards whole, if it fits; else twelve covers. */
    fun plan(list: TierList, titleLineCount: Int): Plan {
        val tiers = list.tiers.filterNot { it.isPool }
        val ranked = tiers.sumOf { it.items.size }
        val room = rowsBottom() - rowsTop(titleLineCount)
        if (ranked <= SMALL_LIST) {
            val whole = tiers.map { tier ->
                if (tier.items.isEmpty()) Row.Thin(tier) else Row.Covers(tier, tier.items, more = 0, small = true)
            }
            if (heightOf(whole) <= room) return Plan(whole, left = 0, ranked = ranked)
        }
        val rows = tiers.map<Tier, Row> { Row.Thin(it) }.toMutableList()
        var budget = COVER_BUDGET
        val cells = cellsPerLine(BIG_W, BIG_GAP)
        for ((index, tier) in tiers.withIndex().filter { it.value.items.isNotEmpty() }) {
            val shown = if (tier.items.size <= minOf(cells, budget)) tier.items.size else minOf(cells - 1, budget)
            val candidate = Row.Covers(tier, tier.items.take(shown), more = tier.items.size - shown, small = false)
            val tried = rows.toMutableList().also { it[index] = candidate }
            if (budget == 0 || heightOf(tried) > room) break
            rows[index] = candidate
            budget -= shown
        }
        val left = ranked - rows.sumOf { (it as? Row.Covers)?.shown?.size ?: 0 }
        return Plan(rows, left = left, ranked = ranked)
    }

    /** The picture. [pictures] are by card id; a card without one is drawn as its title. With [names], a cover carries its name along its foot. */
    fun render(list: TierList, words: PictureWords, pictures: Map<Long, Bitmap>, names: Boolean = false): Bitmap {
        val title = titlePaint()
        val titleLines = titleLines(list.title, title)
        val plan = plan(list, titleLines.size)

        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)
        val glow = Paint().apply {
            shader = RadialGradient(GLOW_X, GLOW_Y, GLOW_RADIUS, GLOW, 0, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glow)

        var baseline = TOP + title.textSize
        titleLines.forEachIndexed { index, line ->
            if (index > 0) baseline += title.textSize * TITLE_LINE
            canvas.drawText(line, SIDE, baseline, title)
        }
        drawByLine(canvas, words, baseline + BY_GAP + BY_SIZE)

        var y = rowsTop(titleLines.size)
        for (row in plan.rows) {
            val height = heightOf(row)
            drawRow(canvas, row, y, height, words, pictures, names)
            y += height + ROW_GAP
        }
        if (plan.left > 0) {
            val see = text(SEE_SIZE, MUTED, bold = true).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(words.seeAll(plan.ranked), WIDTH / 2f, y - ROW_GAP + SEE_GAP + SEE_SIZE, see)
        }
        drawFooter(canvas, words)
        return bitmap
    }

    private fun rowsTop(titleLineCount: Int) =
        TOP + TITLE_SIZE * (1 + TITLE_LINE * (titleLineCount - 1)) + BY_GAP + BY_SIZE + ROWS_GAP_ABOVE

    /** Rows end where the "see all" line still fits above the footer. */
    private fun rowsBottom() = HEIGHT - FOOT_BOTTOM - PILL_HEIGHT - SEE_ROOM - SEE_GAP

    private fun cellsPerLine(width: Float, gap: Float): Int =
        ((WIDTH - 2 * SIDE - BAND - 2 * CELLS_PADDING + gap) / (width + gap)).toInt()

    private fun heightOf(rows: List<Row>): Float = rows.sumOf { heightOf(it).toDouble() }.toFloat() + ROW_GAP * (rows.size - 1).coerceAtLeast(0)

    private fun heightOf(row: Row): Float = when (row) {
        is Row.Thin -> THIN
        is Row.Covers -> {
            val (h, gap) = if (row.small) SMALL_H to SMALL_GAP else BIG_H to BIG_GAP
            val perLine = cellsPerLine(if (row.small) SMALL_W else BIG_W, gap)
            val cells = row.shown.size + if (row.more > 0) 1 else 0
            val lines = ceil(cells / perLine.toFloat()).toInt().coerceAtLeast(1)
            2 * CELLS_PADDING + lines * h + (lines - 1) * gap
        }
    }

    private fun titlePaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT
        textSize = TITLE_SIZE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun text(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    /**
     * The whole name in two lines at most, broken between words: a long one takes a smaller size first,
     * and only what still does not fit ends in an ellipsis. Leaves [paint] at the size chosen.
     */
    private fun titleLines(title: String, paint: TextPaint): List<String> {
        val width = WIDTH - 2 * SIDE
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

    /** "by Danylo · 176 cards ranked", the name bright and bold, the rest muted. */
    private fun drawByLine(canvas: Canvas, words: PictureWords, baseline: Float) {
        val muted = text(BY_SIZE, MUTED)
        val line = TextUtils.ellipsize(words.byLine, muted, WIDTH - 2 * SIDE, TextUtils.TruncateAt.END).toString()
        val author = words.author?.takeIf { it.isNotBlank() }
        val at = author?.let { line.indexOf(it) } ?: -1
        if (author == null || at < 0) {
            canvas.drawText(line, SIDE, baseline, muted)
            return
        }
        val bold = text(BY_SIZE, TEXT, bold = true)
        var x = SIDE
        val before = line.substring(0, at)
        canvas.drawText(before, x, baseline, muted)
        x += muted.measureText(before)
        canvas.drawText(author, x, baseline, bold)
        x += bold.measureText(author)
        canvas.drawText(line.substring(at + author.length), x, baseline, muted)
    }

    private fun drawRow(canvas: Canvas, row: Row, top: Float, height: Float, words: PictureWords, pictures: Map<Long, Bitmap>, names: Boolean) {
        val left = SIDE
        val right = WIDTH - SIDE
        val bottom = top + height
        val shape = Path().apply { addRoundRect(RectF(left, top, right, bottom), ROW_RADIUS, ROW_RADIUS, Path.Direction.CW) }
        canvas.withClip(shape) {
            drawRect(left, top, right, bottom, Paint().apply { color = ROW_FILL })
            // The picture is always dark, so the band takes the tier's dark colour.
            val band = parseTierColor(row.tier.colorDark, fallback = androidx.compose.ui.graphics.Color(CELL_FILL))
            drawRect(left, top, left + BAND, bottom, Paint().apply { color = band.toArgb() })
        }
        val label = text(if (row is Row.Thin) THIN_LABEL_SIZE else LABEL_SIZE, ON_BAND, bold = true).apply { textAlign = Paint.Align.CENTER }
        while (label.textSize > LABEL_SMALLEST && label.measureText(row.tier.label) > LABEL_ROOM) label.textSize -= 2f
        val labelText = TextUtils.ellipsize(row.tier.label, label, LABEL_ROOM, TextUtils.TruncateAt.END).toString()
        canvas.drawText(labelText, left + BAND / 2, top + height / 2 + label.textSize * BASELINE_DROP, label)

        when (row) {
            is Row.Thin -> {
                val count = row.tier.items.size
                val paint = if (count == 0) text(EMPTY_SIZE, FAINT) else text(COUNT_SIZE, MUTED, bold = true)
                val line = if (count == 0) words.nothingYet else words.cards(count)
                canvas.drawText(line, left + BAND + TEXT_INSET, top + height / 2 + paint.textSize * BASELINE_DROP, paint)
            }
            is Row.Covers -> drawCovers(canvas, row, top, pictures, names)
        }
    }

    private fun drawCovers(canvas: Canvas, row: Row.Covers, top: Float, pictures: Map<Long, Bitmap>, names: Boolean) {
        val (w, h, gap) = if (row.small) Triple(SMALL_W, SMALL_H, SMALL_GAP) else Triple(BIG_W, BIG_H, BIG_GAP)
        val perLine = cellsPerLine(w, gap)
        val startX = SIDE + BAND + CELLS_PADDING
        val startY = top + CELLS_PADDING
        fun frameAt(index: Int): RectF {
            val x = startX + (index % perLine) * (w + gap)
            val y = startY + (index / perLine) * (h + gap)
            return RectF(x, y, x + w, y + h)
        }
        val cell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CELL_FILL }
        val tileText = text(TILE_TEXT_SIZE * (w / BIG_W), ON_CELL).apply { textAlign = Paint.Align.CENTER }
        row.shown.forEachIndexed { index, item ->
            val frame = frameAt(index)
            val picture = pictures[item.id]
            if (picture != null) {
                val rounded = Path().apply { addRoundRect(frame, TILE_RADIUS, TILE_RADIUS, Path.Direction.CW) }
                canvas.withClip(rounded) {
                    drawBitmap(picture, cover(picture, frame), frame, Paint(Paint.FILTER_BITMAP_FLAG))
                    if (names && item.title.isNotBlank()) drawNameStrip(this, item.title, frame)
                }
            } else {
                canvas.drawRoundRect(frame, TILE_RADIUS, TILE_RADIUS, cell)
                drawTitle(canvas, item.title, frame, tileText)
            }
        }
        if (row.more > 0) {
            val frame = frameAt(row.shown.size)
            canvas.drawRoundRect(frame, TILE_RADIUS, TILE_RADIUS, cell)
            val more = text(MORE_SIZE * (w / BIG_W), ON_CELL, bold = true).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("+${row.more}", frame.centerX(), frame.centerY() + more.textSize * BASELINE_DROP, more)
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
        val text = text(NAME_TEXT_SIZE * (frame.width() / BIG_W).coerceAtLeast(NAME_SCALE_MIN), android.graphics.Color.WHITE, bold = true).apply {
            textAlign = Paint.Align.CENTER
        }
        val maxWidth = frame.width() - 2 * NAME_PADDING
        val words = title.split(' ', '\n', '\t').filter { it.isNotEmpty() }
        val wrapped = wrapWords(words, maxWidth.toInt()) { text.measureText(it).toInt() }
        val lines = listOfNotNull(wrapped.firstOrNull(), wrapped.drop(1).joinToString(" ").takeIf { it.isNotEmpty() })
            .map { TextUtils.ellipsize(it, text, maxWidth, TextUtils.TruncateAt.END).toString() }
        val lineHeight = text.textSize * NAME_LINE
        val stripTop = frame.bottom - lines.size * lineHeight - NAME_SHADE_RISE
        val shade = Paint().apply {
            shader = LinearGradient(0f, stripTop, 0f, frame.bottom, android.graphics.Color.TRANSPARENT, NAME_SHADE, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(frame.left, stripTop, frame.right, frame.bottom, shade)
        var y = frame.bottom - NAME_PADDING - (lines.size - 1) * lineHeight
        for (line in lines) {
            canvas.drawText(line, frame.centerX(), y, text)
            y += lineHeight
        }
    }

    private fun drawTitle(canvas: Canvas, title: String, frame: RectF, paint: TextPaint) {
        val maxWidth = frame.width() - TILE_TITLE_INSET
        val lines = mutableListOf<String>()
        var rest = title.trim()
        while (rest.isNotEmpty() && lines.size < TILE_TITLE_LINES) {
            val cut = lineEnd(rest, paint.breakText(rest, true, maxWidth, null))
            lines += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty() && lines.isNotEmpty()) {
            lines[lines.lastIndex] = TextUtils.ellipsize(lines.last() + " " + rest, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        }
        val lineHeight = paint.textSize * TILE_TITLE_LINE
        var y = frame.centerY() - (lines.size - 1) * lineHeight / 2 + paint.textSize * BASELINE_DROP
        for (line in lines) {
            canvas.drawText(line, frame.centerX(), y, paint)
            y += lineHeight
        }
    }

    /**
     * The logo and name on the left; on the right the list's address whole,
     * smaller if it must be. An address cut short leads nowhere, so one that
     * still does not fit gives way to [PictureWords.shortFooter].
     */
    private fun drawFooter(canvas: Canvas, words: PictureWords) {
        val middle = HEIGHT - FOOT_BOTTOM - PILL_HEIGHT / 2
        ShareLogo.draw(canvas, RectF(SIDE, middle - LOGO / 2, SIDE + LOGO, middle + LOGO / 2))
        val brand = text(BRAND_SIZE, TEXT, bold = true)
        val brandLeft = SIDE + LOGO + LOGO_GAP
        canvas.drawText(BRAND, brandLeft, middle + BRAND_SIZE * BASELINE_DROP, brand)

        val pill = text(PILL_TEXT, ON_PILL, bold = true)
        val room = WIDTH - SIDE - (brandLeft + brand.measureText(BRAND) + 2 * LOGO_GAP) - 2 * PILL_PAD_X
        var footer = words.footer
        while (pill.textSize > PILL_SMALLEST && pill.measureText(footer) > room) pill.textSize -= 1f
        if (pill.measureText(footer) > room) {
            footer = words.shortFooter
            pill.textSize = PILL_TEXT
            while (pill.textSize > PILL_SMALLEST && pill.measureText(footer) > room) pill.textSize -= 1f
        }
        val width = pill.measureText(footer) + 2 * PILL_PAD_X
        val frame = RectF(WIDTH - SIDE - width, middle - PILL_HEIGHT / 2, WIDTH - SIDE, middle + PILL_HEIGHT / 2)
        canvas.drawRoundRect(frame, PILL_HEIGHT / 2, PILL_HEIGHT / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PILL_FILL })
        canvas.drawText(footer, frame.left + PILL_PAD_X, middle + pill.textSize * BASELINE_DROP, pill)
    }
}

/** Where a line of [text] with room for [fits] chars ends: at a space if it can, never inside an emoji. */
internal fun lineEnd(text: String, fits: Int): Int {
    if (fits >= text.length) return text.length
    val space = text.lastIndexOf(' ', fits)
    if (space > 0) return space
    return text.wholeCutAtOrBefore(fits).takeIf { it > 0 } ?: text.nextWholeCut(0)
}
