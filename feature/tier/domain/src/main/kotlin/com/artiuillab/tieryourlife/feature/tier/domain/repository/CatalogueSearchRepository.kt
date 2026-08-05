package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

interface CatalogueSearchRepository {

    // languageTag is a BCP-47 tag (see AppPreferences.languageTag()) or null to follow the
    // system locale; the implementation resolves null before querying either source.
    suspend fun search(query: String, languageTag: String?): Result<List<CatalogueItem>>
}
