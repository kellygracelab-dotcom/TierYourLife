package com.artiuillab.tieryourlife.feature.community.data.remote.dto

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
    val takeCount: Int = 0,
    val anonymous: Boolean = false,
    val topicId: String? = null,
    val topicName: String? = null,
    val rankingCount: Int = 0,
    val basedOn: BasedOnDto? = null,
    /** From `GET lists/mine`: the author's own waiting proposal this list moves to, and its name. */
    val proposedSection: String? = null,
    val proposedSectionName: String? = null,
    /** Sent only to the list's owner, and only as true. */
    val mine: Boolean = false,
)

@Serializable
data class PublishedFeedDto(
    val lists: List<PublishedListSummaryDto> = emptyList(),
    val nextCursor: String? = null,
    val followingNobody: Boolean = false,
)

@Serializable
data class FollowStateDto(
    val following: Boolean = false,
    val followers: Int = 0,
)

@Serializable
data class SuggestedAuthorDto(
    val uid: String,
    val name: String = "",
    val photoUrl: String? = null,
    val takeCount: Int = 0,
)

@Serializable
data class SuggestedAuthorsDto(
    val authors: List<SuggestedAuthorDto> = emptyList(),
)

@Serializable
data class FollowedAuthorDto(
    val uid: String,
    val name: String = "",
    val photoUrl: String? = null,
)

@Serializable
data class FollowedAuthorsDto(
    val authors: List<FollowedAuthorDto> = emptyList(),
    val topics: List<TopicDto> = emptyList(),
    val sections: List<FollowedSectionDto> = emptyList(),
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
     * The person's own photograph, named rather than addressed: it lives in
     * their private folder, so the server copies it into the feed's folder.
     * Null for a poster, which has an address already.
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
    val anonymous: Boolean = false,
    val topicId: String? = null,
    val topicName: String? = null,
    val takeCount: Int = 0,
    val rankingCount: Int = 0,
    val basedOn: BasedOnDto? = null,
    val mine: Boolean = false,
)

@Serializable
data class PublishListRequestDto(
    val title: String,
    val category: String,
    val coverImageUrl: String? = null,
    val coverPictureId: String? = null,
    /** The author's collage, web addresses only; a single cover above it wins when set. */
    val coverImageUrls: List<String> = emptyList(),
    val tiers: List<PublishedTierDto>,
    val items: List<PublishedItemDto>,
    /** One or the other: a topic the server knows, or a name it has not seen. */
    val topicId: String? = null,
    val newTopic: String? = null,
    /** The published list the cards were taken from; the server decides whether this is a ranking of it. */
    val takenFrom: String? = null,
    /**
     * Always sent, true or false, with no default: a republish without the
     * field keeps whatever the server has, which is how an older app cannot
     * turn an anonymous list back into a named one.
     */
    val anonymous: Boolean,
)

@Serializable
data class PublishedIdDto(
    val id: String,
    /** The topic the list was filed under, named by the server once a new one exists. */
    val topicId: String? = null,
    /** True when the server kept it as a ranking of [id] rather than as a list. */
    val ranking: Boolean = false,
)

@Serializable
data class ReportRequestDto(
    val reason: String,
    val note: String? = null,
)

@Serializable
data class BasedOnDto(
    val id: String,
    val title: String = "",
    /** Empty for an anonymous original, and on copies made before credits carried a uid. */
    val authorUid: String = "",
    /** Empty, or missing, for an anonymous original. */
    val authorName: String? = null,
)

@Serializable
data class RankingDto(
    val uid: String,
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val arrangement: List<Int?> = emptyList(),
    val tierLabels: List<String> = emptyList(),
    val updatedAtMs: Long = 0,
    val mine: Boolean = false,
)

@Serializable
data class RankingsPageDto(
    val rankings: List<RankingDto> = emptyList(),
    val nextCursor: String? = null,
)
