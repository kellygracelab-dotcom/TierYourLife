package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal

/** A refused `PUT lists/{id}/rankings/mine`, by the code in its body; anything else is a failure to try again. */
fun Throwable.rankingRefusal(): RankingRefusal = when (proxyCode()) {
    "NOT_SIGNED_IN" -> RankingRefusal.NotSignedIn
    "BANNED" -> RankingRefusal.Banned
    "OWN_LIST" -> RankingRefusal.OwnList
    "NOT_FOUND" -> RankingRefusal.ListGone
    "WEB_PICTURES_DAILY_LIMIT" -> RankingRefusal.WebPicturesToday
    "TOO_LARGE" -> if (isTooManyWebPictures()) RankingRefusal.TooManyWebPictures else RankingRefusal.Failed
    "INVALID" -> when (proxyDetail()) {
        "tierLabels" -> RankingRefusal.BadTierNames
        "rows", "arrangement" -> RankingRefusal.CardsDontMatch
        else -> RankingRefusal.NotAccepted
    }
    else -> RankingRefusal.Failed
}
