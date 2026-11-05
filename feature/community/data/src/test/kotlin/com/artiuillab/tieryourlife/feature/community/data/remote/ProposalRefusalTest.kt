package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ProposalAllowanceDto
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Every refusal of `POST lists/sections/proposals`, as the screen is told it. */
class ProposalRefusalTest {

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun aNameTheServerWillNotTake_isABadName() {
        assertEquals(ProposalError.BadName, refusal(400, """{"error":"x","code":"BAD_NAME"}""").proposalError())
    }

    @Test
    fun aTakenName_namesTheSectionThatHasIt() {
        assertEquals(ProposalError.NameTaken("street-food"), refusal(400, """{"code":"NAME_TAKEN","detail":"street-food","id":"street-food"}""").proposalError())
    }

    @Test
    fun anIconOrASectionToFileUnderThatWillNotDo() {
        assertEquals(ProposalError.BadIcon, refusal(400, """{"code":"BAD_ICON"}""").proposalError())
        assertEquals(ProposalError.BadFileUnder, refusal(400, """{"code":"BAD_FILE_UNDER"}""").proposalError())
    }

    @Test
    fun aPictureRefused_saysWhy() {
        assertEquals(ProposalError.BadPicture(PictureProblem.TooSmall), refusal(400, """{"code":"BAD_PICTURE","detail":"too_small","why":"too_small"}""").proposalError())
        assertEquals(ProposalError.BadPicture(PictureProblem.Not16By10), refusal(400, """{"code":"BAD_PICTURE","why":"not_16_10"}""").proposalError())
        assertEquals(ProposalError.BadPicture(PictureProblem.Missing), refusal(400, """{"code":"BAD_PICTURE","detail":"missing"}""").proposalError())
    }

    @Test
    fun threeWaiting_andTenToday_areTwoDifferentLimits() {
        assertEquals(ProposalError.TooManyPending, refusal(400, """{"code":"TOO_MANY_PENDING"}""").proposalError())
        assertEquals(ProposalError.TooManyToday, refusal(429, """{"code":"TOO_MANY_PROPOSALS"}""").proposalError())
        assertEquals(ProposalError.TooManyToday, refusal(429, "").proposalError())
    }

    @Test
    fun aBan_saysUntilWhen_andAGuest_isAskedToSignIn() {
        assertEquals(ProposalError.Banned(1_790_000_000_000), refusal(403, """{"code":"BANNED","until":1790000000000}""").proposalError())
        assertEquals(ProposalError.Banned(null), refusal(403, """{"code":"BANNED"}""").proposalError())
        assertEquals(ProposalError.NotSignedIn, refusal(403, """{"code":"NOT_SIGNED_IN"}""").proposalError())
    }

    @Test
    fun anythingElse_isAFailureWorthTryingAgain() {
        assertEquals(ProposalError.Failed, refusal(500, "<html>").proposalError())
        assertEquals(ProposalError.Failed, IOException("offline").proposalError())
    }

    private fun allowance(json: String) = networkJson.decodeFromString(ProposalAllowanceDto.serializer(), json).refusalError()

    @Test
    fun theCheckAhead_namesTheSameTwoLimits_andRoomToProposeIsNoRefusal() {
        assertEquals(
            ProposalError.TooManyPending,
            allowance("""{"pending":3,"maxPending":3,"proposalsToday":3,"maxPerDay":10,"refusal":"TOO_MANY_PENDING"}"""),
        )
        assertEquals(
            ProposalError.TooManyToday,
            allowance("""{"pending":0,"maxPending":3,"proposalsToday":10,"maxPerDay":10,"refusal":"TOO_MANY_PROPOSALS"}"""),
        )
        assertNull(allowance("""{"pending":1,"maxPending":3,"proposalsToday":1,"maxPerDay":10,"refusal":null}"""))
    }

    @Test
    fun aRefusalTheAppHasNoWordsFor_isLeftToTheProposalItself() {
        assertNull(allowance("""{"refusal":"SOMETHING_NEW"}"""))
    }

    @Test
    fun aGuestAsking_isToldToSignIn() {
        assertEquals(ProposalError.NotSignedIn, refusal(403, """{"error":"x","code":"NOT_SIGNED_IN"}""").proposalError())
    }
}
