package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * Every section this phone knows, as screens name and draw them: the list in
 * the server's order, the odd one met alone, and the ids folded into others.
 */
data class SectionDirectory(
    /** In the server's order (§4), Other last; the nine built-ins until anything was read. */
    val sections: List<Section> = Section.BUILT_IN,
    /** Sections asked for one at a time because the list did not carry them. */
    val extra: Map<String, Section> = emptyMap(),
    /** An id merged away, and the id it lives on as. */
    val merged: Map<String, String> = emptyMap(),
    /** Ids the server has never heard of: drawn as Other, and not asked about again until the next answer. */
    val unknown: Set<String> = emptySet(),
    /** When the list last came from the server; null while it is still the app's own nine. */
    val fetchedAtMillis: Long? = null,
) {
    private val byId: Map<String, Section> by lazy { extra + sections.associateBy { it.id } }

    /** What ordinary screens draw, in order. */
    val shown: List<Section> get() = sections.filter { it.shown }

    /** This person's own proposals still waiting, oldest first: only they see them. */
    val waiting: List<Section> get() = sections.filter { it.waiting }

    /** This person's proposals that were declined or merged, until they dismiss them. */
    val settledAgainst: List<Section> get() = sections.filter { it.settledAgainst }

    /** A proposal just sent, at the end where the server keeps them. */
    fun withProposal(section: Section): SectionDirectory =
        copy(sections = sections.filterNot { it.id == section.id } + section.copy(mine = true))

    /** A proposal withdrawn, or a notice dismissed. */
    fun withoutProposal(id: String): SectionDirectory = copy(sections = sections.filterNot { it.id == id && it.mine })

    /** Whether the counts are the server's rather than the zeros the app starts with. */
    val counted: Boolean get() = fetchedAtMillis != null

    /** Follows merges to the id that lives on; a loop stops where it would repeat. */
    fun resolve(id: String): String {
        var current = id
        val seen = mutableSetOf(current)
        while (true) {
            val next = merged[current] ?: return current
            if (!seen.add(next)) return current
            current = next
        }
    }

    fun find(id: String?): Section? = id?.let { byId[resolve(it)] }

    /** Nothing left to ask the server about this id: it is known, or known not to exist. */
    fun settled(id: String): Boolean = find(id) != null || resolve(id) in unknown

    /** One follow changed here, redrawn by the server's own rule, so the tile moves now rather than at the next answer. */
    fun withFollowing(id: String, now: Boolean): SectionDirectory {
        val target = resolve(id)
        return copy(
            sections = SectionOrder.arrange(sections.map { if (it.id == target) it.followed(now) else it }),
            extra = extra.mapValues { (_, section) -> if (section.id == target) section.followed(now) else section },
        )
    }
}

/** The order of docs/sections-api.md §4, for redrawing after a follow made on this phone. */
object SectionOrder {

    /**
     * Followed first; then pinned, lowest rank first; then the busiest of the
     * last thirty days; then the earlier approved. Other always last. A stable
     * sort, so what the rule cannot tell apart keeps the order it came in.
     */
    fun arrange(sections: List<Section>): List<Section> = sections.sortedWith(ORDER)

    private val ORDER: Comparator<Section> = compareBy<Section> { it.isOther }
        .thenByDescending { it.following }
        .thenBy { it.rank == null }
        .thenBy { it.rank ?: 0 }
        .thenByDescending { it.lists30d }
        .thenBy { it.approvedAt }
}
