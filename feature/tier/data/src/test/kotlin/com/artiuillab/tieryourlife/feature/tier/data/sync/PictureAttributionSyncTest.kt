package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureAttributionDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A card picked on one phone publishes with its page link from another: its attribution travels with the board. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PictureAttributionSyncTest : SyncEngineBench() {

    @Test
    fun aCardsAttribution_goesUpWithIt_andComesDownOnAnotherPhone() = runBlocking {
        val fox = PictureAttribution(
            source = "inaturalist",
            author = "Jane Doe",
            license = "CC BY 4.0",
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
            link = "https://www.inaturalist.org/photos/1",
            originalUrl = "https://inaturalist-open-data.s3.amazonaws.com/photos/1/medium.jpg",
        )
        givenLocalBoard("Animals", attribution = fox)
        val theirs = PictureAttributionDto(source = "somewhere-new", link = "https://example.org/page")
        boards.server["remote-1"] = theirBoard("remote-1", title = "Birds", revision = 2).let { board ->
            board.copy(items = board.items.map { it.copy(attribution = theirs) })
        }

        engine().sync()

        val sent = boards.kept.single().second.items.single().attribution
        assertEquals(PictureAttributionDto("inaturalist", "Jane Doe", "CC BY 4.0", fox.licenseUrl, fox.link, fox.originalUrl), sent)
        val arrived = dao.boardByUid("remote-1")!!
        assertEquals(
            listOf(PictureAttribution(source = "somewhere-new", link = "https://example.org/page")),
            dao.itemsOf(arrived.id).map { it.attribution },
        )
    }
}
