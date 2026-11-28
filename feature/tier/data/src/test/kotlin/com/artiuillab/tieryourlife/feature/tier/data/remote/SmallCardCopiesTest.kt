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
import coil3.size.Scale
import coil3.size.Size
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.Cover
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.SmallCardCopies
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.Tile
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.copyWidthFor
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.nextFor
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

    /** What each address comes as; 1×1 for any other. */
    private val sizes = mutableMapOf<String, Pair<Int, Int>>()

    @Test
    fun aTileTakesTheSmallestCopyThatCoversIt_aBiggerPictureTheOriginal() {
        assertEquals(200, copyWidthFor(Size(137, 200)))
        assertEquals(400, copyWidthFor(Size(156, 228)))
        assertEquals(400, copyWidthFor(Size(400, 300)))
        assertNull(copyWidthFor(Size(485, 646)))
        assertNull(copyWidthFor(Size.ORIGINAL))
    }

    @Test
    fun aCopyCoversItsTile_orNamesTheOneThatWould() {
        val tall = Tile(137, 200)
        // A poster, taller than wide: the first copy is enough.
        assertEquals(Cover.Enough, nextFor(200, 300, 200, tall, Scale.FILL))
        // 4:3, cropped into a tall tile: 200 wide is only 150 high, 267 wide would be 200.
        assertEquals(Cover.Copy(400), nextFor(200, 150, 200, tall, Scale.FILL))
        // A duel card: no copy is wide enough, so the original.
        assertEquals(Cover.Original, nextFor(400, 300, 400, Tile(252, 357), Scale.FILL))
        // A copy narrower than asked is the whole picture already.
        assertEquals(Cover.Enough, nextFor(150, 100, 200, tall, Scale.FILL))
        // Fitted, one side reaching the tile is enough.
        assertEquals(Cover.Enough, nextFor(200, 150, 200, tall, Scale.FIT))
        // A server rounding a height down by one pixel does not cost a bigger copy.
        assertEquals(Cover.Enough, nextFor(200, 199, 200, tall, Scale.FILL))
    }

    @Test
    fun aWidePictureInATallTile_isFollowedByTheCopyThatCoversIt() = runBlocking {
        sizes["https://tieryourlife.com/img/200/published/list-1/w-ab12"] = 200 to 150
        sizes["https://tieryourlife.com/img/400/published/list-1/w-ab12"] = 400 to 300

        load(PUBLISHED, 137, 200)

        assertEquals(
            listOf<Any>(
                "https://tieryourlife.com/img/200/published/list-1/w-ab12",
                "https://tieryourlife.com/img/400/published/list-1/w-ab12",
            ),
            asked,
        )
    }

    @Test
    fun aWidePictureWiderThanEveryCopyNeeds_comesAsTheOriginal() = runBlocking {
        sizes["https://tieryourlife.com/img/400/published/list-1/w-ab12"] = 400 to 300

        load(PUBLISHED, 252, 357)

        assertEquals(listOf<Any>("https://tieryourlife.com/img/400/published/list-1/w-ab12", PUBLISHED), asked)
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
                val (width, height) = sizes[data.toString()] ?: (1 to 1)
                SuccessResult(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImage(), chain.request, DataSource.NETWORK)
            }
        }
        val loader = ImageLoader.Builder(context).components {
            add(SmallCardCopies())
            add(network)
        }.build()
        return loader.execute(
            ImageRequest.Builder(context).data(url).size(width, height).scale(Scale.FILL)
                .memoryCachePolicy(CachePolicy.DISABLED).build(),
        )
    }
}
