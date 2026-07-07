# Tasks — Android Markdown Engine

Tasks are grouped by phase. Each capability's tasks name the spec they satisfy.
Ship each phase with tests (JUnit + Compose UI/Robolectric) and keep the core
module WebView-free and dependency-free.

## 1. Project scaffolding
- [ ] 1.1 Create Gradle (Kotlin DSL) multi-module project: `markdown-engine`, `markdown-editor`, `markdown-engine-codeblocks`, `markdown-engine-latex`, `sample`
- [ ] 1.2 Configure Android library plugins, `minSdk 26`, `compileSdk` latest, JVM target 17, Compose enabled
- [ ] 1.3 Add lint/test rule forbidding `android.webkit.WebView` import in the core module (platform-support, video-embeds, extensibility-services)
- [ ] 1.4 Set up CI: build + unit tests + Compose tests + `openspec validate --all --strict`
- [ ] 1.5 Port the iOS `Fixtures` corpus as shared test resources

## 2. Model & parser (markdown-parsing, platform-support)
- [ ] 2.1 Define immutable `MarkdownDocument`, `BlockNode`, `InlineNode` sealed hierarchies with `SourceRange` (UTF-8 byte offsets) and `BlockKind`
- [ ] 2.2 Implement CommonMark block parser (headings, paragraphs, thematic breaks, indented/fenced code, block quotes, lists tight/loose/nested, HTML blocks)
- [ ] 2.3 Implement CommonMark inline parser (emphasis, strong, code spans, links inline+reference, images, autolinks, breaks, backslash escapes, inline HTML)
- [ ] 2.4 Implement GFM extensions (tables + alignment, task items, strikethrough, extended autolinks)
- [ ] 2.5 Implement math extension with currency false-positive guard; gate on `enabledExtensions`
- [ ] 2.6 Implement mermaid fence, YAML frontmatter, footnotes, callouts, wiki-links extensions (each gated)
- [ ] 2.7 Guarantee malformed-input resilience (never throw) and deterministic parse
- [ ] 2.8 Ensure model is thread-safe; expose parse on `Dispatchers.Default`
- [ ] 2.9 Tests: CommonMark/GFM fixtures, source-range assertions, determinism, resilience

## 3. Serialization (markdown-serialization)
- [ ] 3.1 Implement `MarkdownDocument.toMarkdown()` covering every block kind
- [ ] 3.2 Implement `BlockNode.toMarkdown()` / `InlineNode.toMarkdown()`
- [ ] 3.3 Tests: round-trip fidelity (`parse → toMarkdown → parse` equality), single-block serialization

## 4. Theming & services & configuration (theming-customization, extensibility-services)
- [ ] 4.1 Define `MarkdownTheme` token `data class` + `Light`/`Dark` built-ins + `LocalMarkdownTheme`
- [ ] 4.2 Define `MarkdownConfiguration` (`interactiveCheckboxes`, `showCodeLineNumbers`, `codeCopyEnabled`, `enabledExtensions`, `diagramSizing`, `readingWidth`)
- [ ] 4.3 Define service interfaces + defaults: `SyntaxHighlighter`, `LatexRenderer`, `WikiLinkResolver`, `EmbeddedImageProvider`, `VideoEmbedder`
- [ ] 4.4 Define `MarkdownServices` container + `LocalMarkdownServices`
- [ ] 4.5 Tests: defaults-without-services behavior, injection routing, extension gating

## 5. Rendering (document-rendering, custom-block-renderers)
- [ ] 5.1 Implement `@Composable MarkdownView(String | MarkdownDocument, theme, configuration, services, onLinkClick, blockRenderers)`
- [ ] 5.2 Block rendering (headings, paragraphs, quotes, thematic breaks, lists/tasks nested, tables + alignment)
- [ ] 5.3 Inline rendering via `AnnotatedString` + `InlineTextContent` (emphasis, strong, strikethrough, code, links, images, wiki-links, footnote refs, breaks)
- [ ] 5.4 Interactive checkboxes emitting change with source range + checked state
- [ ] 5.5 Image loading via `EmbeddedImageProvider` with placeholder/failure + alt as `contentDescription`
- [ ] 5.6 Wide-content per-block horizontal overflow containers
- [ ] 5.7 Accessibility: heading semantics/levels, focusable links, font-scale
- [ ] 5.8 `blockRenderers` registry for host overrides (`BlockKind -> @Composable (BlockNode, MarkdownTheme)`)
- [ ] 5.9 Tests: rendering snapshots, checkbox change events, overflow, a11y semantics, override registry

## 6. Code highlighting (code-syntax-highlighting)
- [ ] 6.1 Code block presentation (surface, whitespace-preserving, horizontal scroll, optional line numbers + copy-to-clipboard)
- [ ] 6.2 Language alias normalization (`py`→`python`, `c++`→`cpp`, `sh`→`bash`, `ts`→`typescript`)
- [ ] 6.3 `markdown-engine-codeblocks` bridge implementing `SyntaxHighlighter` with configurable theme
- [ ] 6.4 Tests: highlighter routing, alias resolution, no-highlighter fallback, core excludes bridge

## 7. LaTeX math (latex-math-rendering)
- [ ] 7.1 Inline/block math rendering via `LatexRenderer` on Compose Canvas/text (no WebView)
- [ ] 7.2 Feature coverage (fractions, sub/superscripts, radicals, operators, matrices, Greek)
- [ ] 7.3 Invalid-LaTeX raw-source fallback; theme-aware color
- [ ] 7.4 `markdown-engine-latex` bridge implementing `LatexRenderer`
- [ ] 7.5 Tests: inline flow, block layout, fallback, theming, core excludes bridge

## 8. Mermaid diagrams (mermaid-diagrams)
- [ ] 8.1 Canvas rendering scaffold + `diagramSizing` (`Scroll`/`FitToWidth`) with pan/zoom
- [ ] 8.2 Layout engine: flowchart (shapes, edges, subgraphs, self-loops), pie, sequence, mindmap, gantt
- [ ] 8.3 Layout engine: class, state, ER, git graph, journey, timeline (remaining 6)
- [ ] 8.4 Inline styles (hex + CSS named colors) with theme-palette fallback
- [ ] 8.5 Unsupported/unlayoutable fallback to highlighted code block
- [ ] 8.6 Tests: per-type rendering, style parsing, fallback

## 9. Video embeds (video-embeds)
- [ ] 9.1 Pure `classifyVideoUrl(url): VideoKind` (`DirectFile`/`Provider`/`NotVideo`)
- [ ] 9.2 Linked-thumbnail embed with play overlay + alt label
- [ ] 9.3 Direct-file inline Media3 player; provider via `VideoEmbedder` or `Intent.ACTION_VIEW`
- [ ] 9.4 Graceful fallback on playback failure; core references no `WebView`
- [ ] 9.5 Tests: classifier table (extensions/hosts/subdomains), external-open, no-WebView assertion

## 10. Markdown editor (markdown-editor, editor-toolbar-customization)
- [ ] 10.1 `@Composable MarkdownEditor(state, toolbar, showsToolbar, services)` on `BasicTextField`/`TextFieldState`, literal Markdown preserved
- [ ] 10.2 Live syntax styling via `VisualTransformation` (headings, emphasis, code, links, wiki-links, math)
- [ ] 10.3 Formatting commands with toggle semantics + hardware-keyboard shortcuts
- [ ] 10.4 Interactive affordances: checkbox toggle, list continuation, Tab/Shift-Tab indent, empty-item end
- [ ] 10.5 Wiki-link + image affordances; wiki-link completion when resolver present
- [ ] 10.6 Spellcheck with suppression in code/math/wiki-link spans
- [ ] 10.7 Bottom overscroll + readable-column with wide-content breakout
- [ ] 10.8 `MarkdownToolbarItem` sealed catalog + `MarkdownEditorController` (`toggleBold()`, `toggleItalic()`, …); default/custom/hidden toolbar
- [ ] 10.9 Tests: literal-text preservation, styling, toggle commands, list behaviors, toolbar items, controller commands

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
