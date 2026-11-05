package com.artiuillab.tieryourlife.feature.community.domain.model

/** What the server made of taking a proposal back. */
sealed interface Withdrawal {

    /** Taken back while it waited. */
    data object Withdrawn : Withdrawal

    /** Only its notice put away: a moderator had declined or merged it. */
    data class Dismissed(val status: SectionStatus) : Withdrawal

    /** Nothing taken back: a moderator approved it first (409 SETTLED). */
    data class Settled(val status: SectionStatus) : Withdrawal
}
