package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionResponseDtoTest {

    @Test
    fun completed_response_with_image_and_text_blocks_extracts_base64_data() {
        val json = """
            {
              "id": "interaction-1",
              "status": "completed",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "image",
                      "data": "aGVsbG8=",
                      "mime_type": "image/png"
                    },
                    {
                      "type": "text",
                      "text": "Here is your card."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<InteractionResponseDto>(json)

        assertEquals("interaction-1", response.id)
        assertEquals("completed", response.status)
        val imageContent = response.steps.single().content.first { it.type == "image" }
        assertEquals("aGVsbG8=", imageContent.data)
        assertEquals("image/png", imageContent.mimeType)
    }

    @Test
    fun response_without_image_block_has_no_data_among_content() {
        val json = """
            {
              "id": "interaction-2",
              "status": "completed",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "I can't create that image."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<InteractionResponseDto>(json)

        val imageContent = response.steps
            .flatMap { it.content }
            .firstOrNull { it.type == "image" }
        assertNull(imageContent)
    }

    @Test
    fun image_block_in_second_step_is_still_reachable() {
        val json = """
            {
              "id": "interaction-3",
              "status": "completed",
              "steps": [
                {
                  "type": "reasoning",
                  "content": [
                    { "type": "text", "text": "Planning the composition." }
                  ]
                },
                {
                  "type": "model_output",
                  "content": [
                    { "type": "image", "data": "d29ybGQ=", "mime_type": "image/png" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<InteractionResponseDto>(json)

        assertEquals(2, response.steps.size)
        val imageContent = response.steps
            .flatMap { it.content }
            .first { it.type == "image" }
        assertEquals("d29ybGQ=", imageContent.data)
    }

    @Test
    fun error_response_parses_code_and_message() {
        val json = """
            {
              "error": {
                "code": "429",
                "message": "Resource exhausted"
              }
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<GeminiErrorResponseDto>(json)

        assertEquals("429", response.error.code)
        assertTrue(response.error.message!!.isNotBlank())
        assertEquals("Resource exhausted", response.error.message)
    }
}
