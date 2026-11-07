package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.TargetIsStaff
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Proxy #96: a moderator's takedown, dismiss, move or report of a list by staff. */
class TargetIsStaffTest {

    @Test
    fun aListByStaff_isItsOwnRefusal() {
        assertTrue(refusal(403, """{"error":"Only the owner judges staff","code":"TARGET_IS_MODERATOR"}""").named() is TargetIsStaff)
    }

    @Test
    fun anyOtherForbidden_comesThroughAsItWas() {
        val notYours = refusal(403, """{"code":"NOT_YOURS"}""")
        val elsewhere = refusal(409, """{"code":"TARGET_IS_MODERATOR"}""")

        assertSame(notYours, notYours.named())
        assertSame(elsewhere, elsewhere.named())
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
}
