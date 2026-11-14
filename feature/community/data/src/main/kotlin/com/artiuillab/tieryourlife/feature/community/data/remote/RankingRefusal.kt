package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.ExpectedUserMismatch
import com.artiuillab.tieryourlife.feature.community.domain.model.LikeRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal

/** A refused `PUT lists/{id}/rankings/mine`, by the code in its body; anything else is a failure to try again. */
fun Throwable.rankingRefusal(): RankingRefusal = if (this is ExpectedUserMismatch) RankingRefusal.NotSignedIn else when (proxyCode()) {
    "NOT_SIGNED_IN" -> RankingRefusal.NotSignedIn
    "BANNED" -> RankingRefusal.Banned
    "OWN_LIST" -> RankingRefusal.OwnList
    "NOT_FOUND" -> RankingRefusal.ListGone
    "LIST_CHANGED" -> RankingRefusal.ListChanged
    "WEB_PICTURES_DAILY_LIMIT" -> RankingRefusal.WebPicturesToday
    "TOO_LARGE" -> if (isTooManyWebPictures()) RankingRefusal.TooManyWebPictures else RankingRefusal.Failed
    "INVALID" -> when (proxyDetail()) {
        "tierLabels" -> RankingRefusal.BadTierNames
        "rows", "arrangement" -> RankingRefusal.CardsDontMatch
        else -> RankingRefusal.NotAccepted
    }
    else -> RankingRefusal.Failed
}

/** A refused like (SPEC list-flow К2), by the code in its body; anything else is a failure to try again. */
fun Throwable.likeRefusal(): LikeRefusal = if (this is ExpectedUserMismatch) LikeRefusal.NotSignedIn else when (proxyCode()) {
    "NOT_SIGNED_IN" -> LikeRefusal.NotSignedIn
    "BANNED" -> LikeRefusal.Banned
    "NOT_FOUND" -> LikeRefusal.Gone
    "CANNOT_LIKE" -> LikeRefusal.CannotLike
    "LIKE_LIMIT" -> LikeRefusal.TooMany
    else -> LikeRefusal.Failed
}
