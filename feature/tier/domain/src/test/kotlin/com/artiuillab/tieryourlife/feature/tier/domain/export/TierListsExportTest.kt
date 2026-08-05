package com.artiuillab.tieryourlife.feature.tier.domain.export

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Test

// Stands in for real Android plurals/resources — deliberately dumb (no real English
// plural rules) so a test failure can't be masked by the fake accidentally doing the
// right thing; each assertion below spells out exactly which count reached the fake.
private val testStrings = TierListsExportStrings(
    header = "TierYourLife",
    exportedOn = "Exported 5 August 2026",
    listCountText = { count -> "$count list(s)" },
    rankedCountText = { count -> "$count ranked" },
    unrankedCountText = { count -> "$count unranked" },
    tierWithCaptionFormat = "%1\$s — %2\$s",
    tierPlainFormat = "%1\$s",
    tierEmptyLabel = "(empty)",
    unrankedHeading = "Unranked",
)

private fun item(id: Long, title: String) = TierItem(id = id, title = title, imageUrl = null)

private fun tier(
    id: Long,
    label: String,
    caption: String? = null,
    items: List<TierItem> = emptyList(),
    isPool: Boolean = false,
) = Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool, caption = caption)

class TierListsExportTest {

    @Test
    fun header_reportsTotalsAcrossEveryList() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Sci-fi films",
                tiers = listOf(
                    tier(10, "S", "Masterpiece", listOf(item(1, "Interstellar"), item(2, "Arrival"))),
                    tier(11, "Pool", items = listOf(item(3, "Dune")), isPool = true),
                ),
            ),
            TierList(
                id = 2,
                title = "Every A24 film",
                tiers = listOf(
                    tier(20, "S", items = listOf(item(4, "Hereditary"))),
                ),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)
        val firstLines = output.lines().take(3)

        assertEquals(
            listOf("TierYourLife", "Exported 5 August 2026", "2 list(s) · 3 ranked · 1 unranked"),
            firstLines,
        )
    }

    @Test
    fun list_withAnEmptyTier_printsThePlaceholderInsteadOfDroppingIt() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Solo",
                tiers = listOf(tier(10, "S", "Masterpiece", items = emptyList())),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)

        assertEquals(
            listOf(
                "TierYourLife",
                "Exported 5 August 2026",
                "1 list(s) · 0 ranked · 0 unranked",
                "",
                "Solo",
                "====",
                "0 ranked · 0 unranked",
                "",
                "S — Masterpiece",
                "  (empty)",
            ),
            output.lines(),
        )
    }

    @Test
    fun list_withAnEmptyPool_omitsTheUnrankedHeadingEntirely() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Solo",
                tiers = listOf(
                    tier(10, "S", items = listOf(item(1, "Only one"))),
                    tier(11, "Pool", isPool = true, items = emptyList()),
                ),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)

        assertEquals(false, output.contains("Unranked"))
        assertEquals(
            listOf(
                "TierYourLife",
                "Exported 5 August 2026",
                "1 list(s) · 1 ranked · 0 unranked",
                "",
                "Solo",
                "====",
                "1 ranked · 0 unranked",
                "",
                "S",
                "  1. Only one",
            ),
            output.lines(),
        )
    }

    @Test
    fun tier_withACaption_usesTheDashFormat_withoutOne_usesTheLabelAlone() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Solo",
                tiers = listOf(
                    tier(10, "S", "Masterpiece", items = listOf(item(1, "A"))),
                    tier(11, "A", caption = null, items = listOf(item(2, "B"))),
                ),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)

        assertEquals(true, output.contains("S — Masterpiece"))
        assertEquals(true, output.contains("\nA\n"))
    }

    @Test
    fun items_areNumberedWithinTheirOwnTier_restartingAtOneEachTime() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Solo",
                tiers = listOf(
                    tier(10, "S", items = listOf(item(1, "First"), item(2, "Second"))),
                    tier(11, "A", items = listOf(item(3, "Third"))),
                ),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)

        assertEquals(
            listOf(
                "TierYourLife",
                "Exported 5 August 2026",
                "1 list(s) · 3 ranked · 0 unranked",
                "",
                "Solo",
                "====",
                "3 ranked · 0 unranked",
                "",
                "S",
                "  1. First",
                "  2. Second",
                "A",
                "  1. Third",
            ),
            output.lines(),
        )
    }

    @Test
    fun pool_isUnnumberedAndPrintedLast_afterEveryRankedTier() {
        val lists = listOf(
            TierList(
                id = 1,
                title = "Solo",
                tiers = listOf(
                    tier(10, "S", items = listOf(item(1, "Ranked one"))),
                    tier(11, "Pool", isPool = true, items = listOf(item(2, "Loose one"), item(3, "Loose two"))),
                ),
            ),
        )

        val output = buildTierListsExport(lists, testStrings)

        assertEquals(
            listOf(
                "TierYourLife",
                "Exported 5 August 2026",
                "1 list(s) · 1 ranked · 2 unranked",
                "",
                "Solo",
                "====",
                "1 ranked · 2 unranked",
                "",
                "S",
                "  1. Ranked one",
                "Unranked",
                "  Loose one",
                "  Loose two",
            ),
            output.lines(),
        )
    }

    @Test
    fun betweenTwoLists_thereIsExactlyOneBlankLine_andNoneAfterTheLastList() {
        val lists = listOf(
            TierList(id = 1, title = "First", tiers = listOf(tier(10, "S", items = listOf(item(1, "A"))))),
            TierList(id = 2, title = "Second", tiers = listOf(tier(20, "S", items = listOf(item(2, "B"))))),
        )

        val output = buildTierListsExport(lists, testStrings)
        val lines = output.lines()

        // "First"'s single item line is immediately followed by one blank line, then
        // "Second" starts — and the file doesn't end with a trailing blank line.
        val firstIndex = lines.indexOf("First")
        val secondIndex = lines.indexOf("Second")
        assertEquals("", lines[secondIndex - 1])
        assertEquals(true, firstIndex < secondIndex)
        assertEquals("  1. B", lines.last())
    }
}
