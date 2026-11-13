package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * Where a card's picture came from and whom it names, as the server works it
 * out (Proxy CONTRACT.md, PictureAttribution). Kept on the card as it came and
 * sent back with it; [source] stays as the server wrote it, so a catalogue
 * this build has never heard of goes back unchanged.
 */
data class PictureAttribution(
    val source: String,
    /** The person the licence asks to be named. */
    val author: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    /** The page about the picture, or about its title, at the source. */
    val link: String? = null,
    /** Only while the picture is the server's copy: the catalogue address it was copied from. */
    val originalUrl: String? = null,
    /**
     * The picture's title at its catalogue, such as `tmdb:movie:348`: what the
     * server finds a dead address again by. Copied from the search, never made up.
     */
    val id: String? = null,
    /** Addresses the picture had before the server found it again; the server's to write. */
    val formerUrls: List<String> = emptyList(),
) {
    companion object {
        const val TMDB = "tmdb"
        private const val TMDB_IMAGE_HOST = "image.tmdb.org"

        /**
         * Whether TMDB's notice belongs with this picture: by its attribution,
         * or for a card made before there was one, by the address's host.
         */
        fun isTmdb(imageUrl: String?, attribution: PictureAttribution?): Boolean =
            if (attribution != null) attribution.source == TMDB else hostOf(imageUrl) == TMDB_IMAGE_HOST

        private fun hostOf(url: String?): String? =
            url?.substringAfter("https://", missingDelimiterValue = "")?.substringBefore('/')?.ifEmpty { null }
    }
}
