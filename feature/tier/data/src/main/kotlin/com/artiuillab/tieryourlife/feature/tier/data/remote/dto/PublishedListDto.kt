package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PublishedListSummaryDto(
    val id: String,
    val title: String,
    val authorName: String,
    val itemCount: Int,
    val updatedAt: Long = 0,
)

@Serializable
data class PublishedFeedDto(
    val lists: List<PublishedListSummaryDto> = emptyList(),
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
)

@Serializable
data class PublishedListDto(
    val id: String,
    val title: String,
    val authorName: String,
    val itemCount: Int = 0,
    val updatedAt: Long = 0,
    val tiers: List<PublishedTierDto> = emptyList(),
    val items: List<PublishedItemDto> = emptyList(),
)

@Serializable
data class PublishListRequestDto(
    val title: String,
    val tiers: List<PublishedTierDto>,
    val items: List<PublishedItemDto>,
)

@Serializable
data class PublishedIdDto(
    val id: String,
)
