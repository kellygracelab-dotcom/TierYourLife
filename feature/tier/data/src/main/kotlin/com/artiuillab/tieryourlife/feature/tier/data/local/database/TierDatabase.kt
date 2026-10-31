package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.PictureSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.view.ActiveTierItemView
import com.artiuillab.tieryourlife.feature.tier.data.local.view.ActiveTierListView

@Database(
    entities = [
        TierListEntity::class,
        TierEntity::class,
        TierItemEntity::class,
        BoardSyncEntity::class,
        PictureSyncEntity::class,
    ],
    views = [
        ActiveTierListView::class,
        ActiveTierItemView::class,
    ],
    version = 15,
    exportSchema = true,
    // Two new columns with defaults and nothing converted: Room writes the
    // ALTER TABLEs from the exported schemas, and every board stays as it was.
    autoMigrations = [AutoMigration(from = 13, to = 14)],
)
abstract class TierDatabase : RoomDatabase() {

    abstract fun tierDao(): TierDao

    abstract fun boardSyncDao(): BoardSyncDao
}
