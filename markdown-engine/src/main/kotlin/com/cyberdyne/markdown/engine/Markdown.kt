package com.cyberdyne.markdown.engine

import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.model.MarkdownDocument
import com.cyberdyne.markdown.engine.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Top-level entry point for parsing Markdown. The resulting [MarkdownDocument] is
 * an immutable value safe to hand from a background thread to the UI thread.
 */
object Markdown {

    /** Parse synchronously on the calling thread. */
    fun parse(
        source: String,
        configuration: MarkdownConfiguration = MarkdownConfiguration.Default,
    ): MarkdownDocument = MarkdownParser(configuration).parse(source)

    /** Parse off the main thread on [Dispatchers.Default]. */
    suspend fun parseAsync(
        source: String,
        configuration: MarkdownConfiguration = MarkdownConfiguration.Default,
    ): MarkdownDocument = withContext(Dispatchers.Default) {
        MarkdownParser(configuration).parse(source)
    }
}
