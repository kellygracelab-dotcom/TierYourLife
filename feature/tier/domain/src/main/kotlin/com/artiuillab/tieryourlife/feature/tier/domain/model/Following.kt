package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * The two questions a feed asks that are not "about what".
 *
 * [FeedSource] is whose lists, [FeedSort] is in what order, and the category
 * filter is the third and keeps its own row. They are separate because they
 * are answered separately: switching one must not quietly reset another.
 */
enum class FeedSource { Following, Everyone }

enum class FeedSort(val id: String) {
    /** Newest first. */
    Recent("recent"),

    /** By how many people have taken the list to rank for themselves. */
    Popular("popular"),
}

/**
 * Which order a source opens on.
 *
 * Everyone opens on the popular ordering because a stranger's feed with no
 * signal in it is a wall of names. Following opens on the newest, because
 * somebody who followed these people has already vouched for them, and
 * popularity there would bury a good list by an author with twelve followers
 * for ever.
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
