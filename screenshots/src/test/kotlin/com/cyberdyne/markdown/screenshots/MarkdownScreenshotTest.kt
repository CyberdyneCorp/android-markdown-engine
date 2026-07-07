package com.cyberdyne.markdown.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.cyberdyne.markdown.codeblocks.CodeHighlightTheme
import com.cyberdyne.markdown.codeblocks.RegexSyntaxHighlighter
import com.cyberdyne.markdown.editor.MarkdownEditor
import com.cyberdyne.markdown.editor.rememberMarkdownEditorState
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.services.LocalMarkdownServices
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import com.cyberdyne.markdown.latex.NativeLatexRenderer
import org.junit.Rule
import org.junit.Test

class MarkdownScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2600),
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    private fun servicesFor(dark: Boolean) = MarkdownServices(
        syntaxHighlighter = RegexSyntaxHighlighter(if (dark) CodeHighlightTheme.AtomOneDark else CodeHighlightTheme.AtomOneLight),
        latexRenderer = NativeLatexRenderer(),
    )

    @Composable
    private fun Frame(theme: MarkdownTheme, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalMarkdownTheme provides theme,
            LocalMarkdownServices provides servicesFor(theme == MarkdownTheme.Dark),
        ) {
            Surface(color = theme.background) {
                content()
            }
        }
    }

    @Test fun document_light() {
        paparazzi.snapshot("document-light") {
            Frame(MarkdownTheme.Light) {
                MarkdownView(markdown = RICH, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    @Test fun document_dark() {
        paparazzi.snapshot("document-dark") {
            Frame(MarkdownTheme.Dark) {
                MarkdownView(markdown = RICH, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    @Test fun code_and_math() {
        paparazzi.snapshot("code-and-math") {
            Frame(MarkdownTheme.Dark) {
                MarkdownView(markdown = CODE_MATH, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    @Test fun mermaid_flowchart() {
        paparazzi.snapshot("mermaid-flowchart") {
            Frame(MarkdownTheme.Light) {
                MarkdownView(markdown = MERMAID, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    @Test fun editor() {
        paparazzi.snapshot("editor") {
            Frame(MarkdownTheme.Light) {
                val state = rememberMarkdownEditorState("# Draft\n\nType **bold**, *italic*, `code`.\n\n- [x] done\n- [ ] todo")
                MarkdownEditor(state = state, modifier = Modifier.fillMaxSize())
            }
        }
    }

    private companion object {
        val RICH = """
            # Android Markdown Engine

            A **fully native** renderer — *no WebView*. Supports ~~most~~ all of
            CommonMark + GFM.

            - [x] Parse CommonMark
            - [x] Render with Compose
            - [ ] Rule the world

            | Feature | Status |
            |:--------|-------:|
            | Parser  | done   |
            | Render  | done   |

            > [!NOTE]
            > Callouts render natively.

            See the [project](https://example.com).
        """.trimIndent()

        val CODE_MATH = """
            ## Code & math

            ```kotlin
            fun greet(name: String): String {
                // a friendly greeting
                return "Hello, ${'$'}name"
            }
            ```

            The quadratic roots:

            $$
            \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
            $$

            A matrix ${'$'}\begin{pmatrix} a & b \\ c & d \end{pmatrix}${'$'}.
        """.trimIndent()

        val MERMAID = """
            ## Flowchart

            ```mermaid
            flowchart TD
                A[Start] --> B{Works?}
                B -->|Yes| C[Ship it]
                B -->|No| D[Fix it]
                D --> B
                style C fill:#00c853,stroke:#007e33
                style D fill:#ff5252,stroke:#b71c1c
            ```
        """.trimIndent()
    }
}
