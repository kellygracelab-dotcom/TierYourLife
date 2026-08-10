package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

interface CatalogueSearchRepository {

    suspend fun search(query: String, languageTag: String?): Result<List<CatalogueItem>>
}
