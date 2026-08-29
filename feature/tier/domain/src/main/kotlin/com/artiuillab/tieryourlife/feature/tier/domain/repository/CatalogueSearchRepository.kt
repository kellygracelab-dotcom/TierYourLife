package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage

interface CatalogueSearchRepository {

    suspend fun search(query: String, languageTag: String?, page: Int): Result<CatalogueSearchPage>
}
