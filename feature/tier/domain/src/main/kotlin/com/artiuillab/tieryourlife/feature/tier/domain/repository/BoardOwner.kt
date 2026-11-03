package com.artiuillab.tieryourlife.feature.tier.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whose boards "your lists" are: the signed-in account's, or, while nobody
 * is, the ones this phone made without an account. Every read is of one
 * owner's boards; the others stay on the phone, out of sight.
 */
interface BoardOwner {

    /**
     * Fires when the boards that are yours may be other ones: somebody signed
     * in or out, or boards moved into an account. Whatever was read before is
     * somebody else's now; read again.
     */
    val changes: Flow<Unit>
}
