package com.artiuillab.tieryourlife.feature.community.data.di

import android.content.Context
import androidx.room.Room
import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.feature.community.data.cache.FeedCacheDao
import com.artiuillab.tieryourlife.feature.community.data.cache.FeedCacheDatabase
import com.artiuillab.tieryourlife.feature.community.data.cache.RoomCommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.data.cache.SystemCommunityNetwork
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.repository.RetrofitCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindCommunityFeedCache(implementation: RoomCommunityFeedCache): CommunityFeedCache

    @Binds
    @Singleton
    abstract fun bindCommunityNetwork(implementation: SystemCommunityNetwork): CommunityNetwork

    companion object {
        @Provides
        @Singleton
        fun provideCommunityApi(@ProxyRetrofit retrofit: Retrofit): CommunityApi =
            retrofit.create(CommunityApi::class.java)

        @Provides
        @Singleton
        fun provideFeedCacheDatabase(@ApplicationContext context: Context): FeedCacheDatabase =
            Room.databaseBuilder(context, FeedCacheDatabase::class.java, "community_feed_cache.db")
                // A cache, not anybody's work: a newer version simply starts empty.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun provideFeedCacheDao(database: FeedCacheDatabase): FeedCacheDao = database.feedCache()

        @Provides
        fun provideCommunityClock(): CommunityClock = CommunityClock { System.currentTimeMillis() }
    }
}
