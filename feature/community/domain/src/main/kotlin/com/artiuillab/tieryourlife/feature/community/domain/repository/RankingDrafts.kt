package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice

/** Private progress on a version of a public list; never a public Ranking. */
data class RankingDraft(
    val version: Long,
    val rows: List<List<Int>>,
    val pool: List<Int>,
    val choice: RankingChoice? = null,
    val savedBoardUid: String? = null,
    /** The list's title when the progress was kept, for the list of what was begun. */
    val title: String = "",
) {
    val placed: Int get() = rows.sumOf { it.size }
    val total: Int get() = placed + pool.size
}

/** Someone else's list begun on this device and not saved as a list of one's own yet. */
data class StartedRanking(
    val listId: String,
    val title: String,
    val version: Long,
    val placed: Int,
    val total: Int,
    val keptAt: Long,
)

interface RankingDrafts {
    /** Reads wait for earlier keeps; reopening cannot overtake a confirmed reset. */
    suspend fun read(listId: String, owner: String? = null): RankingDraft?
    suspend fun keep(listId: String, draft: RankingDraft, owner: String? = null)

    /**
     * [owner]'s lists begun and still unsaved, the last touched first: a card
     * or more placed, none of it kept as a list yet. Drafts kept before titles
     * were stored are left out, having nothing to be called by.
     */
    suspend fun started(owner: String? = null): List<StartedRanking> = emptyList()

    /** Tests that do not exercise persistence can leave it out. */
    data object None : RankingDrafts {
        override suspend fun read(listId: String, owner: String?): RankingDraft? = null
        override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) = Unit
    }
}
