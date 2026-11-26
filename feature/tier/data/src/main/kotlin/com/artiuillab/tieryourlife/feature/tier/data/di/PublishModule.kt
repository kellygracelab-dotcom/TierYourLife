package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.community.domain.repository.PublishCarrier
import com.artiuillab.tieryourlife.feature.tier.data.publish.BackgroundPublishCarrier
import com.artiuillab.tieryourlife.feature.tier.data.publish.CarryBackup
import com.artiuillab.tieryourlife.feature.tier.data.publish.WorkManagerCarryBackup
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PublishModule {

    @Binds
    @Singleton
    abstract fun bindPublishCarrier(
        implementation: BackgroundPublishCarrier,
    ): PublishCarrier

    @Binds
    @Singleton
    abstract fun bindCarryBackup(
        implementation: WorkManagerCarryBackup,
    ): CarryBackup
}
