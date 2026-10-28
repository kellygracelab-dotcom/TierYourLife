package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop

/** A picture's size in pixels, as it stands upright. */
data class PictureSize(val width: Int, val height: Int)

/**
 * A picture on its way to being a section's: measured, cut to 16:10 on this
 * phone, then put in this person's own folder, which only they and the server
 * can read. The server copies it out once a moderator approves.
 */
interface SectionPictures {

    /** How big the picked picture is, upright; [sourceUri] is a picker's answer. */
    suspend fun measure(sourceUri: String): Result<PictureSize>

    /** Cuts [crop] out as a WebP and puts it in this person's folder; answers the picture id it went under. */
    suspend fun upload(sourceUri: String, crop: PictureCrop): Result<String>
}
