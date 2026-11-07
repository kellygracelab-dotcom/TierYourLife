package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingsPage
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

internal val published = PublishedList(
    summary = PublishedListSummary(
        id = "abc",
        title = "Every A24 film",
        authorUid = "author-1",
        authorName = "Olena M.",
        category = ListCategory.FilmTv.id,
        itemCount = 2,
        updatedAtMillis = 0,
    ),
    tiers = listOf(
        Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(id = 1, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
    ),
    items = listOf(TierItem(0, "Hereditary", null), TierItem(1, "Moonlight", "https://i/2.jpg", credit = "© Jane Doe, CC BY")),
)

internal val myOldRanking = Ranking(
    uid = "me",
    authorName = "Ann",
    authorPhotoUrl = null,
    arrangement = listOf(0, 0),
    tierLabels = listOf("S", "A"),
    updatedAtMillis = 0,
    mine = true,
)

internal class TestCommunity(
    private val rankings: List<Ranking> = emptyList(),
    private val refusesWithdrawal: Boolean = false,
    private val summary: (PublishedListSummary) -> PublishedListSummary = { it },
    private val shownIsNew: Boolean = true,
    private val showRefusal: RankingRefusal? = null,
    private val arrangement: List<Int?> = emptyList(),
    private val unpublishFails: Boolean = false,
    private val holdOpen: Boolean = false,
    private val reportRefusal: Throwable? = null,
) : FakeCommunityRepository() {
    val unpublished = mutableListOf<String>()

    /** Lets a held open answer through. */
    val openHeld = CompletableDeferred<Unit>()
    var offline = false
    var opens = 0

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        if (unpublishFails) return Result.failure(IllegalStateException("offline"))
        unpublished += publishedId
        return Result.success(Unit)
    }

    val shownRankings = mutableListOf<Pair<List<Int?>, List<String>>>()
    val shownRows = mutableListOf<List<List<Int>>>()

    override suspend fun showMyRanking(
        listId: String,
        arrangement: List<Int?>,
        rows: List<List<Int>>,
        tierLabels: List<String>,
    ): Result<ShownRanking> {
        shownRankings += arrangement to tierLabels
        shownRows += rows
        showRefusal?.let { return Result.failure(RankingRefused(it)) }
        val ranking = Ranking("me", "Ann", null, arrangement, tierLabels, updatedAtMillis = 1, mine = true, rows = rows)
        return Result.success(ShownRanking(ranking, created = shownIsNew))
    }
    val withdrawn = mutableListOf<String>()
    val reported = mutableListOf<String>()
    var followStateAsked = 0

    override suspend fun followState(authorUid: String): Result<FollowState> {
        followStateAsked++
        return Result.success(FollowState(following = false))
    }

    override suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit> {
        reportRefusal?.let { return Result.failure(it) }
        reported += publishedId
        return Result.success(Unit)
    }

    /** Held until the test lets the refusal through. */
    val refusal = CompletableDeferred<Unit>()

    override suspend fun open(id: String): Result<PublishedList> {
        opens++
        if (holdOpen) openHeld.await()
        if (offline) return Result.failure(IllegalStateException("offline"))
        return Result.success(
            published.copy(
                summary = summary(published.summary.copy(rankingCount = rankings.size)),
                arrangement = arrangement,
            ),
        )
    }

    override suspend fun rankings(listId: String, after: String?): Result<RankingsPage> = Result.success(RankingsPage(rankings, null))

    override suspend fun withdrawRanking(listId: String): Result<Unit> {
        withdrawn += listId
        if (!refusesWithdrawal) return Result.success(Unit)
        refusal.await()
        return Result.failure(IllegalStateException("offline"))
    }
}

/** Moonlight into S on the reader's own half: arranged, and ready to show. */
internal suspend fun arranged(viewModel: CommunityListViewModel): CommunityListUiState.Success {
    val loaded = viewModel.state.firstWithin { it is CommunityListUiState.Success } as CommunityListUiState.Success
    viewModel.show(Showing.Mine)
    viewModel.moveItem(itemId = 1, toTierId = loaded.mine.tiers.first { !it.isPool }.id, toPosition = 0)
    return viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.arranged == true } as CommunityListUiState.Success
}

internal suspend fun lookedUp(viewModel: CommunityListViewModel) =
    viewModel.state.firstWithin { (it as? CommunityListUiState.Success)?.ownBoardLookedUp == true } as CommunityListUiState.Success

internal suspend fun nextNotice(viewModel: CommunityListViewModel) = withTimeout(5_000) { viewModel.listNotices.first() }
