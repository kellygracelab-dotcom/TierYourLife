package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.data.remote.AccountApi
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RetrofitAccountErasureTest {

    private fun erasureThat(erase: () -> Unit) = RetrofitAccountErasure(
        object : AccountApi {
            override suspend fun erase() = erase()
        },
    )

    @Test
    fun `an erased account is a success`() {
        val result = runBlocking { erasureThat {}.erase() }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `no network is a failure the screen can show, not a crash`() {
        val result = runBlocking { erasureThat { throw IOException("offline") }.erase() }

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `a refusal from the proxy is a failure, not a crash`() {
        val refusal = HttpException(Response.error<Unit>(403, "".toResponseBody()))

        val result = runBlocking { erasureThat { throw refusal }.erase() }

        assertTrue(result.exceptionOrNull() is HttpException)
    }
}
