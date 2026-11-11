package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures

/** Known picture names describe the draft; no bytes are uploaded by this read-only request. */
internal suspend fun publicationPreflight(list: TierList, api: CommunityApi, pictures: OwnPictures): PublicationKind {
    val answer = api.classifyPublication(list.toRequest(pictures, list.ownPictureIds(pictures).toSet()))
    return when (answer.kind) {
        "list" -> PublicationKind.NewList(answer.basedOn?.toDomain(), answer.version?.also { require(it >= 0) })
        "ranking" -> {
            val id = answer.listId?.takeIf { it.isNotBlank() } ?: error("Missing original list")
            val version = answer.version?.takeIf { it >= 0 } ?: error("Missing original version")
            PublicationKind.Ranking(id, version)
        }
        else -> error("Unknown publication kind")
    }
}
