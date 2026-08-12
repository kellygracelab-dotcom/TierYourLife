package com.artiuillab.tieryourlife.feature.aistudio.data.remote

import kotlinx.serialization.json.Json

val networkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
