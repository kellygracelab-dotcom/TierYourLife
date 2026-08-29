package com.artiuillab.tieryourlife.feature.tier.domain.model

/** Why a list could not be published, in terms a screen can explain. */
enum class PublishError {
    NotSignedIn,
    NothingToPublish,
    NoCategory,
    TooManyLists,
    Offline,
    Unknown,
}

class PublishRefused(val error: PublishError) : Exception(error.name)
