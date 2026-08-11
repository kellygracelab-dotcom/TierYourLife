package com.artiuillab.tieryourlife.feature.tier.data.di

import android.content.Context
import androidx.room.Room
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.database.MIGRATION_1_2
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
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    @Provides
    @Singleton
    fun provideTierDao(database: TierDatabase): TierDao = database.tierDao()

    private const val DATABASE_NAME = "tier_your_life.db"
}
