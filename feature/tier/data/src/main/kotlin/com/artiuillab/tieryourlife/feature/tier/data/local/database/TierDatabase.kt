package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

@Database(
    entities = [
        TierListEntity::class,
        TierEntity::class,
        TierItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TierDatabase : RoomDatabase() {

    abstract fun tierDao(): TierDao
}