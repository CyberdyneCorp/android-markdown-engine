# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-07

First release of **android-markdown-engine**: a fully native Android/Kotlin
Jetpack Compose Markdown renderer and editor that mirrors the iOS
[swift-markdown-engine](https://github.com/CyberdyneCorp/swift-markdown-engine)
feature-for-feature — **no WebView, no JavaScript**. Spec-driven with OpenSpec
(17 living capability specs).

### Modules

Published under the `com.cyberdyne.markdown` Maven group.

| Module | Contents |
|--------|----------|
| `markdown-engine` | parser, immutable model, Compose renderer, Mermaid, theming, services, serialization, video |
| `markdown-editor` | source editor, live editor, WYSIWYG, toolbar, diagram builders |
| `markdown-engine-codeblocks` | `SyntaxHighlighter` bridge (no JS) |
| `markdown-engine-latex` | `LatexRenderer` bridge (native Canvas math) |
| `markdown-engine-wear` | `WearMarkdownView` render-only subset |

### Added

**Parsing & model**
- CommonMark + GFM parser with a dependency-free, immutable `MarkdownDocument`
  (UTF-8 source ranges); deterministic and crash-safe on arbitrary input.
- Gated extensions: math (with currency-false-positive guard), Mermaid,
  footnotes, YAML frontmatter, callouts, wiki-links.
- CommonMark-conformant emphasis (delimiter stack, rule of three), fenced-code
  indent handling, and GFM ragged-row tables.
- `toMarkdown()` serialization with round-trip fidelity.

**Rendering (`MarkdownView`)**
- Native Compose rendering of all block and inline kinds: headings, lists,
  task checkboxes (interactive), tables with per-column alignment, block quotes,
  callouts, code blocks (line numbers + copy), images, and footnotes.
- `MarkdownTheme` semantic tokens (light/dark) and injectable `MarkdownServices`,
  both provided via `CompositionLocal`.
- Host-overridable block renderers; links via `LinkAnnotation`; inline images
  and inline math via `InlineTextContent`.

**Mermaid diagrams**
- All 11 diagram types rendered natively on a Compose Canvas: flowchart, pie,
  sequence, class, state, ER, git graph, journey, timeline, mindmap, gantt.
- Inline color styles (`fill`/`stroke`, hex + CSS named colors) with theme
  fallback; `diagramSizing` (scroll / fit-to-width); source fallback for
  unsupported types.

**Code highlighting & LaTeX (bridges)**
- `RegexSyntaxHighlighter` with Atom One light/dark themes (no JS runtime).
- `NativeLatexRenderer`: fractions, super/subscripts, radicals, Greek and
  operators, matrices, and sized `\left…\right` delimiters — Canvas-drawn, no
  MathJax; raw-source fallback.

**Video**
- Pure URL classifier, Media3 inline player, linked-thumbnail embeds, and an
  injectable `VideoEmbedder` for provider videos. No WebView anywhere in core.

**Editors (`markdown-editor`)**
- `MarkdownEditor`: live-styled source editor with `MarkdownEditorController`,
  a customizable `MarkdownToolbarItem` toolbar, list continuation / Tab indent,
  and Ctrl/Cmd+B/I/K shortcuts.
- `MarkdownLiveEditor`: continuous surface with reveal-on-active-line marker
  hiding.
- `MarkdownWysiwygEditor`: block-stack editor with visual table and code
  editors; a visual flowchart builder.
- Wiki-link completion and spellcheck suppression in code/math/wiki spans.

**Platform & tooling**
- Android phone/tablet + an optional Wear OS render-only subset.
- Runnable sample app (Preview / Editor / Live / WYSIWYG tabs).
- CI (unit tests + assemble + `openspec validate`), Maven publishing, and a
  Paparazzi screenshot gallery for visual verification.

### Fixed
- Mermaid cycle-aware layer ordering (feedback edges no longer invert node
  placement) and tightened edge-label spacing.
