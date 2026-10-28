package com.artiuillab.tieryourlife.feature.tier.domain.sync

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
        eat(list.category)
        eat(picture(list.coverImageUrl, pictureIdOf))
        list.coverImageUrls.forEach { eat(picture(it, pictureIdOf)) }

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
        // Only when true, last: every copy published before this keeps its fingerprint, and is not suddenly behind.
        if (list.anonymous) eat(ANONYMOUS)

        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /** Our own picture by its stored name, never the local path, which differs between phones. */
    private fun picture(imageUrl: String?, pictureIdOf: (String?) -> String?): String? =
        imageUrl?.takeIf { it.startsWith("https://") } ?: pictureIdOf(imageUrl)

    /** Distinguishes an absent field from an empty one. */
    private const val NOTHING = "~|"

    private const val ANONYMOUS = "anonymous"
}
