package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.Resumption
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.duels
import com.artiuillab.tieryourlife.feature.play.domain.estimatedDuels
import com.artiuillab.tieryourlife.feature.play.domain.isDashed
import com.artiuillab.tieryourlife.feature.play.domain.isThin
import com.artiuillab.tieryourlife.feature.play.domain.nextPair
import com.artiuillab.tieryourlife.feature.play.domain.progress
import com.artiuillab.tieryourlife.feature.play.domain.refineDuels
import com.artiuillab.tieryourlife.feature.play.domain.result
import com.artiuillab.tieryourlife.feature.play.domain.resumeOn
import com.artiuillab.tieryourlife.feature.play.domain.standing
import com.artiuillab.tieryourlife.feature.play.domain.swissRounds
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** A game: where it is kept, the board its cards were read from, the game itself, and whether that board is your own. */
internal data class Session(val key: String, val board: TierList, val game: SavedGame, val own: Boolean) {
    private val cards = board.tiers.flatMap { it.items }.associateBy { it.id }

    fun card(index: Int): TierItem {
        val id = game.ids[index]
        return cards[id] ?: TierItem(id = id, title = "", imageUrl = null)
    }
}

/** "Choose cards" rows: the pool first, then each tier that has cards, as the design lists them. */
internal fun groupsOf(board: TierList): List<CardGroup> =
    (board.tiers.filter { it.isPool } + board.tiers.filterNot { it.isPool })
        .filter { it.items.isNotEmpty() }
        .map { tier ->
            CardGroup(
                tierId = tier.id,
                label = tier.label,
                caption = tier.caption,
                colorLight = tier.colorLight,
                colorDark = tier.colorDark,
                count = tier.items.size,
                isPool = tier.isPool,
            )
        }

/** Duels a game will take: the estimate while it runs, what it took once the main game is over. */
internal fun totalOf(game: SavedGame): Int {
    val progress = progress(game.spec, game.history)
    return if (progress.mainOver) progress.played else estimatedDuels(game.spec.kind, game.spec.count)
}

internal fun offerFor(saved: SavedGame, board: TierList): ResumeOffer {
    val resumption = saved.resumeOn(board.cardsIn(saved.scope).map { it.id })
    val game = (resumption as? Resumption.Ready)?.game ?: saved
    return ResumeOffer(
        kind = game.spec.kind,
        played = progress(game.spec, game.history).played,
        total = totalOf(game),
        startedAt = saved.startedAt,
        changed = resumption is Resumption.Changed,
        joined = (resumption as? Resumption.Ready)?.joined ?: 0,
    )
}

internal fun stageOf(session: Session, intoMine: Boolean): PlayStage {
    val spec = session.game.spec
    val history = session.game.history
    val pair = nextPair(spec, history) ?: return PlayStage.Finished(resultOf(session, intoMine))
    val progress = progress(spec, history)
    val refining = progress.mainOver
    return PlayStage.Dueling(
        DuelState(
            kind = spec.kind,
            first = session.card(pair.first),
            second = session.card(pair.second),
            number = if (refining) progress.refineAsked + 1 else progress.played + 1,
            total = if (refining) progress.refineTotal else estimatedDuels(spec.kind, spec.count),
            round = progress.round,
            rounds = if (spec.kind == GameKind.Swiss) swissRounds(spec.count) else 0,
            refining = refining,
            canUndo = history.isNotEmpty(),
        ),
    )
}

/**
 * Where the game leaves its cards. One tier being ordered keeps them all in
 * it, best first; otherwise the board's tiers take them as the engine cuts
 * them, the pool never.
 */
internal fun resultOf(session: Session, intoMine: Boolean): ResultState {
    val game = session.game
    val standing = standing(game.spec.count, game.history.duels())
    val ranked = session.board.tiers.filterNot { it.isPool }
    val kept = game.scope?.singleOrNull()?.takeIf { game.keepTier }?.let { id -> ranked.firstOrNull { it.id == id } }
    val placed: List<Pair<Tier, List<Int>>> = when {
        kept != null -> listOf(kept to standing.order)
        ranked.isEmpty() -> emptyList()
        else -> ranked.zip(result(game.spec, game.history, ranked.size))
    }
    return ResultState(
        kind = game.spec.kind,
        duels = game.history.duels().size,
        tiers = placed.map { (tier, cards) ->
            ResultTier(
                tierId = tier.id,
                label = tier.label,
                colorLight = tier.colorLight,
                colorDark = tier.colorDark,
                cards = cards.map { ResultCard(session.card(it), standing.isDashed(it)) },
                caption = tier.caption?.takeIf { it.isNotBlank() },
            )
        },
        dashed = (0 until game.spec.count).count { standing.isDashed(it) },
        thin = isThin(standing),
        refineDuels = if (game.spec.refine) 0 else refineDuels(game.spec.count, game.history),
        intoMine = intoMine,
    )
}
