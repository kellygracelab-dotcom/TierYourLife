package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures

/**
 * An address in somebody's own Storage folder, as the website keeps a photo
 * of theirs: it names their account, so no reader may be handed it.
 */
internal fun isPrivatePicture(url: String): Boolean = PRIVATE_PICTURE.containsMatchIn(url)

/** The picture's name inside such an address, which is how it travels instead. */
internal fun privatePictureIdOf(url: String?): String? {
    if (url == null || !isPrivatePicture(url)) return null
    return url.substringBefore('?').split(FOLDER_SLASH).last().takeIf { PICTURE_ID.matches(it) }
}

internal fun webPictureOf(url: String?): String? = url?.takeIf { it.startsWith("https://") && !isPrivatePicture(it) }

/**
 * A single cover that is one of the person's own photographs goes by its
 * name, as a card's does. One the website chose comes as its private
 * address; it is on offer only while a card of this board shows it.
 */
internal fun TierList.coverPictureIdOf(images: OwnPictures): String? =
    images.pictureIdOf(coverImageUrl) ?: privatePictureIdOf(coverImageUrl)?.takeIf { named ->
        tiers.any { tier -> tier.items.any { (images.pictureIdOf(it.imageUrl) ?: privatePictureIdOf(it.imageUrl)) == named } }
    }

private val PRIVATE_PICTURE = Regex("/o/users%2F", RegexOption.IGNORE_CASE)
private val PICTURE_ID = Regex("[A-Za-z0-9_-]{1,100}")
private val FOLDER_SLASH = Regex("%2F", RegexOption.IGNORE_CASE)
