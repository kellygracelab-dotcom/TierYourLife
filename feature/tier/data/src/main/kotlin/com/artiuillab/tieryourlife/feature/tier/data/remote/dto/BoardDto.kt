package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A board as the account keeps it. Not [PublishListRequestDto] -- that is a
 * picture arranged for strangers, this is the board someone would lose with
 * their phone, trash and pool and the order of everything included.
 */
@Serializable
data class KeptBoardDto(
    val uid: String,
    val revision: Int = 0,
    val deleted: Boolean = false,
    val fingerprint: String? = null,
    /** The phone that last wrote it, shown on the copy kept after a conflict. */
    val deviceName: String? = null,
    val title: String = "",
    val displayMode: String = "WRAP",
    val category: String? = null,
    val coverImageUrl: String? = null,
    val authorName: String? = null,
    val publishedId: String? = null,
    val deletedAt: Long? = null,
    val tiers: List<KeptTierDto> = emptyList(),
    val items: List<KeptItemDto> = emptyList(),
)

@Serializable
data class KeptTierDto(
    val uid: String,
    val position: Int,
    val label: String,
    val caption: String? = null,
    val colorLight: String,
    val colorDark: String,
    val isPool: Boolean = false,
)

@Serializable
data class KeptItemDto(
    val uid: String,
    /**
     * A picture of this person's own, by the file's own name. The path around
     * it means nothing on a second phone, so only this travels.
     */
    val pictureId: String? = null,
    /** Which tier it sits in, by that tier's uid rather than a row number. */
    val tierUid: String,
    val position: Int,
    val title: String,
    val imageUrl: String? = null,
    val source: String = "MANUAL",
    val deletedAt: Long? = null,
)

/** What a board looks like in the index: enough to decide whether to fetch it. */
@Serializable
data class KeptBoardSummaryDto(
    val uid: String,
    val revision: Int,
    val deleted: Boolean = false,
    val fingerprint: String? = null,
    val deviceName: String? = null,
    val title: String = "",
    val itemCount: Int = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class KeptBoardIndexDto(
    @SerialName("boards")
    val boards: List<KeptBoardSummaryDto> = emptyList(),
    val next: String? = null,
)

/** What the account writes back once it has taken a board. */
@Serializable
data class KeptBoardRevisionDto(val uid: String, val revision: Int)

/**
 * The body of a refused write. The board that was already there comes back
 * with it, so the phone can keep both without a second request.
 */
@Serializable
data class BoardConflictDto(
    val error: String = "",
    val code: String = "",
    val board: KeptBoardDto? = null,
)

/**
 * A board on its way up. [basedOn] is the revision this phone was working
 * from; the account refuses the write if it has moved on since, which is what
 * turns a silent overwrite into a second board somebody can look at.
 */
@Serializable
data class KeepBoardRequestDto(
    val basedOn: Int? = null,
    val deviceName: String? = null,
    val fingerprint: String,
    val title: String,
    val displayMode: String,
    val category: String? = null,
    val coverImageUrl: String? = null,
    val authorName: String? = null,
    val publishedId: String? = null,
    val deletedAt: Long? = null,
    val tiers: List<KeptTierDto>,
    val items: List<KeptItemDto>,
)
