package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.ExpectedUser
import com.artiuillab.tieryourlife.core.network.ExpectedUserMismatch
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShowRankingRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShownRankingDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures

/** Check before private uploads, then bind every public POST (including the existing-board retry) locally. */
internal suspend fun ownedPublication(
    list: TierList,
    expectedOwnerUid: String,
    currentOwner: () -> String?,
    pictures: OwnPictures,
    api: CommunityApi,
): PublishedIdDto {
    requirePublicationOwner(expectedOwnerUid, currentOwner())
    val request = list.uploadedPublicationRequest(pictures)
    requirePublicationOwner(expectedOwnerUid, currentOwner())
    val actor = ExpectedUser(expectedOwnerUid)
    return sendOut(list.publishedId, request, { api.publishOwned(it, actor) }, { id, body -> api.republishOwned(id, body, actor) })
}

internal fun requirePublicationOwner(expectedOwnerUid: String, currentOwnerUid: String?) {
    if (expectedOwnerUid.isBlank() || expectedOwnerUid != currentOwnerUid) throw ExpectedUserMismatch()
}

/** A nullable actor preserves legacy callers; explicit Show actions bind the account before token selection. */
internal suspend fun rankingForOwner(
    listId: String,
    request: ShowRankingRequestDto,
    expectedOwnerUid: String?,
    currentOwner: () -> String?,
    api: CommunityApi,
): ShownRankingDto {
    if (expectedOwnerUid == null) return api.showMyRanking(listId, request)
    requirePublicationOwner(expectedOwnerUid, currentOwner())
    return api.showMyRankingOwned(listId, request, ExpectedUser(expectedOwnerUid))
}
