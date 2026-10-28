package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import kotlinx.serialization.Serializable
import retrofit2.HttpException

@Serializable
private data class ProposalRefusalDto(
    val code: String? = null,
    val detail: String? = null,
    val id: String? = null,
    val why: String? = null,
    val until: Long? = null,
)

/**
 * A refused proposal, named by the code the proxy puts in its body: one
 * status answers several refusals. A failure that is not an answer, or an
 * answer nobody wrote copy for, is [ProposalError.Failed]: nothing was lost.
 */
fun Throwable.proposalError(): ProposalError {
    val response = (this as? HttpException)?.response() ?: return ProposalError.Failed
    val body = runCatching { networkJson.decodeFromString(ProposalRefusalDto.serializer(), response.errorBody()?.string().orEmpty()) }
        .getOrNull() ?: ProposalRefusalDto()
    return when (body.code) {
        "BAD_NAME" -> ProposalError.BadName
        "NAME_TAKEN" -> ProposalError.NameTaken(body.id ?: body.detail)
        "BAD_ICON" -> ProposalError.BadIcon
        "BAD_FILE_UNDER" -> ProposalError.BadFileUnder
        "BAD_PICTURE" -> ProposalError.BadPicture(PictureProblem.fromId(body.why ?: body.detail))
        "TOO_MANY_PENDING" -> ProposalError.TooManyPending
        "TOO_MANY_PROPOSALS" -> ProposalError.TooManyToday
        "BANNED" -> ProposalError.Banned(body.until)
        "NOT_SIGNED_IN" -> ProposalError.NotSignedIn
        else -> when (response.code()) {
            HTTP_TOO_MANY -> ProposalError.TooManyToday
            HTTP_FORBIDDEN -> ProposalError.NotSignedIn
            else -> ProposalError.Failed
        }
    }
}

private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY = 429
