package com.artiuillab.tieryourlife.feature.aistudio.domain.library

interface GeneratedCardSaver {
    suspend fun save(tierListId: Long, title: String, imageUri: String): Long
}
