package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.repository.CatalogueSearchRepositoryImpl
import com.artiuillab.tieryourlife.feature.tier.data.repository.RetrofitCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomOwnLists
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTierRepository(
        implementation: RoomTierRepository,
    ): TierRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        implementation: RetrofitCommunityRepository,
    ): CommunityRepository

    @Binds
    @Singleton
    abstract fun bindOwnLists(
        implementation: RoomOwnLists,
    ): OwnLists

    @Binds
    @Singleton
    abstract fun bindCatalogueSearchRepository(
        implementation: CatalogueSearchRepositoryImpl,
    ): CatalogueSearchRepository
}
