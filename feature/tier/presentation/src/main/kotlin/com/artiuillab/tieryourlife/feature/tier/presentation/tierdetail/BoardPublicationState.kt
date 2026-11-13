package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal data class PublicationPreview(val list: TierList, val kind: PublicationKind, val owner: String?)

internal data class RankingOffer(
    val list: TierList,
    val kind: PublicationKind.Ranking,
    val owner: String?,
    val placed: Int,
    val total: Int,
)

internal data class BoardPublicationState(
    val preview: PublicationPreview? = null,
    val offer: RankingOffer? = null,
    val busy: Boolean = false,
    val error: PublishError? = null,
)
