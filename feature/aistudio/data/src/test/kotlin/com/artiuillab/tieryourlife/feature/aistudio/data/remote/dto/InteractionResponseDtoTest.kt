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

    @Test
    fun live_response_shape_with_a_contentless_thought_step_still_yields_the_image() {
        val json = """
            {
              "id": "v1_ChdJUzk4YXUyMko0UzZuc0VQdXR5ZS1RWRIX",
              "object": "interaction",
              "model": "gemini-3.1-flash-image",
              "status": "completed",
              "created": "2026-08-12T08:30:25Z",
              "updated": "2026-08-12T08:30:25Z",
              "service_tier": "standard",
              "usage": {
                "total_tokens": 1590,
                "raw_prompt_token": 452,
                "output_tokens_by_modality": [ { "modality": "image", "tokens": 1120 } ]
              },
              "steps": [
                { "type": "thought", "signature": "EqGuXQqdrl0BEU0yD" },
                {
                  "type": "model_output",
                  "content": [ { "type": "image", "data": "aGVsbG8=", "mime_type": "image/jpeg" } ]
                }
              ]
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<InteractionResponseDto>(json)
        val image = response.steps.flatMap { it.content }.first { it.type == "image" }

        assertEquals(2, response.steps.size)
        assertTrue(response.steps.first().content.isEmpty())
        assertEquals("aGVsbG8=", image.data)
        assertEquals("image/jpeg", image.mimeType)
    }

    @Test
    fun the_request_asks_for_jpeg_because_the_api_rejects_png() {
        val request = InteractionRequestDto(
            model = "gemini-3.1-flash-image",
            input = listOf(InteractionInputDto(text = "A neon-lit Tokyo street in the rain")),
            responseFormat = ImageResponseFormatDto(aspectRatio = "3:4", imageSize = "1K"),
        )

        val encoded = networkJson.encodeToString(InteractionRequestDto.serializer(), request)

        assertTrue(encoded.contains("\"mime_type\":\"image/jpeg\""))
        assertTrue(encoded.contains("\"aspect_ratio\":\"3:4\""))
        assertTrue(encoded.contains("\"image_size\":\"1K\""))
        assertTrue(encoded.contains("\"type\":\"text\""))
        assertTrue(encoded.contains("\"type\":\"image\""))
    }
}
