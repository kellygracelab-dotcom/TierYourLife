package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toStoredCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import java.net.URI

/** A board with its catalogue pictures shown from the account's shared copies, and the cards that changed. */
internal data class WithCopies(val board: TierListEntity, val items: List<TierItemEntity>)

/**
 * What a write's `copies` (catalogue address to ours) changes on a board, as
 * the account already stores it: cards, cover and collage show our address,
 * and each card keeps the catalogue one in `attribution.originalUrl`, under
 * the catalogue's name when it had no credit. Null when nothing on it moves.
 */
internal fun withCopies(board: TierListEntity, items: List<TierItemEntity>, copies: Map<String, String>): WithCopies? {
    if (copies.isEmpty()) return null
    val cards = items.mapNotNull { item ->
        val original = item.imageUrl ?: return@mapNotNull null
        val copy = copies[original] ?: return@mapNotNull null
        val attribution = item.attribution?.copy(originalUrl = original)
            ?: catalogueSourceOf(original)?.let { PictureAttribution(source = it, originalUrl = original) }
        item.copy(imageUrl = copy, attribution = attribution)
    }
    val collage = board.coverImageUrls.toCoverImageUrls()
    val shown = board.copy(
        coverImageUrl = board.coverImageUrl?.let { copies[it] ?: it },
        coverImageUrls = collage.map { copies[it] ?: it }.toStoredCoverImageUrls(),
    )
    if (cards.isEmpty() && shown == board) return null
    return WithCopies(shown, cards)
}

/** The account's `catalogueSourceOf`: a catalogue it copies from, by the picture's host. */
internal fun catalogueSourceOf(url: String): String? {
    val address = runCatching { URI(url) }.getOrNull() ?: return null
    if (address.scheme != "https" || address.userInfo != null || address.port != -1) return null
    val host = address.host ?: return null
    // A local Wikipedia's own uploads carry no free licence.
    if (host in WIKIMEDIA_FILE_HOSTS && !address.path.orEmpty().startsWith("/wikipedia/commons/")) return null
    return HOST_SOURCES[host]
}

private val HOST_SOURCES = mapOf(
    "image.tmdb.org" to "tmdb",
    "s4.anilist.co" to "anilist",
    "images.igdb.com" to "igdb",
    "shared.akamai.steamstatic.com" to "steam",
    "cdn-images.dzcdn.net" to "deezer",
    "e-cdns-images.dzcdn.net" to "deezer",
    "covers.openlibrary.org" to "openlibrary",
    "inaturalist-open-data.s3.amazonaws.com" to "inaturalist",
    "static.inaturalist.org" to "inaturalist",
    "commons.wikimedia.org" to "wikimedia",
    "upload.wikimedia.org" to "wikimedia",
    "thumb.wikimedia.org" to "wikimedia",
)

private val WIKIMEDIA_FILE_HOSTS = setOf("upload.wikimedia.org", "thumb.wikimedia.org")
