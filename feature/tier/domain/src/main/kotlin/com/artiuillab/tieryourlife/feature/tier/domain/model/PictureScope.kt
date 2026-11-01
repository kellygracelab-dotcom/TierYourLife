package com.artiuillab.tieryourlife.feature.tier.domain.model

/** Which catalogues a picture search asks (the proxy's src/pictureSources.ts); [All] adds the web when they find little. */
enum class PictureScope(val id: String) {
    All("all"),
    FilmTv("film_tv"),
    Anime("anime"),
    Games("games"),
    Music("music"),
    Books("books"),
    People("people"),

    /** Not a built-in section: an approved one with this id searches animals. */
    Animals("animals"),
    ;

    companion object {
        /** A board in Anime searches anime first; a section with no catalogue of its own searches everything. */
        fun forCategory(category: String?): PictureScope = entries.firstOrNull { it != All && it.id == category } ?: All
    }
}

/** Why a search found less than it could have: shown as one line, never as an error. */
enum class SearchNotice {
    /** This person's web searches for today are used up. */
    WebDailyLimit,

    /** The web search is out for everybody for now. */
    WebOff,
}
