package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/**
 * Tells sync that a board changed, instead of a flag set on every write. Left
 * out on purpose: row ids, which differ per phone for the same board, and
 * publishedId, which is between one person and the feed.
 */
object BoardFingerprint {

    /**
     * [pictureIdOf] turns a local image path into the picture's name: the path
     * differs per phone, the name does not. Without it two phones would never
     * agree they hold the same board.
     */
    fun of(
        board: TierListEntity,
        tiers: List<TierEntity>,
        items: List<TierItemEntity>,
        pictureIdOf: (String?) -> String?,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.write(
            board.uid,
            board.title,
            board.displayMode,
            board.category,
            board.coverImageUrl,
            board.coverImageUrls,
            board.authorName,
            board.deletedAt?.toString(),
        )

        // By uid, not position: two phones holding the same board must reach
        // the same fingerprint, and reading order is not guaranteed.
        val itemsByTier = items.groupBy { it.tierId }
        tiers.sortedBy { it.uid }.forEach { tier ->
            digest.write(
                tier.uid,
                tier.position.toString(),
                tier.label,
                tier.caption,
                tier.colorLight,
                tier.colorDark,
                tier.isPool.toString(),
            )
            itemsByTier[tier.id].orEmpty().sortedBy { it.uid }.forEach { item ->
                digest.write(
                    item.uid,
                    item.position.toString(),
                    item.title,
                    pictureIdOf(item.imageUrl) ?: item.imageUrl?.takeIf { it.startsWith("http") },
                    item.source,
                    item.deletedAt?.toString(),
                )
            }
        }
        // After the last card and only when there is something to say, so a
        // board with neither keeps the fingerprint it always had. The site
        // writes the same two fields: without the game, a game saved on an
        // untouched board would never sync.
        if (board.anonymous) digest.write(ANONYMOUS)
        board.play?.let(::canonicalPlay)?.let { digest.write(it) }

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private const val ANONYMOUS = "anonymous"

    /**
     * The saved game as both clients write it, not as the server's bytes
     * happen to come: no whitespace, the keys in one order, `scope` only when
     * present and not empty, and `keepTier` only when true. Null when the text is not a game.
     */
    internal fun canonicalPlay(raw: String): String? {
        val game = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
        val fields = PLAY_KEYS.mapNotNull { key -> game[key]?.takeUnless { it is JsonNull }?.let { key to it } } +
            listOfNotNull(game["scope"]?.takeUnless { it is JsonNull || it == JsonArray(emptyList()) }?.let { "scope" to it }) +
            listOfNotNull(game["keepTier"]?.takeIf { it == JsonPrimitive(true) }?.let { "keepTier" to it })
        return fields.joinToString(",", "{", "}") { (key, value) -> "${JsonPrimitive(key)}:$value" }
    }

    private val PLAY_KEYS = listOf("kind", "count", "seed", "refine", "ids", "history", "round", "startedAt")

    /**
     * Fields go in with their lengths, so "Sci" + "fi" is not "Scifi" + "".
     * Absent is not empty, and its marker cannot be mistaken for a length.
     */
    private fun MessageDigest.write(vararg fields: String?) {
        fields.forEach { field ->
            val encoded = if (field == null) "~|" else "${field.length}:$field|"
            update(encoded.toByteArray(Charsets.UTF_8))
        }
    }
}
