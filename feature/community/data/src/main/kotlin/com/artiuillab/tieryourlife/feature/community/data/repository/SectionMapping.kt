package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionMergedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ThemePairDto
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionSymbols
import com.artiuillab.tieryourlife.feature.community.domain.model.ThemePair
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException

/**
 * A section from what the server sent. Whatever is missing, as on a server
 * from before sections were data, takes the built-in's own or the default; an
 * icon the app cannot draw is the default, and only a web picture is kept.
 */
internal fun SectionDto.toDomain(me: String? = null): Section {
    val art = ListCategory.fromId(id)
    val status = SectionStatus.fromId(status)
    return Section(
        id = id,
        name = name.orEmpty().trim(),
        names = names.filterValues { it.isNotBlank() },
        icon = icon?.takeIf { it in SectionSymbols.ALL } ?: art?.symbol ?: SectionSymbols.DEFAULT,
        // The nine draw their own art, whatever is sent.
        pictureUrl = pictureUrl?.takeIf { art == null && it.startsWith("https://") },
        tint = tint?.toDomain(),
        topicBg = topicBg?.toDomain(),
        topicIcon = topicIcon?.toDomain(),
        status = status,
        hidden = hidden,
        builtIn = builtIn ?: (art != null),
        rank = rank,
        approvedAt = approvedAt ?: 0L,
        listCount = listCount,
        lists30d = lists30d,
        following = following,
        collage = collage.filter { it.startsWith("https://") }.take(COLLAGE_SIZE),
        fileUnder = fileUnder,
        // Moderators are sent everybody's proposals; only the ones this account sent are its own.
        mine = status != SectionStatus.Approved && me != null && proposerUid == me,
        declineNote = declineNote?.takeIf { it.isNotBlank() },
        mergedInto = mergedInto,
        waitingLists = waitingLists,
    )
}

/** The way back, so the phone keeps what it was sent in the shape it was sent. */
internal fun Section.toDto() = SectionDto(
    id = id,
    name = name.ifEmpty { null },
    names = names,
    icon = icon,
    pictureUrl = pictureUrl,
    tint = tint?.let { ThemePairDto(it.light, it.dark) },
    topicBg = topicBg?.let { ThemePairDto(it.light, it.dark) },
    topicIcon = topicIcon?.let { ThemePairDto(it.light, it.dark) },
    status = status.id,
    hidden = hidden,
    builtIn = builtIn,
    rank = rank,
    approvedAt = approvedAt,
    listCount = listCount,
    lists30d = lists30d,
    following = following,
    collage = collage,
    fileUnder = fileUnder,
    declineNote = declineNote,
    mergedInto = mergedInto,
    waitingLists = waitingLists,
)

/** A colour pair only when both halves read as colours: half a pair is no look, and Other's stands in. */
private fun ThemePairDto.toDomain(): ThemePair? = ThemePair(light, dark).takeIf { HEX.matches(light) && HEX.matches(dark) }

private val HEX = Regex("^#[0-9A-Fa-f]{6}$")

private const val COLLAGE_SIZE = 4

/**
 * The list in the server's order, which is already the one to draw (§4), with
 * Other moved last in case it was not. An empty answer is no answer: the nine
 * stand rather than nothing at all.
 */
internal fun sectionsFrom(answer: List<SectionDto>, me: String? = null): List<Section> {
    val all = answer.filter { it.id.isNotBlank() }.distinctBy { it.id }.map { it.toDomain(me) }
    if (all.isEmpty()) return Section.BUILT_IN
    return all.filterNot { it.isOther } + all.filter { it.isOther }
}

/** One section's answer, bare or wrapped as `{"section": …}`: the draft names the call, not the envelope. */
internal fun sectionFrom(answer: JsonObject, me: String? = null): Section {
    val body = (answer["section"] as? JsonObject) ?: answer
    return networkJson.decodeFromJsonElement(SectionDto.serializer(), body).toDomain(me)
}

/** A refusal that is itself an answer: 404 is no such section, 410 one that lives on under another id. Null for any other failure. */
internal fun Throwable.sectionRefusal(): SectionLookup? {
    val refusal = this as? HttpException ?: return null
    return when (refusal.code()) {
        HTTP_NOT_FOUND -> SectionLookup.Unknown
        HTTP_GONE -> {
            val body = refusal.response()?.errorBody()?.string().orEmpty()
            val into = runCatching { networkJson.decodeFromString(SectionMergedDto.serializer(), body) }.getOrNull()?.mergedInto
            if (into.isNullOrBlank()) SectionLookup.Unknown else SectionLookup.Merged(into)
        }

        else -> null
    }
}

private const val HTTP_NOT_FOUND = 404
private const val HTTP_GONE = 410
