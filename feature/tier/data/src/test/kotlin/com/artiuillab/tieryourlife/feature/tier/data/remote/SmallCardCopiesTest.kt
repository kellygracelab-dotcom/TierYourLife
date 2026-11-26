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
import coil3.size.Size
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.SmallCardCopies
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.copyWidthFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

private const val STORAGE = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/"
private const val PUBLISHED = "${STORAGE}published%2Flist-1%2Fw-ab12?alt=media&token=t"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SmallCardCopiesTest {

    private val asked = mutableListOf<Any>()
    private var failing: (String) -> Boolean = { false }

    @Test
    fun aTileTakesTheSmallestCopyThatCoversIt_aBiggerPictureTheOriginal() {
        assertEquals(200, copyWidthFor(Size(137, 200)))
        assertEquals(400, copyWidthFor(Size(156, 228)))
        assertEquals(400, copyWidthFor(Size(400, 300)))
        assertNull(copyWidthFor(Size(485, 646)))
        assertNull(copyWidthFor(Size.ORIGINAL))
    }

    @Test
    fun aPublishedCardPicture_comesAsItsCopy() = runBlocking {
        val result = load(PUBLISHED, 137, 200)

        assertTrue(result is SuccessResult)
        assertEquals(listOf<Any>("https://tieryourlife.com/img/200/published/list-1/w-ab12"), asked)
    }

    // Lists now carry the web's address for the original, not the bucket's.
    @Test
    fun theWebsOriginalAddress_comesAsItsCopy_too() = runBlocking {
        load("https://tieryourlife.com/img/original/published/list-1/w-ab12", 137, 200)
        load("https://tieryourlife.com/img/original/avatars/user-1/face", 137, 200)
        load("https://tieryourlife.com/img/original/published/..%2F..%2Fx/w-ab12", 137, 200)
        load("https://tieryourlife.com/img/original/published/list-1/w-ab12?x=1", 137, 200)

        assertEquals(
            listOf<Any>(
                "https://tieryourlife.com/img/200/published/list-1/w-ab12",
                "https://tieryourlife.com/img/original/avatars/user-1/face",
                "https://tieryourlife.com/img/original/published/..%2F..%2Fx/w-ab12",
                "https://tieryourlife.com/img/original/published/list-1/w-ab12?x=1",
            ),
            asked,
        )
    }

    @Test
    fun aCopyThatDoesNotCome_givesWayToTheOriginal() = runBlocking {
        failing = { it.contains("/img/") }

        val result = load(PUBLISHED, 300, 300)

        assertTrue(result is SuccessResult)
        assertEquals(listOf<Any>("https://tieryourlife.com/img/400/published/list-1/w-ab12", PUBLISHED), asked)
    }

    @Test
    fun othersStayAsTheyWere_avatarsSectionsOwnPicturesAndBigOnes() = runBlocking {
        val unchanged = listOf(
            "${STORAGE}avatars%2Fuser-1%2Fface?alt=media",
            "${STORAGE}sections%2Fsection-1%2Fart?alt=media",
            "${STORAGE}topics%2Ftopic-1%2Fart?alt=media",
            "${STORAGE}users%2Fowner%2Fpictures%2Fphoto?alt=media&token=private",
            "https://images.igdb.com/igdb/image/upload/t_cover_big/coqa1.jpg",
            "file:///data/pictures/own.jpg",
        )
        for (url in unchanged) load(url, 137, 200)
        load(PUBLISHED, 485, 646)

        assertEquals(unchanged + PUBLISHED, asked)
    }

    private suspend fun load(url: String, width: Int, height: Int): Any {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val network = Interceptor { chain ->
            val data = chain.request.data
            asked += data
            if (failing(data.toString())) {
                ErrorResult(null, chain.request, IOException("404"))
            } else {
                SuccessResult(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImage(), chain.request, DataSource.NETWORK)
            }
        }
        val loader = ImageLoader.Builder(context).components {
            add(SmallCardCopies())
            add(network)
        }.build()
        return loader.execute(
            ImageRequest.Builder(context).data(url).size(width, height)
                .memoryCachePolicy(CachePolicy.DISABLED).build(),
        )
    }
}
