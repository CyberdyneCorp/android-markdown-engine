package com.cyberdyne.markdown.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.codeblocks.CodeHighlightTheme
import com.cyberdyne.markdown.codeblocks.RegexSyntaxHighlighter
import com.cyberdyne.markdown.editor.MarkdownEditor
import com.cyberdyne.markdown.editor.MarkdownLiveEditor
import com.cyberdyne.markdown.editor.MarkdownWysiwygEditor
import com.cyberdyne.markdown.editor.rememberMarkdownEditorState
import com.cyberdyne.markdown.editor.rememberWysiwygState
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.services.LocalMarkdownServices
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import com.cyberdyne.markdown.latex.NativeLatexRenderer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleApp() {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme() else lightColorScheme()
    val markdownTheme = if (dark) MarkdownTheme.Dark else MarkdownTheme.Light
    val services = remember(dark) {
        MarkdownServices(
            syntaxHighlighter = RegexSyntaxHighlighter(if (dark) CodeHighlightTheme.AtomOneDark else CodeHighlightTheme.AtomOneLight),
            latexRenderer = NativeLatexRenderer(),
        )
    }

    MaterialTheme(colorScheme = colors) {
        Surface(color = markdownTheme.background) {
            CompositionLocalProvider(
                LocalMarkdownTheme provides markdownTheme,
                LocalMarkdownServices provides services,
            ) {
                var tab by remember { mutableIntStateOf(0) }
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Android Markdown Engine") }) },
                ) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        val tabs = listOf("Preview", "Editor", "Live", "WYSIWYG")
                        TabRow(selectedTabIndex = tab) {
                            tabs.forEachIndexed { i, title ->
                                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                            }
                        }
                        when (tab) {
                            0 -> PreviewTab(markdownTheme)
                            1 -> EditorTab()
                            2 -> LiveTab()
                            else -> WysiwygTab()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTab(theme: MarkdownTheme) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        MarkdownView(markdown = SAMPLE, onLinkClick = { /* handled by default URI handler */ })
    }
}

@Composable
private fun EditorTab() {
    val state = rememberMarkdownEditorState(EDITOR_SEED)
    Column(Modifier.fillMaxSize()) {
        MarkdownEditor(state = state, modifier = Modifier)
    }
}

@Composable
private fun LiveTab() {
    val state = rememberMarkdownEditorState(EDITOR_SEED)
    Column(Modifier.fillMaxSize()) {
        MarkdownLiveEditor(state = state, modifier = Modifier)
    }
}

@Composable
private fun WysiwygTab() {
    val state = rememberWysiwygState(EDITOR_SEED)
    MarkdownWysiwygEditor(state = state, modifier = Modifier.fillMaxSize())
}

private const val EDITOR_SEED = "# Draft\n\nType **Markdown** here.\n\n- [ ] a task\n- a bullet\n"

private val SAMPLE = """
    # Android Markdown Engine

    A **fully native** Markdown renderer — *no WebView, no JavaScript*. Supports
    ~~most~~ all of CommonMark + GFM, plus math, Mermaid, and video.

    ## Lists & tasks

    - [x] Parse CommonMark
    - [x] Render with Compose
    - [ ] Rule the world

    1. First
    2. Second
       - nested

    ## Table

    | Feature | Status |
    |:--------|-------:|
    | Parser  | done   |
    | Render  | done   |

    ## Code

    ```kotlin
    fun greet(name: String) = "Hello, ${'$'}name"
    ```

    ## Math

    Inline ${'$'}E = mc^2${'$'} and a block:

    ${'$'}${'$'}
    \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
    ${'$'}${'$'}

    ## Diagram

    ```mermaid
    flowchart TD
        A[Start] --> B{Works?}
        B -->|Yes| C[Ship it]
        B -->|No| D[Fix it]
        D --> B
    ```

    > [!NOTE]
    > Callouts render too.

    See the [project](https://github.com/CyberdyneCorp/android-markdown-engine).
""".trimIndent()
