package com.cyberdyne.markdown.screenshots

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.cyberdyne.markdown.codeblocks.CodeHighlightTheme
import com.cyberdyne.markdown.codeblocks.RegexSyntaxHighlighter
import com.cyberdyne.markdown.engine.services.LocalMarkdownServices
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import com.cyberdyne.markdown.latex.NativeLatexRenderer

fun galleryServices(dark: Boolean) = MarkdownServices(
    syntaxHighlighter = RegexSyntaxHighlighter(if (dark) CodeHighlightTheme.AtomOneDark else CodeHighlightTheme.AtomOneLight),
    latexRenderer = NativeLatexRenderer(),
)

@Composable
fun GalleryFrame(theme: MarkdownTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMarkdownTheme provides theme,
        LocalMarkdownServices provides galleryServices(theme == MarkdownTheme.Dark),
    ) {
        Surface(color = theme.background) { content() }
    }
}
