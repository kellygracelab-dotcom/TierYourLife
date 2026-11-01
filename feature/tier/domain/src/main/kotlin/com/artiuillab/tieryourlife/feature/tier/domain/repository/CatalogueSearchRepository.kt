package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope

interface CatalogueSearchRepository {

    /** [everywhere] asks every catalogue and the web whatever the scope: the empty scope's way out. */
    suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope = PictureScope.All,
        everywhere: Boolean = false,
    ): Result<CatalogueSearchPage>
}
