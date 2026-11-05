package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.WithdrawalDto
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.Withdrawal
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Every answer of `DELETE lists/sections/proposals/{id}`, as the catalog is told it. */
class WithdrawalAnswerTest {

    private fun refused(code: Int, body: String) = Response.error<WithdrawalDto>(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun nothingOrWithdrawn_isTakenBack() {
        assertEquals(Withdrawal.Withdrawn, Response.success<WithdrawalDto>(204, null).toWithdrawal())
        assertEquals(Withdrawal.Withdrawn, Response.success(WithdrawalDto(outcome = "withdrawn", status = "declined")).toWithdrawal())
    }

    @Test
    fun dismissed_saysWhatAModeratorHadMadeOfIt() {
        assertEquals(Withdrawal.Dismissed(SectionStatus.Merged), Response.success(WithdrawalDto(outcome = "dismissed", status = "merged")).toWithdrawal())
        assertEquals(Withdrawal.Dismissed(SectionStatus.Declined), Response.success(WithdrawalDto(outcome = "dismissed", status = "declined")).toWithdrawal())
    }

    @Test
    fun settled_isAnAnswer_notAFailure() {
        val answer = refused(409, """{"error":"That section has been decided already","code":"SETTLED","status":"approved"}""").toWithdrawal()

        assertEquals(Withdrawal.Settled(SectionStatus.Approved), answer)
    }

    @Test
    fun anyOtherRefusal_isThrown() {
        assertThrows(HttpException::class.java) { refused(409, """{"code":"SOMETHING_ELSE"}""").toWithdrawal() }
        assertThrows(HttpException::class.java) { refused(500, "oops").toWithdrawal() }
    }
}
