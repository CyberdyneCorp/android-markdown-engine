# Tasks — Android Markdown Engine

Tasks are grouped by phase. Each capability's tasks name the spec they satisfy.
Ship each phase with tests (JUnit + Compose UI/Robolectric) and keep the core
module WebView-free and dependency-free.

## 1. Project scaffolding
- [x] 1.1 Create Gradle (Kotlin DSL) multi-module project (core module live; editor/bridge/sample scaffolded in settings, enabled progressively)
- [x] 1.2 Configure Android library plugins, `minSdk 26`, `compileSdk 34`, JVM target 17, Compose enabled; Gradle wrapper pinned to 8.9, version catalog
- [x] 1.3 Add test rule forbidding `android.webkit.WebView` in the core module (`InvariantsTest`)
- [ ] 1.4 Set up CI: build + unit tests + Compose tests + `openspec validate --all --strict`
- [ ] 1.5 Port the iOS `Fixtures` corpus as shared test resources

## 2. Model & parser (markdown-parsing, platform-support)
- [x] 2.1 Define immutable `MarkdownDocument`, `BlockNode`, `InlineNode` sealed hierarchies with `SourceRange` (UTF-8 byte offsets via `Utf8Offsets`) and `BlockKind`
- [x] 2.2 Implement CommonMark block parser (headings ATX+setext, thematic breaks, indented/fenced code, block quotes, lists tight/loose/nested, HTML blocks)
- [x] 2.3 Implement CommonMark inline parser (emphasis/strong/strike via delimiter stack, code spans, links inline+reference, images, autolinks, breaks, backslash escapes, inline HTML)
- [x] 2.4 Implement GFM extensions (tables + alignment, task items, strikethrough, extended autolinks)
- [x] 2.5 Implement math extension with currency false-positive guard; gate on `enabledExtensions`
- [x] 2.6 Implement mermaid fence, YAML frontmatter, footnotes, callouts, wiki-links extensions (each gated)
- [x] 2.7 Guarantee malformed-input resilience (never throw) and deterministic parse
- [x] 2.8 Ensure model is thread-safe; expose parse on `Dispatchers.Default` (`Markdown.parseAsync`)
- [x] 2.9 Tests: parsing scenarios, source-range validity, determinism, resilience (22 tests)
- [ ] 2.10 Broaden to the full CommonMark/GFM spec fixture corpus (edge-case conformance)

## 3. Serialization (markdown-serialization)
- [x] 3.1 Implement `MarkdownDocument.toMarkdown()` covering every block kind
- [x] 3.2 Implement `BlockNode.toMarkdown()` / `InlineNode.toMarkdown()`
- [x] 3.3 Tests: round-trip fidelity (`parse → toMarkdown → parse` equality), single-block serialization

## 4. Theming & services & configuration (theming-customization, extensibility-services)
- [x] 4.1 Define `MarkdownTheme` token `data class` + `Light`/`Dark` built-ins + `LocalMarkdownTheme`
- [x] 4.2 Define `MarkdownConfiguration` (`interactiveCheckboxes`, `showCodeLineNumbers`, `codeCopyEnabled`, `enabledExtensions`, `diagramSizing`, `readingWidthDp`)
- [x] 4.3 Define service interfaces + defaults: `SyntaxHighlighter`, `LatexRenderer`, `WikiLinkResolver`, `EmbeddedImageProvider`, `VideoEmbedder`
- [x] 4.4 Define `MarkdownServices` container + `LocalMarkdownServices`
- [x] 4.5 Tests: defaults present; extension gating covered in parser tests (injection-routing UI tests in androidTest)

## 5. Rendering (document-rendering, custom-block-renderers)
- [x] 5.1 Implement `@Composable MarkdownView(String | MarkdownDocument, theme, configuration, services, onLinkClick, onCheckboxToggle, blockRenderers)`
- [x] 5.2 Block rendering (headings, paragraphs, quotes, callouts, thematic breaks, lists/tasks nested, tables + alignment, html, footnote defs)
- [x] 5.3 Inline rendering via `AnnotatedString` (emphasis, strong, strikethrough, code, links, wiki-links, footnote refs, breaks, inline math raw)
- [x] 5.4 Interactive checkboxes emitting change with source range + checked state
- [x] 5.5 Image loading via `EmbeddedImageProvider` (placeholder default) + alt as `contentDescription`
- [x] 5.6 Wide-content per-block horizontal overflow containers (code, tables)
- [x] 5.7 Accessibility: heading semantics; font-scale via Compose sp
- [x] 5.8 `blockRenderers` registry for host overrides (`BlockKind -> @Composable (BlockNode, MarkdownTheme)`)
- [x] 5.9 Compose UI tests (androidTest): heading/paragraph render, checkbox toggle, link click
- [ ] 5.10 Inline images/math as `InlineTextContent`; migrate links off deprecated `ClickableText` to `LinkAnnotation`

## 6. Code highlighting (code-syntax-highlighting)
- [x] 6.1 Code block presentation (surface, whitespace-preserving, horizontal scroll, optional line numbers + copy-to-clipboard)
- [x] 6.2 Language alias normalization (`py`→`python`, `c++`→`cpp`, `sh`→`bash`, `ts`→`typescript`)
- [ ] 6.3 `markdown-engine-codeblocks` bridge implementing `SyntaxHighlighter` with configurable theme
- [x] 6.4 Tests: alias resolution + plain fallback (highlighter routing in androidTest); bridge exclusion pending module

## 7. LaTeX math (latex-math-rendering)
- [ ] 7.1 Inline/block math rendering via `LatexRenderer` on Compose Canvas/text (no WebView)
- [ ] 7.2 Feature coverage (fractions, sub/superscripts, radicals, operators, matrices, Greek)
- [ ] 7.3 Invalid-LaTeX raw-source fallback; theme-aware color
- [ ] 7.4 `markdown-engine-latex` bridge implementing `LatexRenderer`
- [ ] 7.5 Tests: inline flow, block layout, fallback, theming, core excludes bridge

## 8. Mermaid diagrams (mermaid-diagrams)
- [x] 8.1 Canvas rendering scaffold + `diagramSizing` (`Scroll`/`FitToWidth`) sizing
- [x] 8.2 Layout engine: flowchart (shapes, styled edges, longest-path layering), pie, sequence, mindmap, gantt
- [x] 8.3 Layout engine: class, state, ER, git graph, journey, timeline (all 11 types render natively)
- [ ] 8.4 Inline styles (hex + CSS named colors) with theme-palette fallback
- [x] 8.5 Unsupported/unlayoutable fallback to source code block
- [x] 8.6 Tests: `MermaidParser` (14 unit tests, all 11 types); native-vs-fallback Compose tests (androidTest)

## 9. Video embeds (video-embeds)
- [x] 9.1 Pure `VideoUrls.classify(url): VideoKind` (`DIRECT_FILE`/`PROVIDER`/`NOT_VIDEO`)
- [x] 9.2 Linked-thumbnail embed with play overlay + alt label
- [x] 9.3 Direct-file inline Media3 player; provider via `VideoEmbedder` or platform URI handler
- [x] 9.4 Core references no `WebView` (enforced by `InvariantsTest`)
- [x] 9.5 Tests: classifier table (extensions/hosts/subdomains); no-WebView assertion (external-open + failure fallback UI tests pending device run)

## 10. Markdown editor (markdown-editor, editor-toolbar-customization)
- [x] 10.1 `@Composable MarkdownEditor(state, toolbar, showsToolbar)` on `BasicTextField`, literal Markdown preserved (`markdown-editor` module)
- [x] 10.2 Live syntax styling via `MarkdownStyler` `VisualTransformation` (headings, bold, italic, strikethrough, inline & fenced code)
- [x] 10.3 Formatting commands with toggle semantics; Tab/Enter hardware-key handling
- [x] 10.4 Interactive affordances: list continuation (Enter), Tab/Shift-Tab indent, empty-item end, `toggleTask()` controller command
- [ ] 10.5 Wiki-link + image affordances; wiki-link completion when resolver present
- [ ] 10.6 Spellcheck with suppression in code/math/wiki-link spans
- [ ] 10.7 Bottom overscroll + readable-column with wide-content breakout
- [x] 10.8 `MarkdownToolbarItem` sealed catalog + `MarkdownEditorController` (`toggleBold()`, `toggleItalic()`, …); default/custom/hidden toolbar
- [x] 10.9 Tests: 12 `EditOps` unit tests (wrap/toggle/list/indent/task/link/heading); Compose UI tests (default/hidden/custom toolbar, bold command)

## 11. Live & WYSIWYG editors (continuous-live-editor, live-editor-inline-lists, wysiwyg-editor, visual-diagram-builders)
- [ ] 11.1 Continuous live scrolling surface reconstructing shared Markdown
- [ ] 11.2 Reveal-on-active-line marker hiding via cursor-aware `OffsetMapping`
- [ ] 11.3 Inline live block composables + per-type tap-to-edit; Live toolbar with full Insert menu
- [ ] 11.4 Inline-editable flat lists (bulleted/ordered/task) with Enter/Tab behaviors + checkbox toggle; nested/multi-block stay tap-to-edit
- [ ] 11.5 Block-based WYSIWYG surface: block stack, `+` insert, reorder, delete
- [ ] 11.6 WYSIWYG per-type editors: text/heading/list/quote/task, table grid, code + language picker, image/video insert, math preview, Mermaid source+preview
- [ ] 11.7 Visual diagram builders: flowchart, pie, sequence, mindmap, gantt (phase 1) then class/state/ER/gitGraph/journey/timeline; source-editor fallback for unbuilt types
- [ ] 11.8 Tests: reveal-on-active-line, list keyboard behavior, block management, serialization from builders

## 12. Platform, docs & release (platform-support)
- [ ] 12.1 Optional Wear OS render-only subset (no editor); heavy diagrams degrade
- [ ] 12.2 Platform-idiomatic interaction (touch/long-press menus, keyboard shortcuts)
- [ ] 12.3 API docs mirroring iOS names; README with quick-start parity examples
- [ ] 12.4 Publish AARs (Maven coordinates paralleling iOS products)
- [ ] 12.5 `openspec validate --all --strict` green in CI

## 13. Finalize
- [ ] 13.1 Full regression suite green; sample app exercises every capability
- [ ] 13.2 Archive this change (`openspec archive add-android-markdown-engine`) once implemented and merged
