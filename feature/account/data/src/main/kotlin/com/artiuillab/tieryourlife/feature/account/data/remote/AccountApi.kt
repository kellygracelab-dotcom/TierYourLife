package com.artiuillab.tieryourlife.feature.account.data.remote

import retrofit2.http.DELETE

interface AccountApi {

    /** Under "boards" on the proxy; "account" cannot collide with a board uid, always Firestore's twenty characters. */
    @DELETE("boards/account")
    suspend fun erase()
}
