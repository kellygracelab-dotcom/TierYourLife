package com.artiuillab.tieryourlife.baselineprofile

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

// What a guest sees with nothing but this device: no account is signed in, and
// a step whose content never arrives (the community feed, without the proxy
// admitting this install) is skipped rather than failed.

val targetAppId: String =
    InstrumentationRegistry.getArguments().getString("targetAppId")?.takeIf { it.isNotEmpty() }
        ?: "com.artiuillab.tieryourlife"

private const val SHORT_WAIT = 2_000L
private const val LONG_WAIT = 10_000L
private const val SEED_ITEMS = 45
private const val SEEDED_ROWS = 3
private const val ATTEMPTS = 3

private const val EDGE_FRACTION = 5
private const val FLING_STEPS = 8
private const val SWIPE_STEPS = 20
private const val DRAG_STEPS = 200

private val LISTS_TAB: BySelector = By.res("home_tab_mine")
private val COMMUNITY_TAB: BySelector = By.res("home_tab_community")
private val TIERS: BySelector = By.res("tier_detail_tier_list")
private val EXPLORE: BySelector = By.res("explore_screen")
private val POOL_ITEMS: BySelector = By.res("tier_detail_pool_items")
private val TILE: BySelector = By.res(Pattern.compile("tier_detail_tile_\\d+"))
private val TEXT_FIELD: BySelector = By.clazz("android.widget.EditText")
private val BOARD_ON_HOME: BySelector = By.res(Pattern.compile("tier_list_(tile|card)_\\d+"))
private val COMMUNITY_CARD: BySelector = By.res(Pattern.compile("explore_card_(?!author_).+"))

fun MacrobenchmarkScope.waitForLists() {
    device.wait(Until.hasObject(LISTS_TAB), LONG_WAIT)
    device.waitForIdle()
}

/** Leaves a board with cards on the home screen, making one on the first run. */
fun MacrobenchmarkScope.ensureBoard() {
    if (device.hasObject(BOARD_ON_HOME)) return
    if (!tap(By.res("home_suggestion_0"))) return
    if (!device.wait(Until.hasObject(TIERS), LONG_WAIT)) return
    repeat(SEED_ITEMS) {
        addItemByHand("Card ${it + 1}")
        dragFromPoolInto(row = it % SEEDED_ROWS + 1)
    }
    device.pressBack()
    waitForLists()
}

// A card leaves the pool by being lifted upwards and carried, which a tap or a
// fling does not do; a slow drag does.
private fun MacrobenchmarkScope.dragFromPoolInto(row: Int) {
    if (!device.hasObject(POOL_ITEMS)) tap(By.res("tier_detail_pool_toggle"))
    val card = fresh { device.findObject(POOL_ITEMS)?.findObject(TILE)?.visibleBounds } ?: return
    val target = fresh { device.findObject(By.res("tier_detail_row_$row"))?.visibleBounds } ?: return
    device.drag(card.centerX(), card.centerY(), target.right - target.height() / 2, target.centerY(), DRAG_STEPS)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.addItemByHand(title: String) {
    if (!tap(By.res("tier_detail_manual_add_button"))) return
    // A dialog is a window of its own, which test tags do not reach.
    val label = appString("manual_dialog_add").orEmpty()
    val typed = label.isNotEmpty() && type(TEXT_FIELD, title)
    val added = typed &&
        fresh { device.findObject(TEXT_FIELD)?.windowRoot()?.findObject(By.text(label))?.click() } != null
    if (!added) device.pressBack()
    if (device.hasObject(TEXT_FIELD)) device.pressBack()
    device.wait(Until.gone(TEXT_FIELD), SHORT_WAIT)
}

fun MacrobenchmarkScope.openBoardAndScroll() {
    showTab(LISTS_TAB)
    if (!tap(BOARD_ON_HOME)) return
    if (!device.wait(Until.hasObject(TIERS), LONG_WAIT)) return
    device.waitForIdle()
    scrollDownAndUp(TIERS)
    device.pressBack()
    waitForLists()
}

fun MacrobenchmarkScope.browseCommunity() {
    showTab(COMMUNITY_TAB)
    if (!device.wait(Until.hasObject(EXPLORE), LONG_WAIT)) return
    device.wait(Until.hasObject(COMMUNITY_CARD), SHORT_WAIT)
    scrollDownAndUp(EXPLORE)
    swipeSideways(EXPLORE, towardsEnd = true)
    device.wait(Until.hasObject(By.res("explore_tab_following").selected(true)), SHORT_WAIT)
    swipeSideways(EXPLORE, towardsEnd = false)
    device.wait(Until.hasObject(By.res("explore_tab_explore").selected(true)), SHORT_WAIT)
}

fun MacrobenchmarkScope.openCommunityList() {
    showTab(COMMUNITY_TAB)
    if (!tap(COMMUNITY_CARD, LONG_WAIT)) return
    device.wait(Until.gone(EXPLORE), SHORT_WAIT)
    device.waitForIdle()
    scrollDownAndUp(By.scrollable(true))
    device.pressBack()
    device.wait(Until.hasObject(EXPLORE), SHORT_WAIT)
}

fun MacrobenchmarkScope.openSearch() {
    showTab(LISTS_TAB)
    val label = appString("tier_lists_content_description_search") ?: return
    if (!tap(By.desc(label))) return
    if (type(By.res("home_search_field"), "Card")) device.waitForIdle()
    if (!tap(By.res("home_search_close"))) device.pressBack()
    waitForLists()
}

/** The search a board adds cards from; results need the network, the screen does not. */
fun MacrobenchmarkScope.openCatalogueSearch() {
    showTab(LISTS_TAB)
    if (!tap(BOARD_ON_HOME)) return
    if (!tap(By.res("tier_detail_add_chip"), LONG_WAIT)) return
    if (type(By.res("tier_detail_item_search_field"), "Star")) {
        device.wait(Until.hasObject(By.res("tier_detail_item_search_results_list")), LONG_WAIT)
        scrollDownAndUp(By.res("tier_detail_item_search_results_list"))
    }
    if (!tap(By.res("tier_detail_item_search_close"))) device.pressBack()
    device.wait(Until.hasObject(TIERS), SHORT_WAIT)
    device.pressBack()
    waitForLists()
}

private fun MacrobenchmarkScope.showTab(tab: BySelector) {
    if (fresh { device.findObject(tab)?.isSelected } != true) tap(tab)
}

private fun MacrobenchmarkScope.tap(selector: BySelector, timeout: Long = SHORT_WAIT): Boolean {
    val tapped = fresh { device.wait(Until.findObject(selector), timeout)?.click() } != null
    if (tapped) device.waitForIdle()
    return tapped
}

private fun MacrobenchmarkScope.type(selector: BySelector, text: String): Boolean =
    fresh { device.wait(Until.findObject(selector), SHORT_WAIT)?.setText(text) } != null

// Gestures go through the device at bounds read just before, never through a
// held node: Compose replaces nodes as it recomposes, and a held one goes stale.
private fun MacrobenchmarkScope.scrollDownAndUp(selector: BySelector) {
    val area = fresh { device.findObject(selector)?.visibleBounds } ?: return
    val x = area.centerX()
    val top = area.top + area.height() / EDGE_FRACTION
    val bottom = area.bottom - area.height() / EDGE_FRACTION
    device.swipe(x, bottom, x, top, FLING_STEPS)
    device.waitForIdle()
    device.swipe(x, top, x, bottom, FLING_STEPS)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.swipeSideways(selector: BySelector, towardsEnd: Boolean) {
    val area: Rect = fresh { device.findObject(selector)?.visibleBounds } ?: return
    val y = area.centerY()
    val left = area.left + area.width() / EDGE_FRACTION
    val right = area.right - area.width() / EDGE_FRACTION
    if (towardsEnd) {
        device.swipe(right, y, left, y, SWIPE_STEPS)
    } else {
        device.swipe(left, y, right, y, SWIPE_STEPS)
    }
    device.waitForIdle()
}

private fun <T> MacrobenchmarkScope.fresh(block: () -> T?): T? {
    repeat(ATTEMPTS) {
        try {
            return block()
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
    return null
}

private fun UiObject2.windowRoot(): UiObject2 {
    var node = this
    while (true) node = node.parent ?: return node
}

// Labels are looked up in the app's own resources, so the journeys read the
// same in every language the device may be set to.
@SuppressLint("DiscouragedApi")
private fun appString(name: String): String? {
    val resources = InstrumentationRegistry.getInstrumentation().context.packageManager
        .getResourcesForApplication(targetAppId)
    val id = resources.getIdentifier(name, "string", targetAppId)
    return if (id == 0) null else resources.getString(id)
}
