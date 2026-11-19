package com.artiuillab.tieryourlife.feature.account.data.settings

import com.artiuillab.tieryourlife.core.settings.ColourChoice
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.data.remote.ColourDto
import com.artiuillab.tieryourlife.feature.account.data.remote.ColourWriteDto
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsApi
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsDto
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** CONTRACT.md, boards, Settings: the later change wins, a guest's colour stays on the device. */
class ColourSyncTest {

    private class FakeSettings(var colour: ColourDto? = null, var keepsLater: Boolean = false, var refuse: Int? = null) : SettingsApi {
        val writes = mutableListOf<ColourWriteDto>()
        var reads = 0

        override suspend fun settings(): SettingsDto {
            reads++
            refuse?.let { throw HttpException(Response.error<Any>(it, "{}".toResponseBody())) }
            return SettingsDto(colour)
        }

        override suspend fun keepColour(colour: ColourWriteDto): ColourDto {
            writes += colour
            val held = this.colour
            if (keepsLater && held != null) return held.copy(landed = false)
            return ColourDto(colour.name, colour.hue, colour.changedAt, updatedAt = colour.changedAt + 1).also { this.colour = it }
        }
    }

    private val ann = Account.SignedIn(email = null, photoUrl = null, uid = "ann")
    private val preferences = FakeAppPreferences()
    private val colour = ColourChoice(preferences)
    private val accounts = FakeAccountRepository(ann)

    private fun sync(api: SettingsApi) =
        ColourSync(api, colour, accounts, MutableSharedFlow(), CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))

    @Test
    fun theAccountsLaterColour_isTaken() = runBlocking {
        preferences.colour = ColourSetting("sky", 212, changedAt = 100)
        val choice = ColourChoice(preferences)
        val account = FakeSettings(ColourDto("custom", 300, changedAt = 200))

        ColourSync(account, choice, accounts, MutableSharedFlow(), CoroutineScope(Dispatchers.Unconfined)).meet()

        assertEquals(ColourSetting("custom", 300, 200), choice.current.value)
        assertEquals(emptyList<ColourWriteDto>(), account.writes)
    }

    @Test
    fun thisDevicesLaterColour_goesUp_andSoDoesOneTheAccountNeverHad() = runBlocking {
        preferences.colour = ColourSetting("matcha", 40, changedAt = 300)
        val choice = ColourChoice(preferences)
        for (held in listOf(ColourDto("sky", 212, changedAt = 200), null)) {
            val account = FakeSettings(held)
            ColourSync(account, choice, accounts, MutableSharedFlow(), CoroutineScope(Dispatchers.Unconfined)).meet()
            assertEquals(listOf(ColourWriteDto("matcha", 40, 300)), account.writes)
            assertEquals(ColourSetting("matcha", 40, 300), choice.current.value)
        }
    }

    @Test
    fun nothingChosenAnywhere_nothingWritten() = runBlocking {
        val account = FakeSettings(null)

        sync(account).meet()

        assertEquals(emptyList<ColourWriteDto>(), account.writes)
        assertNull(colour.current.value)
    }

    @Test
    fun aChoice_goesUpAtOnce_andALaterOneElsewhere_isShownInstead() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val account = FakeSettings(ColourDto("peach", 42, changedAt = Long.MAX_VALUE / 2), keepsLater = true)
        ColourSync(account, colour, accounts, MutableSharedFlow(), scope).start()

        colour.choose("sky")
        withTimeout(WAIT) { while (colour.current.value?.name != "peach") delay(10) }

        assertEquals("sky", account.writes.single().name)
        assertEquals(ColourSetting("peach", 42, Long.MAX_VALUE / 2), colour.current.value)
        scope.cancel()
    }

    @Test
    fun aGuest_keepsTheColourOnTheDevice() = runBlocking {
        val guest = FakeAccountRepository(Account.Guest)
        val account = FakeSettings(ColourDto("sky", 212, changedAt = 900))
        val sync = ColourSync(account, colour, guest, MutableSharedFlow(), CoroutineScope(Dispatchers.Unconfined))

        colour.choose("matcha")
        sync.keep(colour.current.value!!)
        sync.meet()

        assertEquals(0, account.reads)
        assertEquals(emptyList<ColourWriteDto>(), account.writes)
        assertEquals("matcha", colour.current.value?.name)
    }

    @Test
    fun aRefusal_leavesTheDevicesColour() = runBlocking {
        colour.choose("matcha")
        val account = FakeSettings(refuse = 403)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val signIns = MutableSharedFlow<String>()
        ColourSync(account, colour, accounts, signIns, scope).start()

        signIns.emit("ann")

        assertEquals(1, account.reads)
        assertEquals("matcha", colour.current.value?.name)
        scope.cancel()
    }

    @Test
    fun aNameThisBuildDoesNotKnow_isKept_andNotWrittenBack() = runBlocking {
        val account = FakeSettings(ColourDto("rose", 330, changedAt = 500))

        sync(account).meet()
        sync(account).meet()

        assertEquals(ColourSetting("rose", 330, 500), colour.current.value)
        assertEquals(emptyList<ColourWriteDto>(), account.writes)
    }

    // QA D4, 19.11: a colour chosen on the site shows when the app is back on screen, read at most every half minute.
    @Test
    fun backOnScreen_readsTheAccountsColour_atMostEveryHalfMinute() = runBlocking {
        var clock = 1_000_000L
        val account = FakeSettings(ColourDto("sky", 300, changedAt = 200))
        val sync = ColourSync(account, colour, accounts, MutableSharedFlow(), CoroutineScope(Dispatchers.Unconfined)) { clock }

        sync.backOnScreen()
        assertEquals(ColourSetting("sky", 300, 200), colour.current.value)
        account.colour = ColourDto("matcha", 300, changedAt = 300)
        clock += 10_000
        sync.backOnScreen()
        assertEquals(1, account.reads)

        clock += 30_000
        sync.backOnScreen()
        assertEquals(2, account.reads)
        assertEquals("matcha", colour.current.value?.name)
    }

    // An account where no own colour was ever made: its hue stays none, and none goes back up.
    @Test
    fun noOwnHue_staysNone() = runBlocking {
        val account = FakeSettings(ColourDto("sky", null, changedAt = 200))

        sync(account).meet()

        assertEquals(ColourSetting("sky", null, 200), colour.current.value)
        assertEquals(emptyList<ColourWriteDto>(), account.writes)
    }

    private companion object {
        const val WAIT = 2_000L
    }
}
