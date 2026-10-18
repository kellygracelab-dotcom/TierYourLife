package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.data.remote.AccountApi
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitAccountErasure @Inject constructor(
    private val api: AccountApi,
) : AccountErasure {

    override suspend fun erase(): Result<Unit> = try {
        Result.success(api.erase())
    } catch (e: IOException) {
        Timber.w(e, "Erasing the account failed")
        Result.failure(e)
    } catch (e: HttpException) {
        Timber.w(e, "Erasing the account was refused")
        Result.failure(e)
    }
}
