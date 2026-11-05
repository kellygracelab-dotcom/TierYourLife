package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.WithdrawalDto
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.Withdrawal
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response

@Serializable
private data class SettledRefusalDto(
    val code: String? = null,
    val status: String? = null,
)

/** What a withdrawal came to; any refusal but 409 SETTLED is thrown, as the other calls throw theirs. */
fun Response<WithdrawalDto>.toWithdrawal(): Withdrawal {
    if (isSuccessful) {
        val answer = body()
        return if (answer?.outcome == DISMISSED) {
            Withdrawal.Dismissed(if (answer.status == SectionStatus.Merged.id) SectionStatus.Merged else SectionStatus.Declined)
        } else {
            Withdrawal.Withdrawn
        }
    }
    if (code() == HTTP_CONFLICT) {
        val refusal = runCatching { networkJson.decodeFromString<SettledRefusalDto>(errorBody()?.string().orEmpty()) }.getOrNull()
        if (refusal?.code == SETTLED) return Withdrawal.Settled(SectionStatus.fromId(refusal.status))
    }
    throw HttpException(this)
}

private const val DISMISSED = "dismissed"
private const val SETTLED = "SETTLED"
private const val HTTP_CONFLICT = 409
