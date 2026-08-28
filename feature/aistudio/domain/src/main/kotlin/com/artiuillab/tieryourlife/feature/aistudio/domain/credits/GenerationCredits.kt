package com.artiuillab.tieryourlife.feature.aistudio.domain.credits

/**
 * How many generations are left. Counted on the server, because a balance the
 * device could edit would not be a balance.
 */
interface GenerationCredits {

    /** Null when generation is not metered, or when the count could not be read. */
    suspend fun remaining(): Int?
}
