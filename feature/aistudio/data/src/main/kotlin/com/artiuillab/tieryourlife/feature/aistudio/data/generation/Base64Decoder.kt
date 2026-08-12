package com.artiuillab.tieryourlife.feature.aistudio.data.generation

interface Base64Decoder {
    fun decode(value: String): ByteArray
}
