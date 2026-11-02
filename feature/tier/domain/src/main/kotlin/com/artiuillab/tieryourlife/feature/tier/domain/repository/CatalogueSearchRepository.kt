package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope

interface CatalogueSearchRepository {

    /**
     * [everywhere] asks every catalogue and the web whatever the scope: the empty scope's way out.
     * [quick] leaves out the slow catalogues and the web, and says in [CatalogueSearchPage.rest]
     * whether asking again without it can add anything.
     */
    suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope = PictureScope.All,
        everywhere: Boolean = false,
        quick: Boolean = false,
    ): Result<CatalogueSearchPage>
}
