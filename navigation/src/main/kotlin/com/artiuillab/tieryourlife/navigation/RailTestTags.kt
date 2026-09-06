package com.artiuillab.tieryourlife.navigation

internal object RailTestTags {
    const val RAIL = "home_rail"
    const val NEW_LIST = "home_rail_new_list"
    fun item(destination: RailDestination): String = "home_rail_${destination.name.lowercase()}"
}
