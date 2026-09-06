package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * This phone's own pictures as the account knows them. Publishing needs
 * both halves: the name a picture travels under, and the picture sent up
 * before the server is asked to copy it.
 */
interface OwnPictures {
    /** The id of a picture this app keeps itself, or null for one on somebody else's server. */
    fun pictureIdOf(imageUrl: String?): String?

    /** Sends these up now, whatever the Wi-Fi rule says, and answers with the ones that are there. */
    suspend fun sendNow(pictureIds: List<String>): Set<String>
}
