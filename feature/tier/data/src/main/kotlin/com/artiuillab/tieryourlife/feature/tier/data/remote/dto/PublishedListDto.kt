package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PublishedListSummaryDto(
    val id: String,
    val title: String,
    val authorUid: String = "",
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val category: String = "other",
    val itemCount: Int,
    val coverImageUrl: String? = null,
    val previewImages: List<String> = emptyList(),
    val tierColors: List<String> = emptyList(),
    val updatedAt: Long = 0,
)

@Serializable
data class PublishedFeedDto(
    val lists: List<PublishedListSummaryDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class PublishedTierDto(
    val label: String,
    val caption: String? = null,
    val colorLight: String,
    val colorDark: String,
)

@Serializable
data class PublishedItemDto(
    val title: String,
    val imageUrl: String? = null,
    /**
     * A photograph of this person's own, named rather than addressed: it lives
     * in their private folder, which nobody else may read, so the server
     * copies it into the feed's own folder and puts that address in the
     * snapshot. Null for a poster, which already has an address of its own.
     */
    val pictureId: String? = null,
    /**
     * Which tier the author put this card in, by position among the published
     * tiers, or null for one they left unranked. Absent on everything
     * published before this existed.
     */
    val tierIndex: Int? = null,
)

@Serializable
data class PublishedListDto(
    val id: String,
    val title: String,
    val authorUid: String = "",
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val category: String = "other",
    val itemCount: Int = 0,
    val coverImageUrl: String? = null,
    val previewImages: List<String> = emptyList(),
    val tierColors: List<String> = emptyList(),
    val updatedAt: Long = 0,
    val tiers: List<PublishedTierDto> = emptyList(),
    val items: List<PublishedItemDto> = emptyList(),
)

@Serializable
data class PublishListRequestDto(
    val title: String,
    val category: String,
    val coverImageUrl: String? = null,
    val coverPictureId: String? = null,
    val tiers: List<PublishedTierDto>,
    val items: List<PublishedItemDto>,
)

@Serializable
data class PublishedIdDto(
    val id: String,
)

@Serializable
data class ReportRequestDto(
    val reason: String,
    val note: String? = null,
)
