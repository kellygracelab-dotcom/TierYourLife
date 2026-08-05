package com.artiuillab.tieryourlife.feature.tier.data.di

import android.content.Context
import androidx.room.Room
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTierDatabase(
        @ApplicationContext context: Context,
    ): TierDatabase = Room.databaseBuilder(
        context,
        TierDatabase::class.java,
        DATABASE_NAME,
    )
        // A database file newer than this build's schema cannot be migrated: there is no
        // downward path and Room will not invent one, so without this the app opens to an
        // error it can never recover from. That is reachable in normal use, not only on a
        // developer's machine — allowBackup is on, so Android can restore a database saved
        // by a newer install onto an older one, and the schema was squashed to version 1,
        // which renumbered every version that ever existed.
        //
        // Only downgrades are destructive. A genuine forward migration is still required to
        // exist, so this cannot quietly swallow a missing upgrade path and wipe real data.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    @Provides
    @Singleton
    fun provideTierDao(database: TierDatabase): TierDao = database.tierDao()

    private const val DATABASE_NAME = "tier_your_life.db"
}
