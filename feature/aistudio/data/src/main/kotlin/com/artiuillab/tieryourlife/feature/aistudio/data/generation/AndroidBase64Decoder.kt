package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBase64Decoder @Inject constructor() : Base64Decoder {

    override fun decode(value: String): ByteArray = Base64.decode(value, Base64.DEFAULT)
}
