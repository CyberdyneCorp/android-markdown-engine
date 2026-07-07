# Design — Android Markdown Engine

## Context

We are porting the iOS `swift-markdown-engine` to Android/Kotlin, mirroring its
17 OpenSpec capabilities and its public API names where Kotlin idiom allows. The
iOS engine is deliberately native (SwiftUI Canvas, CoreText, TextKit 2 — no
WebView/JS) with a zero-dependency core and optional bridge products. We preserve
those invariants on Android.

## Goals / Non-Goals

- **Goals**: feature parity with iOS; same public names; zero-dependency,
  WebView-free core; immutable thread-safe model; optional bridge modules.
- **Non-goals**: Kotlin Multiplatform shared core; Apple Pencil/Scribble parity;
  byte-identical serialization; WebView-backed provider players in-library.

## Module layout (Gradle)

Mirrors the iOS Swift package products:

| Gradle module | iOS product | Contents | Runtime deps |
|---|---|---|---|
| `markdown-engine` | `SwiftMarkdownEngine` | parser, model, rendering, theming, services, serialization, mermaid, video classifier + Media3 player | Compose UI, Media3 (video) only; **no WebView, no JS, no 3rd-party parser** |
| `markdown-editor` | `MarkdownEditor` | live editor, continuous live surface, inline lists, WYSIWYG, toolbar, diagram builders | + core, Compose text |
| `markdown-engine-codeblocks` | `MarkdownEngineCodeBlocks` | `SyntaxHighlighter` bridge | + core, a native highlighter lib |
| `markdown-engine-latex` | `MarkdownEngineLatex` | `LatexRenderer` bridge | + core, a native math lib |
| `sample` | example apps | demo/host app exercising the library | all |

Media3 lives behind the video feature; if strict "zero deps" is required for the
absolute core, the inline player can be split into a `markdown-engine-video`
module later. For v1 we keep it in core (matches iOS bundling AVKit in core) but
gate it so it is only linked when video blocks are used.

## Key iOS → Android mappings

| iOS / Swift | Android / Kotlin |
|---|---|
| SwiftUI `MarkdownView` / `MarkdownEditor` | `@Composable MarkdownView` / `MarkdownEditor` |
| SwiftUI environment | `CompositionLocal` (`LocalMarkdownTheme`, `LocalMarkdownServices`) |
| view modifiers (`.markdownTheme(.dark)`) | function parameters (`theme = MarkdownTheme.Dark`) |
| TextKit 2 live-styled source editor | `BasicTextField` + `TextFieldState` + cursor-aware `VisualTransformation`/`OffsetMapping` |
| CoreText math | Compose `Canvas`/text + native math lib (JLaTeXMath-style), no MathJax |
| SwiftUI `Canvas` (Mermaid) | Compose `Canvas` + custom Kotlin layout engine |
| AVKit / `AVPlayer` | Media3 / ExoPlayer `PlayerView` |
| `openURL` | `Intent.ACTION_VIEW` |
| Pasteboard | `ClipboardManager` |
| Dynamic Type / VoiceOver | Compose `fontScale` / TalkBack `semantics { heading() }` |
| SF Symbols (`systemImage`) | `ImageVector` / drawable |
| `Sendable` value types | immutable `data class` / `sealed` hierarchies |
| off-main-actor parsing | `Dispatchers.Default` coroutine |

## Model design

- `MarkdownDocument(blocks: List<BlockNode>, metadata: Map<String,String>)` —
  immutable `data class`; `metadata` holds parsed YAML frontmatter.
- `BlockNode` and `InlineNode` are `sealed` hierarchies; each node carries
  `range: SourceRange` (UTF-8 byte offsets `[start, end)`).
- **UTF-8 offset caution**: Kotlin `String` indexing is UTF-16. The parser will
  work over a `ByteArray`/UTF-8 view (or track a parallel UTF-8 offset) so ranges
  match the iOS contract and round-trip editing stays byte-accurate.
- `BlockKind` enum (used by `custom-block-renderers`) enumerates block cases
  (`Heading`, `Paragraph`, `CodeBlock`, `Table`, `Callout`, `Mermaid`, …).

## Parser design

- Hand-written recursive-descent CommonMark parser (block phase → inline phase),
  no third-party dependency, targeting CommonMark + GFM + gated extensions.
- Extension recognition (math, mermaid, footnotes, wiki-links, callouts,
  frontmatter) gated by `MarkdownConfiguration.enabledExtensions`.
- **Decision — own parser vs. library**: we write our own to honor the
  zero-dependency-core invariant and to preserve exact source ranges for the
  editor. This is the single largest correctness surface; it will be driven by a
  CommonMark/GFM spec fixture suite (port the iOS `Fixtures`).

## Rendering design

- `MarkdownView` walks the document, emitting one Compose node per block through
  a `BlockRenderer` registry (built-ins + host overrides from
  `custom-block-renderers`).
- Inline rendering builds an `AnnotatedString` for text runs; non-text inlines
  (images, inline math, wiki-links) use Compose `InlineTextContent`.
- Wide blocks wrap in a per-block `horizontalScroll` container.

## Editor design (the hard part)

- Source-of-truth is always the Markdown `String`/`TextFieldState`.
- **Live styling** uses a `VisualTransformation` that computes spans from an
  incremental re-parse of the changed region; markers are kept in the text.
- **Reveal-on-active-line** (`continuous-live-editor`) uses a cursor-aware
  `OffsetMapping` that collapses marker glyphs to zero width except on the caret's
  line — this is the trickiest piece and gets dedicated tests.
- **Inline block elements** in the live surface are interleaved composables
  (a `LazyColumn` of text-run editors and live block editors), not text
  attachments, so async/Canvas content stays live.
- Keyboard behaviors (Enter continue/end list, Tab indent) via `onKeyEvent` +
  `onPreviewKeyEvent`.

## Services & configuration

- Interfaces: `SyntaxHighlighter`, `LatexRenderer`, `WikiLinkResolver`,
  `EmbeddedImageProvider`, plus `VideoEmbedder`.
- `MarkdownServices` is a `data class` container with default implementations
  (plain monospaced highlighter, raw-source math, plain-text wiki-link, default
  image loader). Injected directly or via `LocalMarkdownServices`.
- `MarkdownConfiguration` `data class`: `interactiveCheckboxes`,
  `showCodeLineNumbers`, `codeCopyEnabled`, `enabledExtensions: Set<MarkdownExtension>`
  (`Math`, `Mermaid`, `Footnotes`, `WikiLinks`, …), `diagramSizing: DiagramSizing`
  (`Scroll`/`FitToWidth`), `readingWidth`.

## Mermaid design

- Custom Kotlin layout engine per diagram family; renders to Compose `Canvas`.
- CSS named-color table + hex parsing for inline styles; theme palette fallback.
- Unsupported/unlayoutable → highlighted code-block fallback.
- Sizing governed by `diagramSizing`; oversized diagrams pan/zoom via gesture
  modifiers.

## Video design

- Pure `classifyVideoUrl(url): VideoKind` (`DirectFile`/`Provider`/`NotVideo`) —
  fully unit-testable, no Android deps.
- Direct files → Media3 inline player. Providers → `VideoEmbedder` if injected,
  else `Intent.ACTION_VIEW`. Core references **no** `android.webkit.WebView`
  (enforced by a lint/import test).

## Distribution

- Published as AARs to Maven Central (or the org registry) with coordinates that
  parallel the iOS product names.

## Risks / open questions

- **Mermaid layout engine** is a large re-implementation; phase the 11 types
  (flowchart/pie/sequence/mindmap/gantt first, then class/state/ER/gitGraph/
  journey/timeline) — matches the iOS phased delivery in `visual-diagram-builders`.
- **Reveal-on-active-line** in Compose text: `OffsetMapping` zero-width collapse
  is non-trivial; prototype early.
- **Native math library** choice (JLaTeXMath vs. a Compose-native port) affects
  the LaTeX bridge; keep it behind `LatexRenderer` so it is swappable.
- **UTF-8 vs UTF-16 offsets**: commit to UTF-8 byte offsets to match iOS; add
  conversion helpers for Compose (UTF-16) selection APIs.
