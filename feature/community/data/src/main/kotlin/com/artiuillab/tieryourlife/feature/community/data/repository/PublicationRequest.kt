package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures

private const val MAX_BOARD_UID = 100
private const val MAX_SOURCE_CARDS = 2000

/** A failed card-photo upload must not turn the confirmed derivative into a Ranking. */
internal suspend fun TierList.uploadedPublicationRequest(images: OwnPictures): PublishListRequestDto {
    val uploaded = images.sendNow(ownPictureIds(images))
    val changingCopy = takenFrom != null && publishedId == null
    val missingCard = tiers.flatMap { it.items }.any { card ->
        val name = images.pictureIdOf(card.imageUrl) ?: privatePictureIdOf(card.imageUrl)
        webPictureOf(card.imageUrl) == null && name != null && name !in uploaded
    }
    if (changingCopy && missingCard) throw PublishRefused(PublishError.PictureRefused)
    return toRequest(images, uploaded)
}

/**
 * This person's own pictures, each once: a poster has its own address, a
 * gallery photo is a file whose name is the only part worth sending.
 */
internal fun TierList.ownPictureIds(images: OwnPictures): List<String> =
    tiers.flatMap { tier -> tier.items.map { it.imageUrl } }
        .mapNotNull { url -> images.pictureIdOf(url) ?: privatePictureIdOf(url) }
        .plus(listOfNotNull(coverPictureIdOf(images)))
        .distinct()

internal fun TierList.toRequest(images: OwnPictures, uploaded: Set<String>) = PublishListRequestDto(
    title = title,
    category = category ?: ListCategory.Other.id,
    coverImageUrl = webPictureOf(coverImageUrl),
    coverPictureId = coverPictureIdOf(images)?.takeIf { it in uploaded },
    coverImageUrls = coverImageUrls.mapNotNull(::webPictureOf),
    topicId = topicId,
    // A name the server has not seen yet; once it has, the id above is what travels.
    newTopic = newTopicName?.takeIf { topicId == null },
    // Only when the author took it off: no topic on this board may be a moderator's on the list.
    noTopic = topicTakenOff && topicId == null && newTopicName == null,
    boardUid = uid.trim().takeIf { it.length in 1..MAX_BOARD_UID },
    takenFrom = takenFrom,
    sourceVersion = originVersion,
    anonymous = anonymous,
    tiers = tiers.filterNot { it.isPool }.map {
        PublishedTierDto(it.label, it.caption, it.colorLight, it.colorDark)
    },
    // A picture that would not upload is left unnamed rather than named and
    // missing. Tier indexes count the published tiers (the board's minus the
    // pool); a card in the pool says so with null.
    items = run {
        val ranked = tiers.filterNot { it.isPool }
        tiers.flatMap { tier ->
            val where = ranked.indexOfFirst { it.id == tier.id }.takeIf { it >= 0 }
            tier.items.map { item ->
                PublishedItemDto(
                    title = item.title,
                    imageUrl = webPictureOf(item.imageUrl),
                    pictureId = (images.pictureIdOf(item.imageUrl) ?: privatePictureIdOf(item.imageUrl))?.takeIf { it in uploaded },
                    tierIndex = where,
                    credit = item.credit,
                    sourceIndex = item.sourceIndex?.takeIf { it in 0 until MAX_SOURCE_CARDS },
                )
            }
        }
    },
)
