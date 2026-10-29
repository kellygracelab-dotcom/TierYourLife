package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal

/** A refused `PUT lists/{id}/rankings/mine`, by the code in its body; anything else is a failure to try again. */
fun Throwable.rankingRefusal(): RankingRefusal = when (proxyCode()) {
    "NOT_SIGNED_IN" -> RankingRefusal.NotSignedIn
    "BANNED" -> RankingRefusal.Banned
    "OWN_LIST" -> RankingRefusal.OwnList
    else -> RankingRefusal.Failed
}
