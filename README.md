# Android Markdown Engine

A fully native Android/Kotlin Markdown renderer and editor built with **Jetpack
Compose** — no `WebView`, no JavaScript. It mirrors the iOS
[swift-markdown-engine](https://github.com/CyberdyneCorp/swift-markdown-engine)
feature-for-feature and, where Kotlin idiom allows, name-for-name.

CommonMark + GFM, plus math, Mermaid diagrams, footnotes, callouts, wiki-links,
frontmatter, and video embeds. The core module has **zero third-party runtime
dependencies**; syntax highlighting and LaTeX are optional bridge modules.

## Modules

| Module | Maps to iOS product | Contents |
|---|---|---|
| `markdown-engine` | `SwiftMarkdownEngine` | parser, immutable model, Compose renderer, Mermaid, theming, services, serialization, video |
| `markdown-editor` | `MarkdownEditor` | live-styled source editor, toolbar, controller |
| `markdown-engine-codeblocks` | `MarkdownEngineCodeBlocks` | `SyntaxHighlighter` bridge (no JS) |
| `markdown-engine-latex` | `MarkdownEngineLatex` | `LatexRenderer` bridge (native Canvas math) |
| `markdown-engine-wear` | watchOS subset | `WearMarkdownView` render-only constrained subset |
| `sample` | example app | runnable Preview + Editor demo |

All library modules publish under the `com.cyberdyne.markdown` Maven group.

## Render

```kotlin
import com.cyberdyne.markdown.engine.rendering.MarkdownView

MarkdownView("# Hello\n\nSome **bold** text and \$E=mc^2\$.")
```

## Theme

```kotlin
MarkdownView(source, theme = MarkdownTheme.Dark)
// or provide via CompositionLocal so nested views inherit it:
CompositionLocalProvider(LocalMarkdownTheme provides myTheme) { /* ... */ }
```

## Configuration

```kotlin
MarkdownView(
    markdown = source,
    configuration = MarkdownConfiguration(
        interactiveCheckboxes = true,
        showCodeLineNumbers = true,
        enabledExtensions = MarkdownExtension.ALL,       // math, mermaid, footnotes, wiki-links, …
        diagramSizing = DiagramSizing.SCROLL,
    ),
)
```

## Custom services (highlighting + math)

```kotlin
val services = MarkdownServices(
    syntaxHighlighter = RegexSyntaxHighlighter(CodeHighlightTheme.AtomOneDark),
    latexRenderer = NativeLatexRenderer(),
)
MarkdownView(source, services = services)
```

## Custom block renderers

```kotlin
MarkdownView(
    markdown = source,
    blockRenderers = mapOf(
        BlockKind.THEMATIC_BREAK to { _, theme -> DottedRule(theme.accent) },
    ),
)
```

## Edit

```kotlin
import com.cyberdyne.markdown.editor.MarkdownEditor
import com.cyberdyne.markdown.editor.rememberMarkdownEditorState

val state = rememberMarkdownEditorState("# Draft\n\nType **Markdown** here…")
MarkdownEditor(
    state = state,
    toolbar = listOf(
        MarkdownToolbarItem.Bold, MarkdownToolbarItem.Italic, MarkdownToolbarItem.Divider,
        MarkdownToolbarItem.Heading(2), MarkdownToolbarItem.BulletList, MarkdownToolbarItem.Link,
        MarkdownToolbarItem.Custom(id = "emph", icon = "✦", label = "Bold+Italic") { c ->
            c.toggleBold(); c.toggleItalic()
        },
    ),
)
```

## Parse / serialize

```kotlin
val doc = Markdown.parse(source)          // or Markdown.parseAsync(source) off the main thread
val roundTripped = doc.toMarkdown()
```

## Building

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26).

```bash
./gradlew test assembleDebug     # unit tests + library AARs + sample APK
```

### Visual verification (screenshots)

The `screenshots` module renders the real Compose UI to PNGs on the JVM with
[Paparazzi](https://github.com/cashapp/paparazzi) — no emulator needed (the
Android analog of Playwright screenshots).

```bash
./gradlew :screenshots:recordPaparazziDebug   # regenerate golden images
./gradlew :screenshots:verifyPaparazziDebug    # diff against goldens (run in a matching env)
```

Goldens live under `screenshots/src/test/snapshots/images/` and cover the rich
document (light/dark), highlighted code + native LaTeX, a Mermaid flowchart, and
the editor. Goldens here were recorded on macOS; `verify` is font-rendering
sensitive, so record in your CI environment before enabling it as a gate.

The project is spec-driven with [OpenSpec](https://openspec.dev); living specs
are under `openspec/`.

## Platforms

Android phone/tablet. A Wear OS render-only subset is planned. The core model is
immutable and safe to parse off the main thread.
