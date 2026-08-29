package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * Eight, fixed. Free text would splinter the community feed into synonyms
 * nobody can browse. [id] is what travels; the display name is a resource.
 */
enum class ListCategory(val id: String) {
    Anime("anime"),
    FilmTv("film_tv"),
    Games("games"),
    Music("music"),
    Food("food"),
    Sport("sport"),
    People("people"),
    Other("other"),
    ;

    companion object {
        fun fromId(id: String?): ListCategory? = entries.firstOrNull { it.id == id }
    }
}
