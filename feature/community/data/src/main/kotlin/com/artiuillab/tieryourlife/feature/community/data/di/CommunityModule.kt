package com.artiuillab.tieryourlife.feature.community.data.di

import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.repository.RetrofitCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunityModule {

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        implementation: RetrofitCommunityRepository,
    ): CommunityRepository

    companion object {
        @Provides
        @Singleton
        fun provideCommunityApi(@ProxyRetrofit retrofit: Retrofit): CommunityApi =
            retrofit.create(CommunityApi::class.java)
    }
}
