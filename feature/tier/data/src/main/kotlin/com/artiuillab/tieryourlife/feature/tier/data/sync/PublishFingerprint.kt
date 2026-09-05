package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import java.security.MessageDigest

/**
 * Sibling to [BoardFingerprint], separate on purpose: that one asks "have two
 * phones the same board", this one "is the copy in the feed still what this
 * board says". Covers exactly what `toRequest` sends; the display mode never
 * travels, so changing it does not leave the copy behind.
 */
object PublishFingerprint {

    fun of(list: TierList, pictureIdOf: (String?) -> String?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun eat(value: String?) {
            // Length-prefixed, so "ab" + "c" is not "a" + "bc".
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

    /** Our own picture by its stored name, never the local path, which differs between phones. */
    private fun picture(imageUrl: String?, pictureIdOf: (String?) -> String?): String? =
        imageUrl?.takeIf { it.startsWith("https://") } ?: pictureIdOf(imageUrl)

    /** Distinguishes an absent field from an empty one. */
    private const val NOTHING = "~|"
}
