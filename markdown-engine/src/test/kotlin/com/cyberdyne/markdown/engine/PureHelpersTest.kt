package com.cyberdyne.markdown.engine

import com.cyberdyne.markdown.engine.code.LanguageAliases
import com.cyberdyne.markdown.engine.video.VideoKind
import com.cyberdyne.markdown.engine.video.VideoUrls
import kotlin.test.Test
import kotlin.test.assertEquals

class PureHelpersTest {

    // code-syntax-highlighting / language aliases
    @Test fun aliasesNormalizeToCanonical() {
        assertEquals("python", LanguageAliases.normalize("py"))
        assertEquals("cpp", LanguageAliases.normalize("c++"))
        assertEquals("bash", LanguageAliases.normalize("sh"))
        assertEquals("typescript", LanguageAliases.normalize("ts"))
    }

    @Test fun unknownLanguagePassesThroughLowercased() {
        assertEquals("swift", LanguageAliases.normalize("Swift"))
        assertEquals(null, LanguageAliases.normalize(null))
        assertEquals(null, LanguageAliases.normalize("  "))
    }

    // video-embeds / classifier
    @Test fun directFileByExtensionIgnoringQuery() {
        assertEquals(VideoKind.DIRECT_FILE, VideoUrls.classify("https://cdn.example.com/a.MP4"))
        assertEquals(VideoKind.DIRECT_FILE, VideoUrls.classify("https://x.com/v.m3u8?token=abc"))
        assertEquals(VideoKind.DIRECT_FILE, VideoUrls.classify("https://x.com/clip.mov#t=10"))
    }

    @Test fun providerByHostIncludingSubdomains() {
        assertEquals(VideoKind.PROVIDER, VideoUrls.classify("https://www.youtube.com/watch?v=abc"))
        assertEquals(VideoKind.PROVIDER, VideoUrls.classify("https://youtu.be/abc"))
        assertEquals(VideoKind.PROVIDER, VideoUrls.classify("https://m.vimeo.com/123"))
    }

    @Test fun notVideoOtherwise() {
        assertEquals(VideoKind.NOT_VIDEO, VideoUrls.classify("https://example.com/photo.png"))
        assertEquals(VideoKind.NOT_VIDEO, VideoUrls.classify("https://news.site/article"))
        assertEquals(VideoKind.NOT_VIDEO, VideoUrls.classify("not a url"))
    }
}
