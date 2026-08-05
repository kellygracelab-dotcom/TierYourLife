package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.preferences.SharedPreferencesAppPreferences
import com.artiuillab.tieryourlife.feature.tier.data.repository.CatalogueSearchRepositoryImpl
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
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
    abstract fun bindCatalogueSearchRepository(
        implementation: CatalogueSearchRepositoryImpl,
    ): CatalogueSearchRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferences(
        implementation: SharedPreferencesAppPreferences,
    ): AppPreferences
}
