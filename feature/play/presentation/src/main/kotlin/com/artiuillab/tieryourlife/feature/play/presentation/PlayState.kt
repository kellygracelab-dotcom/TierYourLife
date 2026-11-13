package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.runtime.Immutable
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.MIN_PLAY_CARDS
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

data class PlayState(
    val stage: PlayStage = PlayStage.Closed,
    /** Keys with a game put down, for the dot on the board's button. */
    val saved: Set<String> = emptySet(),
)

/** What Play has over the board. */
sealed interface PlayStage {
    data object Closed : PlayStage

    data class Picking(val picker: Picker) : PlayStage

    data class Dueling(val duel: DuelState) : PlayStage

    data class Finished(val result: ResultState) : PlayStage
}

enum class PickerPage { Games, Cards, Individual }

@Immutable
data class CardOption(val item: TierItem, val tierId: Long)

/** The games in the order the picker lists them: the default first. */
val PlayGames = listOf(GameKind.Swiss, GameKind.Hill, GameKind.Bracket)

/** One row of "Choose cards": the pool or a tier that has cards. */
@Immutable
data class CardGroup(
    val tierId: Long,
    val label: String,
    val caption: String?,
    val colorLight: String,
    val colorDark: String,
    val count: Int,
    val isPool: Boolean,
)

/** A game put down for this board, as the picker's first row says it. */
@Immutable
data class ResumeOffer(
    val kind: GameKind,
    val played: Int,
    val total: Int,
    val startedAt: Long,
    /** A hill or bracket whose cards changed: it can only start again. */
    val changed: Boolean = false,
    /** Whether enough of its cards are left to start it again. */
    val canStartAgain: Boolean = true,
    /** A tier it was played in is no longer on the board: the cards may all be there, the game's area is not. */
    val tierGone: Boolean = false,
    /** Cards that joined a Swiss game since it was put down. */
    val joined: Int = 0,
)

@Immutable
data class Picker(
    val title: String,
    val groups: List<CardGroup>,
    val chosen: Set<Long>,
    val kind: GameKind = GameKind.Swiss,
    val page: PickerPage = PickerPage.Games,
    /** Opened on the cards because the board is too big to play whole: there is no game page behind it yet. */
    val cardsFirst: Boolean = false,
    val resume: ResumeOffer? = null,
    /** The game put down was just discarded here: its row offers "Undo" in its place. */
    val discarded: Boolean = false,
    val intoMine: Boolean = false,
    val cards: List<CardOption> = emptyList(),
    /** Null means all current candidates; a list freezes a sample/manual cohort and its order. */
    val selectedIds: List<Long>? = null,
) {
    val candidates: List<CardOption> get() = cards.filter { it.tierId in chosen }.distinctBy { it.item.id }
    val selectedCards: List<CardOption> get() {
        val available = candidates
        val explicit = selectedIds ?: return available
        val byId = cards.associateBy { it.item.id }
        return explicit.distinct().mapNotNull { byId[it] }
    }
    val count: Int get() = if (cards.isEmpty()) groups.filter { it.tierId in chosen }.sumOf { it.count } else selectedCards.size
    val total: Int get() = if (cards.isEmpty()) groups.sumOf { it.count } else cards.map { it.item.id }.distinct().size
    val everything: Boolean get() = groups.isNotEmpty() && groups.all { it.tierId in chosen } && selectedIds == null

    /** Every ranked tier with cards and nothing else: the area the pool stays out of. An empty tier in a saved scope does not count. */
    val rankedIds: Set<Long> get() = groups.filterNot { it.isPool }.map { it.tierId }.toSet()
    val rankedCount: Int get() = groups.filterNot { it.isPool }.sumOf { it.count }
    val ranked: Boolean get() = rankedIds.isNotEmpty() && chosen.filter { id -> groups.any { it.tierId == id } }.toSet() == rankedIds

    /** Ranked differs from Everything only when the board has a pool. */
    val offersRanked: Boolean get() = rankedIds.isNotEmpty() && groups.any { it.isPool }

    /** The one tier chosen, when it is one tier and not the pool: a game over it only orders it. */
    val singleTier: CardGroup?
        get() = chosen.singleOrNull()?.let { id -> groups.firstOrNull { it.tierId == id } }?.takeUnless { it.isPool }

    val keepsTier: Boolean get() = singleTier != null

    /** The pool alone. */
    val poolOnly: Boolean get() = chosen.singleOrNull()?.let { id -> groups.firstOrNull { it.tierId == id } }?.isPool == true
    val canPlay: Boolean get() = count >= MIN_PLAY_CARDS
}

@Immutable
data class DuelState(
    val kind: GameKind,
    val first: TierItem,
    val second: TierItem,
    /** This duel's number and how many there will be; while refining, of the close calls alone. */
    val number: Int,
    val total: Int,
    /** Swiss only: the round this duel is in, and how many there are at most. */
    val round: Int = 0,
    val rounds: Int = 0,
    val refining: Boolean = false,
    val canUndo: Boolean = false,
) {
    val progress: Float get() = if (total <= 0) 0f else ((number - 1).toFloat() / total).coerceIn(0f, 1f)

    /** Only the first few duels say what Skip is for; after that it is known. */
    val explainsSkip: Boolean get() = !refining && number <= SKIP_EXPLAINED_DUELS
}

private const val SKIP_EXPLAINED_DUELS = 3

@Immutable
data class ResultState(
    val kind: GameKind,
    /** Duels answered: a skip is no evidence and is not counted. */
    val duels: Int,
    val tiers: List<ResultTier>,
    val dashed: Int,
    val thin: Boolean,
    /** Close calls Refine would add; 0 once it has been used or when there are none. */
    val refineDuels: Int,
    val intoMine: Boolean,
    val unplayedCount: Int = 0,
    val unplayedPolicy: UnplayedPolicy = UnplayedPolicy.Keep,
) {
    val canRefine: Boolean get() = refineDuels > 0

    /** Every duel skipped is no evidence at all: applying would only pile the cards into the first tier. */
    val canApply: Boolean get() = duels > 0
}

enum class UnplayedPolicy { Keep, Pool }

@Immutable
data class ResultTier(
    val tierId: Long,
    val label: String,
    val colorLight: String,
    val colorDark: String,
    val cards: List<ResultCard>,
    /** The band's name under its letter, as the board shows it. */
    val caption: String? = null,
)

@Immutable
data class ResultCard(val item: TierItem, val dashed: Boolean)

sealed interface PlayEvent {
    data object Closed : PlayEvent

    /** The board an applied result, or its undo, left: the screen beneath takes it. */
    data class Arranged(val board: TierList, val undone: Boolean = false) : PlayEvent

    data class Said(val notice: PlayNotice) : PlayEvent
}

/** What Play says on the board once it has stepped aside, each with the one thing that can be done about it. */
sealed interface PlayNotice {
    /** With Discard. */
    data class GameSaved(val played: Int, val total: Int) : PlayNotice

    /** With Undo. */
    data class BoardArranged(val duels: Int) : PlayNotice

    /** With Undo. */
    data object SavedGameDiscarded : PlayNotice

    /** With Undo. */
    data object ResultDiscarded : PlayNotice

    data object Failed : PlayNotice

    /** Neither the file nor the board took the game. */
    data object NotSaved : PlayNotice

    /** The list was published again while it was played, and the game went. */
    data object ListUpdated : PlayNotice
}
