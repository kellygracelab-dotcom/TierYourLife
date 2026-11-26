package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.serialization.Serializable

/**
 * What this phone and the account last agreed a board holds, kept so a board
 * changed on both sides can be merged instead of coming down as a second one.
 * Only compared, never written back: each value is the field as both sides
 * read it, the picture by its name or catalogue address rather than the path.
 */
@Serializable
internal data class LastAgreed(
    val fingerprint: String,
    val board: Map<String, String?>,
    val tiers: Map<String, Map<String, String?>>,
    val cards: Map<String, AgreedCard>,
)

@Serializable
internal data class AgreedCard(val tier: String, val position: Int, val fields: Map<String, String?>)

/** One side of a merge: the board here, or the account's as it would be written here. */
internal class BoardSide(val board: TierListEntity, val tiers: List<TierEntity>, val items: List<TierItemEntity>)

/** The merged board, ready for `replaceContents`; the star and this phone's own fields are the caller's. */
internal class MergedBoard(val board: TierListEntity, val tiers: List<IncomingTier>)

private class Field<E>(val name: String, val read: (E) -> String?, val take: (into: E, from: E) -> E)

private enum class Pick { OURS, THEIRS }

/** Changed on both sides in the same field of the same thing: only then is the account's kept as a second board. */
private class Conflict : Exception()

/**
 * Danylo, 25.11: a second board appeared whenever the phone and the site both
 * had one open. Changed on both sides, each field of the board, of each tier
 * and of each card is taken from the side that changed it; a card added on one
 * side stays, one removed on one side and untouched on the other goes. Order
 * within a tier follows the side that reordered it, the other side's cards in
 * where they stood there. Null only when one field of one thing changed on both.
 */
internal class ThreeWayMerge(private val pictureIdOf: (String?) -> String?) {

    fun agreedOf(fingerprint: String, side: BoardSide): LastAgreed {
        val tierUids = side.tiers.associate { it.id to it.uid }
        return LastAgreed(
            fingerprint = fingerprint,
            board = boardFields.readAll(side.board),
            tiers = side.tiers.associate { it.uid to tierFields.readAll(it) },
            cards = side.items.mapNotNull { item ->
                val tier = tierUids[item.tierId] ?: return@mapNotNull null
                item.uid to AgreedCard(tier, item.position, cardFields.readAll(item))
            }.toMap(),
        )
    }

    fun merge(base: LastAgreed, ours: BoardSide, theirs: BoardSide): MergedBoard? = try {
        val board = mergeFields(boardFields, base.board, ours.board, theirs.board)
        val tiers = mergeAll(base.tiers, ours.tiers.associateBy { it.uid }, theirs.tiers.associateBy { it.uid }, tierFields)
        val oursCards = Placed.of(ours)
        val theirsCards = Placed.of(theirs)
        val cards = mergeAll(base.cards.mapValues { it.value.fields + (TIER to it.value.tier) }, oursCards.fieldsByUid, theirsCards.fieldsByUid, placedFields)
        if (cards.values.any { it.tier !in tiers }) throw Conflict()
        val ordered = tiers.values.sortedWith(compareBy({ it.position }, { it.uid })).mapIndexed { index, tier ->
            val members = cards.values.filter { it.tier == tier.uid }.associateBy { it.item.uid }
            val order = orderOf(tier.uid, members.keys, base, oursCards, theirsCards)
            IncomingTier(
                tier = tier.copy(id = index.toLong(), tierListId = 0),
                items = order.mapIndexed { at, uid -> members.getValue(uid).item.copy(id = 0, tierId = index.toLong(), position = at) },
            )
        }
        MergedBoard(board, ordered)
    } catch (expected: Conflict) {
        null
    }

    /** A card with the uid of the tier it is in, so moving it is a field like any other. */
    private data class PlacedCard(val item: TierItemEntity, val tier: String)

    private class Placed(val fieldsByUid: Map<String, PlacedCard>, val orderByTier: Map<String, List<String>>) {
        companion object {
            fun of(side: BoardSide): Placed {
                val tierUids = side.tiers.associate { it.id to it.uid }
                val placed = side.items.mapNotNull { item -> tierUids[item.tierId]?.let { PlacedCard(item, it) } }
                return Placed(
                    fieldsByUid = placed.associateBy { it.item.uid },
                    orderByTier = placed.groupBy { it.tier }.mapValues { (_, cards) -> cards.sortedBy { it.item.position }.map { it.item.uid } },
                )
            }
        }
    }

    private fun orderOf(tier: String, members: Set<String>, base: LastAgreed, ours: Placed, theirs: Placed): List<String> {
        val before = base.cards.filterValues { it.tier == tier }.entries.sortedBy { it.value.position }.map { it.key }
        val here = ours.orderByTier[tier].orEmpty()
        val there = theirs.orderByTier[tier].orEmpty()
        val (backbone, other) = if (reordered(here, before) && !reordered(there, before)) here to there else there to here
        val order = backbone.filter { it in members }.toMutableList()
        other.forEachIndexed { index, uid ->
            if (uid !in members || uid in order) return@forEachIndexed
            // After the last of the cards it followed there, wherever they now stand.
            val after = other.subList(0, index).maxOfOrNull { order.indexOf(it) } ?: -1
            order.add(after + 1, uid)
        }
        // Cannot happen, as every member is on one side; kept so no card is ever lost.
        order += members.filterNot { it in order }.sorted()
        return order
    }

    /** Whether the cards both lists hold stand in another order. */
    private fun reordered(side: List<String>, before: List<String>): Boolean {
        val common = before.toSet().intersect(side.toSet())
        return side.filter { it in common } != before.filter { it in common }
    }

    private fun <E> mergeAll(
        base: Map<String, Map<String, String?>>,
        ours: Map<String, E>,
        theirs: Map<String, E>,
        fields: List<Field<E>>,
    ): Map<String, E> = (ours.keys + theirs.keys).mapNotNull { uid ->
        val before = base[uid]
        val here = ours[uid]
        val there = theirs[uid]
        val merged = when {
            here != null && there != null -> mergeFields(fields, before.orEmpty(), here, there)
            // Removed on the other side: gone, unless changed here since.
            here != null -> keptBeside(before, fields.readAll(here))?.let { here }
            there != null -> keptBeside(before, fields.readAll(there))?.let { there }
            else -> null
        }
        merged?.let { uid to it }
    }.toMap()

    /** A thing on one side only: new there, kept; or removed on the other, gone unless it changed meanwhile. */
    private fun keptBeside(before: Map<String, String?>?, now: Map<String, String?>): Unit? = when {
        before == null -> Unit
        before == now -> null
        else -> throw Conflict()
    }

    private fun <E> mergeFields(fields: List<Field<E>>, before: Map<String, String?>, ours: E, theirs: E): E =
        fields.fold(ours) { merged, field ->
            when (pick(before[field.name], field.read(ours), field.read(theirs), field.name in before)) {
                Pick.OURS -> merged
                Pick.THEIRS -> field.take(merged, theirs)
            }
        }

    private fun pick(before: String?, here: String?, there: String?, known: Boolean): Pick = when {
        here == there -> Pick.OURS
        known && here == before -> Pick.THEIRS
        known && there == before -> Pick.OURS
        else -> throw Conflict()
    }

    private fun <E> List<Field<E>>.readAll(of: E): Map<String, String?> = associate { it.name to it.read(of) }

    private val boardFields: List<Field<TierListEntity>> = listOf(
        Field("title", { it.title }) { into, from -> into.copy(title = from.title) },
        Field("displayMode", { it.displayMode }) { into, from -> into.copy(displayMode = from.displayMode) },
        Field("category", { it.category }) { into, from -> into.copy(category = from.category) },
        Field("cover", { pictureIdOf(it.coverImageUrl) ?: it.coverImageUrl }) { into, from -> into.copy(coverImageUrl = from.coverImageUrl) },
        Field("collage", { it.coverImageUrls }) { into, from -> into.copy(coverImageUrls = from.coverImageUrls) },
        Field("coverFocus", { listOf(it.coverFocusX, it.coverFocusY, it.coverFocusZoom).joinToString() }) { into, from ->
            into.copy(coverFocusX = from.coverFocusX, coverFocusY = from.coverFocusY, coverFocusZoom = from.coverFocusZoom)
        },
        Field("authorName", { it.authorName }) { into, from -> into.copy(authorName = from.authorName) },
        Field("deletedAt", { it.deletedAt?.toString() }) { into, from -> into.copy(deletedAt = from.deletedAt) },
        Field("anonymous", { it.anonymous.toString() }) { into, from -> into.copy(anonymous = from.anonymous) },
        Field("play", { board -> board.play?.let { BoardFingerprint.canonicalPlay(it) ?: it } }) { into, from -> into.copy(play = from.play) },
        Field("origin", { listOf(it.takenFrom, it.originVersion).joinToString() }) { into, from ->
            into.copy(takenFrom = from.takenFrom, originVersion = from.originVersion)
        },
        Field("rankingChoice", { it.rankingChoice }) { into, from -> into.copy(rankingChoice = from.rankingChoice) },
    )

    private val tierFields: List<Field<TierEntity>> = listOf(
        Field("position", { it.position.toString() }) { into, from -> into.copy(position = from.position) },
        Field("label", { it.label }) { into, from -> into.copy(label = from.label) },
        Field("caption", { it.caption }) { into, from -> into.copy(caption = from.caption) },
        Field("colour", { "${it.colorLight} ${it.colorDark}" }) { into, from -> into.copy(colorLight = from.colorLight, colorDark = from.colorDark) },
        Field("isPool", { it.isPool.toString() }) { into, from -> into.copy(isPool = from.isPool) },
    )

    private val cardFields: List<Field<TierItemEntity>> = listOf(
        Field("title", { it.title }) { into, from -> into.copy(title = from.title) },
        Field("picture", ::pictureOf) { into, from -> into.copy(imageUrl = from.imageUrl, attribution = from.attribution) },
        Field("source", { it.source }) { into, from -> into.copy(source = from.source) },
        Field("deletedAt", { it.deletedAt?.toString() }) { into, from -> into.copy(deletedAt = from.deletedAt) },
        Field("credit", { it.credit }) { into, from -> into.copy(credit = from.credit) },
        Field("sourceIndex", { it.sourceIndex?.toString() }) { into, from -> into.copy(sourceIndex = from.sourceIndex) },
        Field("focus", { listOf(it.focusX, it.focusY, it.focusZoom).joinToString() }) { into, from ->
            into.copy(focusX = from.focusX, focusY = from.focusY, focusZoom = from.focusZoom)
        },
    )

    private val placedFields: List<Field<PlacedCard>> = cardFields.map { field ->
        Field<PlacedCard>(field.name, { field.read(it.item) }) { into, from -> into.copy(item = field.take(into.item, from.item)) }
    } + Field(TIER, { it.tier }) { into, from -> into.copy(tier = from.tier) }

    /**
     * The account shows a catalogue picture from its shared copy and keeps the
     * catalogue's address beside it: the same picture, so taking the copy is
     * no change to the card.
     */
    private fun pictureOf(item: TierItemEntity): String? {
        pictureIdOf(item.imageUrl)?.let { return it }
        val original = item.attribution?.originalUrl
        return if (original != null && item.imageUrl?.let(::isSharedCopy) == true) original else item.imageUrl
    }

    private fun isSharedCopy(url: String) = SHARED_COPIES.any { it in url }

    private companion object {
        const val TIER = "tier"
        val SHARED_COPIES = listOf("published%2Fcatalogue%2F", "/published/catalogue/")
    }
}
