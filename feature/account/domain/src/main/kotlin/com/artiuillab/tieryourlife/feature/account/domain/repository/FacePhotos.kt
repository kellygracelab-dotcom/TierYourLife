package com.artiuillab.tieryourlife.feature.account.domain.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop

/**
 * A picture on its way to being a face: cut to a square on this phone, then
 * put in this person's own folder, which only they and the server can read.
 * The server looks at it and makes the copy everybody sees.
 */
interface FacePhotos {

    /**
     * The square face as a file on this phone, answered by its address; quick,
     * nothing leaves the phone, so the screen can show it at once. [crop] null
     * takes the middle square. [sourceUri] is a picker's answer, a file, or a
     * picture on the web.
     */
    suspend fun prepare(sourceUri: String, crop: FaceCrop?): Result<String>

    /** Puts a [prepare]d face in this person's own folder and answers the picture id it was stored under. */
    suspend fun upload(preparedUri: String): Result<String>
}
