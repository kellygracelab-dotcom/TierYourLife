package com.artiuillab.tieryourlife.feature.community.domain.model

/** What a person sends to propose a section (`POST lists/sections/proposals`). */
data class SectionProposal(
    val name: String,
    /** One of [SectionSymbols.ALL]. */
    val icon: String,
    /** The cropped picture, already in this person's own folder. */
    val pictureId: String,
    /** The approved section their lists live in until a moderator decides. */
    val fileUnder: String,
)

/** What the server says of a name before anything is sent (`GET lists/sections/name-check`). */
sealed interface SectionNameCheck {
    data object Free : SectionNameCheck

    /** Another section, or another waiting proposal, already goes by it. */
    data class Taken(val id: String?) : SectionNameCheck

    /** Too short, too long, or without a letter or digit. */
    data object Bad : SectionNameCheck
}

/** Why a proposal was refused, each in terms a screen can explain. */
sealed interface ProposalError {
    data object BadName : ProposalError

    data class NameTaken(val id: String?) : ProposalError

    data object BadIcon : ProposalError

    data object BadFileUnder : ProposalError

    data class BadPicture(val why: PictureProblem) : ProposalError

    /** Three of this person's proposals already wait for a moderator. */
    data object TooManyPending : ProposalError

    /** Ten proposals in the last day, however they ended. */
    data object TooManyToday : ProposalError

    /** Null for a ban with no end. */
    data class Banned(val untilMillis: Long?) : ProposalError

    data object NotSignedIn : ProposalError

    /** No answer, or one nobody wrote copy for: nothing was lost and it may be tried again. */
    data object Failed : ProposalError
}

enum class PictureProblem(val id: String) {
    Missing("missing"),
    NotImage("not_image"),
    TooSmall("too_small"),
    Not16By10("not_16_10"),
    ;

    companion object {
        fun fromId(id: String?): PictureProblem = entries.firstOrNull { it.id == id } ?: NotImage
    }
}

class ProposalRefused(val error: ProposalError) : Exception(error.toString())

/** How many proposals one person may have waiting at once. */
const val MAX_PENDING_SECTIONS = 3

/** The longest name a section may have, in characters. */
const val MAX_SECTION_NAME = 24

/**
 * The part of a picture a section shows, as fractions of the picture's own
 * width and height: the same numbers whatever size it is decoded at.
 */
data class PictureCrop(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

/** A section's picture: 16:10, at least 960 × 600, and never more than 1440 × 900. */
object SectionPictureSize {
    const val ASPECT = 1.6f
    const val MIN_WIDTH = 960
    const val MAX_WIDTH = 1440

    /** The exported width for a crop [cropWidthPx] wide: as wide as it is up to the most, null when it is narrower than the least. */
    fun exportWidth(cropWidthPx: Int): Int? = if (cropWidthPx < MIN_WIDTH) null else minOf(cropWidthPx, MAX_WIDTH)
}
