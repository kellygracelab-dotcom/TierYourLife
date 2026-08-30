package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import java.security.MessageDigest

/**
 * A short stand-in for everything about a board that is worth keeping.
 *
 * This is what tells sync that a board changed, instead of a flag set by hand
 * on every write. The flag would have to be set in every place that touches a
 * card -- a rename, a drag, a colour, emptying a tier -- and the first one
 * anybody forgets is a board that silently stops being backed up. Reading the
 * board and looking at it cannot be forgotten.
 *
 * Two things are deliberately not in it:
 *
 * **The row ids.** They are this database's own counters, so the same board on
 * two phones has different ones. A fingerprint built from them would say every
 * board differs from itself.
 *
 * **`publishedId`.** Publishing is between one person and the feed; a phone
 * that has not published anything should not have the board pushed back at it
 * as a change.
 */
object BoardFingerprint {

    fun of(board: TierListEntity, tiers: List<TierEntity>, items: List<TierItemEntity>): String {
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

        // Sorted by uid rather than by position: two phones that hold the same
        // board in the same order must reach the same fingerprint, and reading
        // order is not guaranteed to be either.
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
                    item.imageUrl,
                    item.source,
                    item.deletedAt?.toString(),
                )
            }
        }

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * Fields go in with their lengths, so that moving a character from the end
     * of one to the start of the next cannot land on the same digest -- a
     * board titled "Sci" with a tier "fi" would otherwise be a board titled
     * "Scifi" with a tier "".
     *
     * Absent is not empty. A caption nobody wrote and a caption someone
     * cleared are different states, and the marker for absent cannot be
     * mistaken for a length.
     */
    private fun MessageDigest.write(vararg fields: String?) {
        fields.forEach { field ->
            val encoded = if (field == null) "~|" else "${field.length}:$field|"
            update(encoded.toByteArray(Charsets.UTF_8))
        }
    }
}
