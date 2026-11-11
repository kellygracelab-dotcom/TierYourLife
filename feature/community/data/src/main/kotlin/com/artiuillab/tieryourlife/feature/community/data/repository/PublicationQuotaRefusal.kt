package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.errorText
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Missing or malformed optional dates keep the legacy refusal; the response body remains readable. */
internal fun Throwable.publicationQuotaRefusal(): PublishError {
    val retryAt = runCatching {
        networkJson.parseToJsonElement(errorText()).jsonObject["retryAt"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 }
    }.getOrNull()
    return retryAt?.let { PublishError.WeeklyLimit(it) } ?: PublishError.TooManyLists
}
