package com.artiuillab.tieryourlife.core.usage

import com.artiuillab.tieryourlife.core.network.ProxyHeaders
import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsageModule {

    @Binds
    internal abstract fun bindUsage(reporter: UsageReporter): Usage

    @Binds
    abstract fun bindProxyHeaders(headers: UsageHeaders): ProxyHeaders

    companion object {
        @Provides
        @Singleton
        internal fun provideUsageApi(@ProxyRetrofit retrofit: Retrofit): UsageApi = retrofit.create(UsageApi::class.java)
    }
}
