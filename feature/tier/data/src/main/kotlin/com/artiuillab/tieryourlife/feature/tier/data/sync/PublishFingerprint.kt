package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import java.security.MessageDigest

/**
 * What a board looked like the last time it was published.
 *
 * Sibling to [BoardFingerprint], and separate from it on purpose: that one
 * answers "have these two phones got the same board", this one answers "is the
 * copy in the feed still what this board says". They cover different fields.
 * The display mode, for instance, is part of the board and never travels to
 * the feed, so a board whose only change is the display mode has not left its
 * published copy behind, and saying it had would send somebody to republish
 * something that was already right.
 *
 * Covers exactly what `toRequest` sends: the title, the category, the cover,
 * the tiers a reader sees, and every card's name and picture. Not where the
 * cards sit -- that is not published either.
 */
object PublishFingerprint {

    fun of(list: TierList, pictureIdOf: (String?) -> String?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun eat(value: String?) {
            // Length-prefixed, so that two neighbouring fields cannot be slid
            // into one another: "ab" + "c" must not read the same as "a" + "bc".
            digest.update(
                if (value == null) NOTHING.toByteArray() else "${value.length}:$value|".toByteArray(),
            )
        }

        eat(list.title)
        eat(list.category?.id)
        eat(picture(list.coverImageUrl, pictureIdOf))

        list.tiers.filterNot { it.isPool }.forEach { tier ->
            eat(tier.label)
            eat(tier.caption)
            eat(tier.colorLight)
            eat(tier.colorDark)
        }
        // Every card, in the order publishing sends them.
        list.tiers.flatMap { it.items }.forEach { item ->
            eat(item.title)
            eat(picture(item.imageUrl, pictureIdOf))
        }

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * A card's picture as the feed will know it: somebody else's address as it
     * stands, or our own picture by the name it is stored under. The local path
     * is deliberately not used -- it differs between phones and would make the
     * same board look changed on a second one.
     */
    private fun picture(imageUrl: String?, pictureIdOf: (String?) -> String?): String? =
        imageUrl?.takeIf { it.startsWith("https://") } ?: pictureIdOf(imageUrl)

    /** Distinguishes an absent field from an empty one. */
    private const val NOTHING = "~|"
}
