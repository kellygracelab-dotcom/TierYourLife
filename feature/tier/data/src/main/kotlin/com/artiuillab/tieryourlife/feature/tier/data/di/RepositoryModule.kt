package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.repository.CatalogueSearchRepositoryImpl
import com.artiuillab.tieryourlife.feature.tier.data.repository.RetrofitCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomOwnLists
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.data.sync.BoardSyncEngine
import com.artiuillab.tieryourlife.feature.tier.data.sync.Connection
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureSync
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureVault
import com.artiuillab.tieryourlife.feature.tier.data.sync.Pictures
import com.artiuillab.tieryourlife.feature.tier.data.sync.RoomBoardBackup
import com.artiuillab.tieryourlife.feature.tier.data.sync.SystemConnection
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
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
    abstract fun bindBoardSync(
        implementation: BoardSyncEngine,
    ): BoardSync

    @Binds
    @Singleton
    abstract fun bindBoardBackup(
        implementation: RoomBoardBackup,
    ): BoardBackup

    @Binds
    @Singleton
    abstract fun bindConnection(
        implementation: SystemConnection,
    ): Connection

    @Binds
    @Singleton
    abstract fun bindPictures(
        implementation: PictureVault,
    ): Pictures

    @Binds
    @Singleton
    abstract fun bindPictureRestore(
        implementation: PictureSync,
    ): PictureRestore

    @Binds
    @Singleton
    abstract fun bindCatalogueSearchRepository(
        implementation: CatalogueSearchRepositoryImpl,
    ): CatalogueSearchRepository
}
