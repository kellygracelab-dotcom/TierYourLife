package com.artiuillab.tieryourlife.feature.tier.data.di

import android.content.Context
import androidx.room.Room
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_1_2
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_2_3
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_3_4
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_4_5
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_5_6
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        // No destructive fallback of any kind. Opening a newer database with an
        // older build used to drop every table without a word, which turns
        // "you installed the wrong build" into "your library is gone" -- and
        // these boards exist nowhere else. Refusing to open is recoverable by
        // installing the newer build again; a silent wipe is not recoverable at
        // all. Play will not serve an older version over a newer one, so the
        // case this protects is a phone with a developer build on it.
        .build()

    @Provides
    @Singleton
    fun provideTierDao(database: TierDatabase): TierDao = database.tierDao()

    @Provides
    @Singleton
    fun provideBoardSyncDao(database: TierDatabase): BoardSyncDao = database.boardSyncDao()

    private const val DATABASE_NAME = "tier_your_life.db"
}
