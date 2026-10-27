package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SectionDto(
    val id: String,
    val listCount: Int = 0,
    val followerCount: Int = 0,
    val newThisWeek: Int = 0,
    val following: Boolean = false,
    val collage: List<String> = emptyList(),
)

@Serializable
data class SectionsDto(val sections: List<SectionDto> = emptyList())

@Serializable
data class FollowedSectionDto(val id: String, val followerCount: Int = 0)

@Serializable
data class SectionFollowDto(val following: Boolean = false)
