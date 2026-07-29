package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

interface MovieSearchRepository {

    suspend fun searchMovies(query: String): Result<List<TierItem>>
}