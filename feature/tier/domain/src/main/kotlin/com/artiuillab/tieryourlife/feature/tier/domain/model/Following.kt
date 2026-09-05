package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * [FeedSource] is whose lists, [FeedSort] in what order; the category is the
 * third question and keeps its own row. Separate, so switching one never
 * quietly resets another.
 */
enum class FeedSource { Following, Everyone }

enum class FeedSort(val id: String) {
    /** Newest first. */
    Recent("recent"),

    /** By how many people have taken the list to rank for themselves. */
    Popular("popular"),
}

/**
 * Everyone opens on popular: a stranger's feed with no signal is a wall of
 * names. Following opens on newest: popularity there would bury a good list
 * by an author with twelve followers for ever.
 */
val FeedSource.opensOn: FeedSort
    get() = if (this == FeedSource.Following) FeedSort.Recent else FeedSort.Popular

/** Whether this person follows an author, and how many people do. */
data class FollowState(val following: Boolean, val followers: Int)

/** Somebody worth following, for a screen that has nobody yet. */
data class SuggestedAuthor(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    /** How many times their most taken list has been taken. */
    val takeCount: Int,
)
