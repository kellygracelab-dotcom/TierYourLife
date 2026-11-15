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
 * the proxy): every platform cuts a cover or a card's picture by the same
 * formula and starts a picture without a focus at the same default, so the
 * same picture shows the same part on the phone, the site and the server.
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
            val default = CoverFocus.defaultFor(case.number("width"), case.number("height"))
            val frame = coverFrame(
                width = case.number("width"),
                height = case.number("height"),
                aspect = case.number("aspect"),
                x = focus?.number("x") ?: default.x.exactly(),
                y = focus?.number("y") ?: default.y.exactly(),
                zoom = focus?.number("zoom") ?: default.zoom.exactly(),
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

    @Test
    fun aPictureWithoutAFocus_startsWhereTheVectorsSay() {
        val defaults = vectors.getValue("defaults").jsonObject
        val ratio = defaults.number("tallRatio")

        assertEquals(defaults.getValue("tall").jsonObject.focus(), CoverFocus.defaultFor(1000.0, 1000.0 * ratio + 1))
        assertEquals(defaults.getValue("other").jsonObject.focus(), CoverFocus.defaultFor(1000.0, 1000.0 * ratio))
        assertEquals(defaults.getValue("other").jsonObject.focus(), CoverFocus.defaultFor(1600.0, 900.0))
    }

    private fun JsonObject.focus() = CoverFocus(number("x").toFloat(), number("y").toFloat(), number("zoom").toFloat())

    /** The float as written, 0.35 rather than 0.3499999940…, as the server reads it from the file. */
    private fun Float.exactly() = toString().toDouble()

    private companion object {
        const val FLOAT_SHARE = 1e-4
    }
}
