package com.artiuillab.tieryourlife.feature.tier.data.remote

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.LargerCollage
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.largerCollageUrl
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

private const val COLLAGE = "https://tieryourlife.com/img/collage/jl4N-x_1.webp?v=1795637564577"
private const val LARGER = "https://tieryourlife.com/img/collage/jl4N-x_1.webp?v=1795637564577&s=2"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LargerCollageTest {

    private val asked = mutableListOf<Any>()
    private var failing = false

    @Test
    fun aCollageWiderThanItsOwnSize_comesAtTwice() = runBlocking {
        load(COLLAGE, 1000, 625)
        load(COLLAGE, 400, 320)

        assertEquals(listOf<Any>(LARGER, LARGER), asked)
    }

    @Test
    fun aCollageNoBiggerThan480By300_staysAsItIs() = runBlocking {
        load(COLLAGE, 480, 300)
        load(COLLAGE, 240, 150)

        assertEquals(listOf<Any>(COLLAGE, COLLAGE), asked)
    }

    @Test
    fun aLargerCollageThatDoesNotCome_givesWayToTheUsualOne() = runBlocking {
        failing = true

        val result = load(COLLAGE, 1000, 625)

        assertTrue(result is SuccessResult)
        assertEquals(listOf<Any>(LARGER, COLLAGE), asked)
    }

    @Test
    fun onlyOurCollagesAreAskedLarger() {
        assertEquals(
            "https://tieryourlife.web.app/img/collage/a.webp?s=2",
            largerCollageUrl("https://tieryourlife.web.app/img/collage/a.webp".toHttpUrl()),
        )
        assertNull(largerCollageUrl("https://example.com/img/collage/a.webp".toHttpUrl()))
        assertNull(largerCollageUrl("http://tieryourlife.com/img/collage/a.webp".toHttpUrl()))
        assertNull(largerCollageUrl("https://tieryourlife.com/img/400/published/a/b".toHttpUrl()))
        assertNull(largerCollageUrl("https://tieryourlife.com/img/collage/a.png".toHttpUrl()))
        assertNull(largerCollageUrl("https://tieryourlife.com/img/collage/a.webp?s=1".toHttpUrl()))
        assertNull(largerCollageUrl("https://u:p@tieryourlife.com/img/collage/a.webp".toHttpUrl()))
    }

    private suspend fun load(url: String, width: Int, height: Int): Any {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val network = Interceptor { chain ->
            val data = chain.request.data
            asked += data
            if (failing && data.toString().contains("s=2")) {
                ErrorResult(null, chain.request, IOException("404"))
            } else {
                SuccessResult(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImage(), chain.request, DataSource.NETWORK)
            }
        }
        val loader = ImageLoader.Builder(context).components {
            add(LargerCollage())
            add(network)
        }.build()
        return loader.execute(
            ImageRequest.Builder(context).data(url).size(width, height).memoryCachePolicy(CachePolicy.DISABLED).build(),
        )
    }
}
