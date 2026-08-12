package com.artiuillab.tieryourlife.feature.aistudio.data.generation

interface ImageBytesStore {
    fun save(bytes: ByteArray): String
    fun delete(uri: String)
    fun clear()
}
