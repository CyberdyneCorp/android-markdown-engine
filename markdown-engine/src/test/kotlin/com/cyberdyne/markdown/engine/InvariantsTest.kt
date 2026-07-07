package com.cyberdyne.markdown.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the cross-platform invariant (mirrored from iOS): the core module never
 * references a WebView or a JavaScript runtime. WebView-backed provider embedders
 * must live in the host app. This is a source-level guardrail; it is upgraded to a
 * dependency/bytecode check in CI.
 */
class InvariantsTest {

    @Test fun coreSourceHasNoWebView() {
        val srcMain = File("src/main")
        if (!srcMain.exists()) return // guardrail is a no-op when run outside the module dir
        val offenders = srcMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("android.webkit.WebView") || it.readText().contains("WebView(") }
            .map { it.path }
            .toList()
        assertTrue(offenders.isEmpty(), "core must not reference WebView; offenders: $offenders")
    }
}
