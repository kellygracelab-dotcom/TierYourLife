package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * What the list holds, as its section and topic say: the server puts that kind of picture first, so
 * "Sushi" in a food list is the dish and "Arsenal" in a sport list is the club. Both may be unknown.
 */
data class ListKind(
    /** The list's section id ([TierList.category]): `film_tv`, `food`, or an approved one of somebody's own. */
    val section: String? = null,
    val topic: String? = null,
)
