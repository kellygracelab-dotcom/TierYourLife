package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * A photo from this phone on its way to being a face: shrunk to a square and
 * put in this person's own folder, which only they and the server can read.
 * The server looks at it and makes the copy everybody sees.
 */
interface FacePhotos {

    /** Answers the picture id it was stored under. [sourceUri] is whatever the picker handed over. */
    suspend fun upload(sourceUri: String): Result<String>
}
