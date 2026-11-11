package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PublicationQuotaRefusalTest {
    private fun refused(retry: String) = HttpException(Response.error<Unit>(409, retry.toResponseBody("application/json".toMediaType())))

    @Test fun theRollingLimitKeepsItsRetryDateThroughRepeatedReaders() {
        val failure = refused("""{"code":"TOO_MANY_LISTS","retryAt":1794733500000}""")
        repeat(2) { assertEquals(PublishError.WeeklyLimit(1794733500000), failure.asPublishError()) }
    }

    @Test fun legacyAndMalformedDatesKeepTheExistingLimitBehavior() {
        for (field in listOf("", ",\"retryAt\":null", ",\"retryAt\":-1", ",\"retryAt\":\"later\"", ",\"retryAt\":{}")) {
            assertEquals(PublishError.TooManyLists, refused("""{"code":"TOO_MANY_LISTS"$field}""").asPublishError())
        }
    }

    @Test fun anUnrelatedConflictIsNotTurnedIntoAQuotaByAnOptionalDate() {
        assertEquals(PublishError.SourceChanged, refused("""{"code":"LIST_CHANGED","retryAt":1794733500000}""").asPublishError())
    }
}
