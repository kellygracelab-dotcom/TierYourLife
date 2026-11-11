package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice

/** Private progress on a version of a public list; never a public Ranking. */
data class RankingDraft(
    val version: Long,
    val rows: List<List<Int>>,
    val pool: List<Int>,
    val choice: RankingChoice? = null,
    val savedBoardUid: String? = null,
)

interface RankingDrafts {
    /** Reads wait for earlier keeps; reopening cannot overtake a confirmed reset. */
    suspend fun read(listId: String, owner: String? = null): RankingDraft?
    suspend fun keep(listId: String, draft: RankingDraft, owner: String? = null)

    /** Tests that do not exercise persistence can leave it out. */
    data object None : RankingDrafts {
        override suspend fun read(listId: String, owner: String?): RankingDraft? = null
        override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) = Unit
    }
}
