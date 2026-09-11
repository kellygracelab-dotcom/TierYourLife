package com.artiuillab.tieryourlife.feature.community.domain.model

/** Why a list could not be published, in terms a screen can explain. */
sealed interface PublishError {
    data object NotSignedIn : PublishError

    data object NothingToPublish : PublishError

    data object TooManyLists : PublishError

    data object TooLarge : PublishError

    /** One of the photographs on the board may not go into a public feed. */
    data object PictureRefused : PublishError

    /** A moderator took one of their lists down and kept them from publishing. Null for a ban with no end. */
    data class Banned(val untilMillis: Long?) : PublishError

    data object Offline : PublishError

    /** Play would not vouch for this installation. Retrying will not help. */
    data object NotVerified : PublishError

    data object Unknown : PublishError
}

class PublishRefused(val error: PublishError) : Exception(error.toString())
