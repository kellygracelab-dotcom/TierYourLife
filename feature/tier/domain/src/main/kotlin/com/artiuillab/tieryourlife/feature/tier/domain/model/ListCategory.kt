package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * The nine sections the app has always had: their ids, and the Material
 * Symbols name each is drawn with. Sections are data now and any approved id
 * may stand where one of these did; these nine stay as the art and the words
 * the app draws while it knows nothing else.
 */
enum class ListCategory(val id: String, val symbol: String) {
    Anime("anime", "animation"),
    FilmTv("film_tv", "movie"),
    Games("games", "sports_esports"),
    Music("music", "music_note"),
    Books("books", "menu_book"),
    Food("food", "restaurant"),
    Sport("sport", "sports_soccer"),
    People("people", "person"),
    Other("other", "category"),
    ;

    companion object {
        fun fromId(id: String?): ListCategory? = entries.firstOrNull { it.id == id }
    }
}
