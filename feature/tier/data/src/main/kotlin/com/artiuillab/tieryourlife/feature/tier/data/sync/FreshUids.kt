package com.artiuillab.tieryourlife.feature.tier.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/** A board's tiers and cards under other uids, kept apart: new ones for a copy, or the ones they took here. */
internal class FreshUids(val tiers: Map<String, String>, val items: Map<String, String>) {
    constructor(tierUids: Collection<String>, itemUids: Collection<String>) : this(
        tiers = tierUids.associateWith { UUID.randomUUID().toString() },
        items = itemUids.associateWith { UUID.randomUUID().toString() },
    )

    /**
     * The board's game under the copy's uids, since it names its cards and
     * tiers by uid. Null when it names one the copy does not have.
     */
    fun play(stored: String?): String? {
        val game = stored?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject ?: return null
        val renamed = game.mapValues { (key, value) ->
            when (key) {
                "ids" -> value.renamedBy(items) ?: return null
                "scope" -> if (value is JsonNull) value else value.renamedBy(tiers) ?: return null
                else -> value
            }
        }
        if ("ids" !in renamed) return null
        return JsonObject(renamed).toString()
    }
}

private fun JsonElement.renamedBy(uids: Map<String, String>): JsonArray? {
    val names = this as? JsonArray ?: return null
    return JsonArray(
        names.map { name ->
            val uid = (name as? JsonPrimitive)?.takeIf { it.isString }?.content
            JsonPrimitive(uids[uid] ?: return null)
        },
    )
}
