package com.cyberdyne.markdown.engine.video

import java.net.URI

/** The category of a URL for video-embed purposes. Mirrors the iOS classifier. */
enum class VideoKind {
    /** A direct video file (`.mp4`, `.mov`, `.m4v`, `.m3u8`). */
    DIRECT_FILE,

    /** A known streaming provider (YouTube, Vimeo). */
    PROVIDER,

    /** Not a video. */
    NOT_VIDEO,
}

/**
 * Pure, deterministic classification of a URL string into a [VideoKind]. No Android
 * dependencies, fully unit-testable.
 */
object VideoUrls {
    private val fileExtensions = listOf(".mp4", ".mov", ".m4v", ".m3u8")
    private val providerHosts = listOf("youtube.com", "youtu.be", "vimeo.com")

    fun classify(url: String): VideoKind {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        if (fileExtensions.any { path.endsWith(it) }) return VideoKind.DIRECT_FILE

        val host = hostOf(url)?.removePrefix("www.")?.removePrefix("m.")
        if (host != null && providerHosts.any { host == it || host.endsWith(".$it") }) {
            return VideoKind.PROVIDER
        }
        return VideoKind.NOT_VIDEO
    }

    private fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()
    } catch (_: Exception) {
        null
    }
}
