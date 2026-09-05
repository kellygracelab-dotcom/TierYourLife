package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
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

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

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
