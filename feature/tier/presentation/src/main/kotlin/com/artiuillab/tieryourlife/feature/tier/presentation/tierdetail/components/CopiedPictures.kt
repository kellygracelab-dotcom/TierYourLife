package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.textOf
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A copied list keeps its original's picture addresses, and the server retires
 * an address some time after the original stops showing it. A card whose
 * picture fails takes the one its original shows now. The original is read
 * once a visit, and only cards whose picture failed change: one chosen by hand
 * loads, so it is never touched.
 */
internal class CopiedPictures(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    private val scope: CoroutineScope,
    private val current: () -> TierList?,
    private val reload: suspend () -> Unit,
) {
    private val waiting = linkedMapOf<Long, TierItem>()
    private val asked = mutableSetOf<Pair<Long, String>>()
    private var original: PublishedList? = null
    private var mending: Job? = null

    fun lost(card: TierItem) {
        val sourceId = current()?.takenFrom ?: return
        val address = card.imageUrl?.takeIf { it.startsWith("http") } ?: return
        if (!asked.add(card.id to address)) return
        waiting[card.id] = card
        if (mending?.isActive == true) return
        mending = scope.launch {
            // Every card on screen fails within a frame or two of the others: one read serves them all.
            delay(GATHER_MILLIS)
            while (waiting.isNotEmpty()) {
                val cards = waiting.values.toList()
                waiting.clear()
                val source = originalOf(sourceId) ?: return@launch
                val mended = cards.mapNotNull { card -> pictureNow(card, source)?.let { card to it } }
                mended.forEach { (card, picture) -> repository.replaceLostPicture(card.id, requireNotNull(card.imageUrl), picture) }
                if (mended.isNotEmpty()) reload()
            }
        }
    }

    private suspend fun originalOf(sourceId: String): PublishedList? =
        original?.takeIf { it.summary.id == sourceId }
            ?: runSuspendCatching { community.open(sourceId).getOrThrow() }.getOrNull()
                ?.takeIf { it.summary.id == sourceId }
                ?.also { original = it }

    private companion object {
        const val GATHER_MILLIS = 400L
    }
}

/**
 * The original's card for [card] and the picture it shows now, or null where
 * nothing better is known. Its old place is trusted only while the name still
 * matches, in any language the original is read in; otherwise the one card of
 * that name.
 */
internal fun pictureNow(card: TierItem, original: PublishedList): TierItem? {
    val names = original.translations.byLocale.values
    fun TierItem.named(title: String) = this.title == title || names.any { it.textOf(this.title) == title }
    val byPlace = card.sourceIndex?.let(original.items::getOrNull)?.takeIf { it.named(card.title) }
    // A card that is only its picture has no name to find it by.
    val match = byPlace ?: card.title.takeIf { it.isNotBlank() }?.let { title -> original.items.singleOrNull { it.named(title) } }
    return match?.takeIf { it.imageUrl != null && it.imageUrl != card.imageUrl }
}
