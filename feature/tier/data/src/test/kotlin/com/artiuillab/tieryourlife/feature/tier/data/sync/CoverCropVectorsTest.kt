package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.coverFrame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

/**
 * Backend's shared vectors (`cover-crop-vectors.json`, copied unchanged from
 * the proxy): every platform cuts a cover by the same formula, so the same
 * focus shows the same part of a picture on the phone, the site and the server.
 */
class CoverCropVectorsTest {

    private val vectors = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResource("cover-crop-vectors.json")).readText(),
    ).jsonObject

    private val tolerance = vectors.getValue("tolerance").jsonPrimitive.double

    private val cases = vectors.getValue("cases").jsonArray.map { it.jsonObject }

    private fun JsonObject.number(key: String) = getValue(key).jsonPrimitive.double

    @Test
    fun everyCase_cutsTheSameFrame() {
        cases.forEach { case ->
            val focus = case["focus"].takeUnless { it == null || it == JsonNull }?.jsonObject
            val frame = coverFrame(
                width = case.number("width"),
                height = case.number("height"),
                aspect = case.number("aspect"),
                x = focus?.number("x") ?: CoverFocus.CENTRE.toDouble(),
                y = focus?.number("y") ?: CoverFocus.CENTRE.toDouble(),
                zoom = focus?.number("zoom") ?: CoverFocus.MIN_ZOOM.toDouble(),
            )
            val crop = case.getValue("crop").jsonObject
            val name = case.getValue("name").jsonPrimitive.content
            assertEquals(name, crop.number("x"), frame.left, tolerance)
            assertEquals(name, crop.number("y"), frame.top, tolerance)
            assertEquals(name, crop.number("width"), frame.width, tolerance)
            assertEquals(name, crop.number("height"), frame.height, tolerance)
        }
    }

    /** What the screens draw with: a focus kept in floats lands within a ten-thousandth of the picture of the same frame. */
    @Test
    fun theFloatFocusTheScreensKeep_landsOnTheSameFrame() {
        cases.forEach { case ->
            val focus = case["focus"].takeUnless { it == null || it == JsonNull }?.jsonObject
            val width = case.number("width")
            val frame = coverFrame(
                width.toFloat(),
                case.number("height").toFloat(),
                case.number("aspect").toFloat(),
                focus?.let { CoverFocus(it.number("x").toFloat(), it.number("y").toFloat(), it.number("zoom").toFloat()) },
            )
            val crop = case.getValue("crop").jsonObject
            val name = case.getValue("name").jsonPrimitive.content
            val close = width * FLOAT_SHARE
            assert(abs(crop.number("x") - frame.left) < close) { "$name: x ${frame.left} for ${crop.number("x")}" }
            assert(abs(crop.number("y") - frame.top) < close) { "$name: y ${frame.top} for ${crop.number("y")}" }
            assert(abs(crop.number("width") - frame.width) < close) { "$name: width ${frame.width} for ${crop.number("width")}" }
            assert(abs(crop.number("height") - frame.height) < close) { "$name: height ${frame.height} for ${crop.number("height")}" }
        }
    }

    private companion object {
        const val FLOAT_SHARE = 1e-4
    }
}
