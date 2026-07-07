# Add Android Markdown Engine

## Why

The iOS/macOS `swift-markdown-engine` provides a fully native (no WebView, no
JavaScript) Markdown renderer and editor. There is no Android counterpart, so
teams shipping the same product on both platforms cannot reuse the feature set,
naming, or behavior. This change establishes a native Android/Kotlin library
that mirrors the iOS engine feature-for-feature and, where Kotlin idiom allows,
name-for-name — so a developer moving between the two SDKs finds the same
`MarkdownView`, `MarkdownConfiguration`, `MarkdownTheme`, `MarkdownServices`,
and service protocols.

## What Changes

- Introduce a Jetpack Compose Markdown **rendering** engine: `MarkdownView`
  composable rendering a parsed `MarkdownDocument` or a raw Markdown `String`
  with no WebView/JavaScript.
- Introduce a dependency-free **parser** producing an immutable, thread-safe
  `MarkdownDocument` (`BlockNode`/`InlineNode`) with preserved UTF-8 source
  ranges: CommonMark + GFM + math, Mermaid, footnotes, frontmatter, callouts,
  and wiki-links.
- Introduce **serialization** (`MarkdownDocument.toMarkdown()`,
  `BlockNode.toMarkdown()`, `InlineNode.toMarkdown()`) with round-trip fidelity.
- Introduce **theming** (`MarkdownTheme` semantic tokens, light/dark, injected
  via `CompositionLocal`) and **extensibility services** (`MarkdownServices`
  container + `SyntaxHighlighter`, `LatexRenderer`, `WikiLinkResolver`,
  `EmbeddedImageProvider` interfaces) with sensible defaults.
- Introduce a **`MarkdownConfiguration`** object (`interactiveCheckboxes`,
  `showCodeLineNumbers`, `enabledExtensions`, reading-width, code copy,
  `diagramSizing`).
- Introduce native **code syntax highlighting** (injectable), **LaTeX math**
  (injectable, Canvas-based), **Mermaid diagrams** (native Canvas layout engine,
  11 types), and **video embeds** (Media3/ExoPlayer inline players + tappable
  thumbnails, pure URL classifier).
- Introduce a **`MarkdownEditor`** composable (live-styled Markdown source
  editor), a **continuous live editor** surface, **inline-editable lists**, a
  **block-based WYSIWYG editor**, **custom block renderers**, a customizable
  **editor toolbar** (`MarkdownToolbarItem`, `MarkdownEditorController`), and
  **visual diagram builders**.
- Define **platform support**: Android phone/tablet full feature set, optional
  Wear OS render-only constrained subset, immutable model safe to parse off the
  main thread (`Dispatchers.Default`).
- Ship optional **bridge modules** (`markdown-engine-codeblocks`,
  `markdown-engine-latex`) so the core module stays free of heavy dependencies —
  mirroring the iOS Highlightr/SwiftMath bridge products.

No breaking changes: this is a greenfield library.

## Capabilities

Each Android capability maps 1:1 to an iOS `swift-markdown-engine` capability of
the same name, for clean cross-platform traceability.

### New Capabilities

| Android capability (new spec) | iOS capability it mirrors |
|---|---|
| `markdown-parsing` | `markdown-parsing` |
| `document-rendering` | `document-rendering` |
| `markdown-serialization` | `markdown-serialization` |
| `code-syntax-highlighting` | `code-syntax-highlighting` |
| `latex-math-rendering` | `latex-math-rendering` |
| `mermaid-diagrams` | `mermaid-diagrams` |
| `video-embeds` | `video-embeds` |
| `theming-customization` | `theming-customization` |
| `extensibility-services` | `extensibility-services` |
| `custom-block-renderers` | `custom-block-renderers` |
| `markdown-editor` | `markdown-editor` |
| `continuous-live-editor` | `continuous-live-editor` |
| `live-editor-inline-lists` | `live-editor-inline-lists` |
| `wysiwyg-editor` | `wysiwyg-editor` |
| `editor-toolbar-customization` | `editor-toolbar-customization` |
| `visual-diagram-builders` | `visual-diagram-builders` |
| `platform-support` | `platform-support` |

### Modified Capabilities

None — no existing specs in `openspec/specs/`.

## Impact

- **New Gradle modules**: `markdown-engine` (core), `markdown-editor`,
  `markdown-engine-codeblocks`, `markdown-engine-latex`, plus a `sample` app.
- **New public API surface** (Kotlin): `MarkdownView`, `MarkdownEditor`,
  `MarkdownDocument`, `BlockNode`, `InlineNode`, `MarkdownConfiguration`,
  `MarkdownTheme`, `MarkdownServices`, `MarkdownEditorController`,
  `MarkdownToolbarItem`, and the four service interfaces + `VideoEmbedder`.
- **Dependencies**: core module has zero third-party runtime dependencies and
  never imports `WebView`/JavaScript; bridge modules add their highlighter/math
  libraries; the editor/video features add Compose text + Media3.
- **Distribution**: published as AARs (Maven coordinates) mirroring the iOS
  Swift Package products.
- **Non-functional**: immutable `Sendable`-equivalent model, parsing off the
  main thread, TalkBack accessibility, Dynamic Type / font-scale support.

## Non-goals

- **Not** re-implementing Apple Pencil / Scribble handwriting input as a
  blocking requirement; Android stylus/handwriting parity is explicitly deferred
  to a later change (the iOS `markdown-editor` Pencil requirement is out of scope
  for v1 and captured as a deferred note).
- **Not** shipping WebView-backed provider (YouTube/Vimeo) inline players in any
  module; provider embedders remain host-app responsibilities via the injectable
  `VideoEmbedder`, exactly as on iOS.
- **Not** achieving byte-identical Markdown output vs the iOS serializer; the
  requirement is semantic round-trip fidelity, not textual equality.
- **Not** targeting a Kotlin Multiplatform shared core in this change; this is an
  Android/JVM library. (KMP may be revisited later.)
- **Not** implementing the sample/demo application's product features (e.g. the
  iOS PencilNotes app) beyond what is needed to exercise the library.
