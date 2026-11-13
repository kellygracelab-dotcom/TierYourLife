package com.artiuillab.tieryourlife.feature.tier.data.remote

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PublicMediaInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val STORAGE = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/"

class PublicMediaInterceptorTest {
    @Test
    fun publicFolders_areReadThroughTheWebHandler() {
        for (kind in listOf("published", "avatars", "sections", "topics")) {
            for (slash in listOf("%2F", "%2f")) {
                val url = "$STORAGE$kind${slash}item-1${slash}w-ab12_34?alt=media&token=old-download-token"
                val sent = send(Request.Builder().url(url).build())

                assertEquals("https://tieryourlife.web.app/img/original/$kind/item-1/w-ab12_34", sent.url.toString())
            }
        }
    }

    @Test
    fun theAccountsSharedCatalogueCopies_areReadThroughTheWebHandlerToo() {
        val url = "${STORAGE}published%2Fcatalogue%2Fw-0123456789abcdef0123456789abcdef?alt=media"

        assertEquals(
            "https://tieryourlife.web.app/img/original/published/catalogue/w-0123456789abcdef0123456789abcdef",
            send(Request.Builder().url(url).build()).url.toString(),
        )
    }

    @Test
    fun publicReads_dropCredentialsButKeepTheOriginalRequestAndOtherHeaders() {
        val url = "${STORAGE}published%2Flist-1%2Fpicture-1?alt=media&token=old-download-token"
        val original = Request.Builder().url(url)
            .header("Authorization", "Bearer private-token")
            .header("X-Firebase-AppCheck", "private-app-check")
            .header("Cookie", "private-cookie")
            .header("Accept", "image/*")
            .build()

        val sent = send(original)

        assertEquals(url, original.url.toString())
        assertEquals("Bearer private-token", original.header("Authorization"))
        assertNull(sent.header("Authorization"))
        assertNull(sent.header("X-Firebase-AppCheck"))
        assertNull(sent.header("Cookie"))
        assertEquals("image/*", sent.header("Accept"))
        assertNull(sent.url.query)
    }

    @Test
    fun privateForeignAndMalformedAddresses_areNeverRewritten() {
        val unchanged = listOf(
            "${STORAGE}users%2Fowner%2Fpictures%2Fphoto?alt=media&token=private",
            "${STORAGE}proposals%2Fproposal-1%2Fphoto?alt=media&token=private",
            "${STORAGE}other%2Flist-1%2Fphoto?alt=media",
            "${STORAGE}published%2Flist-1%2Fphoto",
            "${STORAGE}published%2Flist-1%2Fphoto?alt=json",
            "${STORAGE}published/list-1/photo?alt=media",
            "${STORAGE}published%2F..%2Fphoto?alt=media",
            "${STORAGE}published%2Flist-1%2F..%2Fphoto?alt=media",
            "${STORAGE}published%252Flist-1%252Fphoto?alt=media",
            "${STORAGE}published%2F%2Fphoto?alt=media",
            "${STORAGE}published%2Flist-1%2F?alt=media",
            "https://firebasestorage.googleapis.com/v0/b/other-bucket/o/published%2Flist-1%2Fphoto?alt=media",
            "https://firebasestorage.googleapis.com.evil.test/v0/b/tieryourlife.firebasestorage.app/o/published%2Flist-1%2Fphoto?alt=media",
            "https://user:password@firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/published%2Flist-1%2Fphoto?alt=media",
            "https://:password@firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/published%2Flist-1%2Fphoto?alt=media",
            "https://firebasestorage.googleapis.com:8443/v0/b/tieryourlife.firebasestorage.app/o/published%2Flist-1%2Fphoto?alt=media",
            "http://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/published%2Flist-1%2Fphoto?alt=media",
            "https://images.igdb.com/igdb/image/upload/t_cover_big/coqa1.jpg",
            "https://tieryourlife.web.app/img/original/published/list-1/photo",
        )
        for (url in unchanged) {
            val original = Request.Builder().url(url).header("Authorization", "unchanged").build()
            val sent = send(original)

            assertEquals(url, original.url, sent.url)
            assertEquals(url, "unchanged", sent.header("Authorization"))
        }
    }

    @Test
    fun headKeepsItsMethod_andOtherMethodsStayOnStorage() {
        val url = "${STORAGE}published%2Flist-1%2Fphoto?alt=media"
        val head = send(Request.Builder().url(url).head().build())
        assertEquals("HEAD", head.method)
        assertEquals("https://tieryourlife.web.app/img/original/published/list-1/photo", head.url.toString())

        val original = Request.Builder().url(url).post("test body".toRequestBody()).build()
        val post = send(original)
        assertEquals(original.url, post.url)
        assertEquals("POST", post.method)
        assertEquals(original.body, post.body)
    }

    @Test
    fun objectAndOwnerLimits_matchThePublicHandler() {
        val id = "a".repeat(128)
        val picture = "p".repeat(1024 - "published//$id".length)
        val atLimit = "${STORAGE}published%2F$id%2F$picture?alt=media"
        assertEquals("https://tieryourlife.web.app/img/original/published/$id/$picture", send(Request.Builder().url(atLimit).build()).url.toString())

        for (url in listOf(
            "${STORAGE}published%2F${id}a%2Fphoto?alt=media",
            "${STORAGE}published%2F$id%2F${picture}p?alt=media",
        )) {
            val original = Request.Builder().url(url).build()
            assertEquals(original.url, send(original).url)
        }
    }

    private fun send(original: Request): Request {
        var sent: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(PublicMediaInterceptor())
            .addInterceptor { chain: Interceptor.Chain ->
                sent = chain.request()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody(null)).build()
            }
            .build()
        client.newCall(original).execute().close()
        return requireNotNull(sent)
    }
}
