package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
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
    ],
    views = [
        ActiveTierListView::class,
        ActiveTierItemView::class,
    ],
    // Version 1, and there has never been another. The schema went through four versions
    // during development and carried hand-written migrations between them, but the app has
    // never shipped, so those migrated from states that only ever existed on the machines
    // that built them. Squashed before release — see the commit that did it if you want to
    // read them; the next migration this project writes will be a real one.
    version = 1,
    exportSchema = true,
)
abstract class TierDatabase : RoomDatabase() {

    abstract fun tierDao(): TierDao
}