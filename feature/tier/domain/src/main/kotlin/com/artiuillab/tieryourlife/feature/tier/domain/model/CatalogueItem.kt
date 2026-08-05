package com.artiuillab.tieryourlife.feature.tier.domain.model

// A result from a remote catalogue, source-agnostic on purpose: the search screen fans out to
// more than one catalogue (TMDB, Wikidata, ...) and merges their results into one list, so no
// single source's id shape (e.g. TMDB's numeric id) can be the model's identity.
//
// id carries an explicit source prefix ("tmdb:157336", "wikidata:Q11788") rather than being a
// bare numeric id — this screen already crashed once on duplicate LazyColumn keys when every
// TMDB result carried id = 0, and merging two sources is exactly where numeric ids collide.
// Selection state keys off this id.
data class CatalogueItem(
    val id: String,
    val title: String,
    // The year for a TMDB result, the description for a Wikidata result.
    val subtitle: String?,
    val imageUrl: String?,
)
