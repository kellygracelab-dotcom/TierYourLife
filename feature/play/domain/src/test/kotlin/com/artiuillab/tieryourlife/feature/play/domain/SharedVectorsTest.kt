package com.artiuillab.tieryourlife.feature.play.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The website plays by the same file. A change here that moves any number is
 * a change to both platforms: regenerate with PLAY_VECTORS_WRITE=1 and tell
 * the web side.
 */
class SharedVectorsTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun theEngineStillGivesWhatTheSharedVectorsSay() {
        assertEquals(
            "docs/play-vectors.json is out of date; regenerate it with PLAY_VECTORS_WRITE=1",
            Json.parseToJsonElement(vectorsFile.readText()),
            buildVectors(),
        )
    }

    @Test
    fun writeVectors() {
        assumeTrue(System.getenv("PLAY_VECTORS_WRITE") == "1")
        vectorsFile.writeText(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), buildVectors()) + "\n")
    }
}
