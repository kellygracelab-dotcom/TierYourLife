package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * How long somebody is kept from publishing.
 *
 * [id] is what travels to the server. A ban stops publishing and nothing
 * else: their boards stay on their phone, they go on making and ranking, and
 * their account is untouched. What stops is putting things in front of other
 * people, which is the only thing that was misused.
 */
enum class BanLength(val id: String) {
    Week("week"),
    Month("month"),
    ThreeMonths("three_months"),
    SixMonths("six_months"),

    /**
     * The only one with no natural end, which is why it is asked about twice
     * and never sits beside the others.
     */
    Forever("forever"),
    ;

    val isForever: Boolean get() = this == Forever
}
