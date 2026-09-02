package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lives here rather than beside the rest of the account because the endpoint
 * does: ending an account is the server removing everything it kept, and the
 * thing that talks to that server is this module.
 */
@Singleton
class RetrofitAccountErasure @Inject constructor(
    private val api: BoardsApi,
) : AccountErasure {

    override suspend fun erase(): Result<Unit> = try {
        Result.success(api.eraseAccount())
    } catch (e: IOException) {
        Timber.w(e, "Erasing the account failed")
        Result.failure(e)
    } catch (e: HttpException) {
        Timber.w(e, "Erasing the account was refused")
        Result.failure(e)
    }
}
