package com.artiuillab.tieryourlife.feature.tier.domain.model

/** Why a list could not be published, in terms a screen can explain. */
enum class PublishError {
    NotSignedIn,
    NothingToPublish,
    TooManyLists,
    TooLarge,

    /** One of the photographs on the board may not go into a public feed. */
    PictureRefused,

    /** Something written on the board may not go into a public feed. */
    WordingRefused,
    Offline,
    Unknown,
}

class PublishRefused(val error: PublishError) : Exception(error.name)
