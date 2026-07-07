# markdown-editor Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Native Markdown editor view
The library SHALL provide a `@Composable MarkdownEditor` that edits Markdown
source text with live visual styling, without converting the text to rich text.
The document text SHALL always contain the literal Markdown characters (e.g.
`**bold**`). It SHALL support programmatic binding: bound-text changes reflect in
the editor, and user edits update the binding.

#### Scenario: Literal Markdown preserved
- **WHEN** the user types `**bold**`
- **THEN** the document text SHALL contain the literal characters `**bold**` while displaying styled emphasis

#### Scenario: Two-way binding
- **WHEN** the bound text state changes programmatically
- **THEN** the editor SHALL reflect it, and user edits SHALL update the bound state

### Requirement: Live syntax styling
As the user types, `MarkdownEditor` SHALL apply syntax-aware styling — heading
scale, emphasis, strong, strikethrough, inline code, fenced code, links,
wiki-links, and math — while keeping the raw Markdown characters editable.

#### Scenario: Heading prefix stays editable
- **WHEN** a line begins with `## `
- **THEN** it SHALL receive H2 typography while the `## ` prefix remains present and editable

#### Scenario: Inline code styled with backticks retained
- **WHEN** the user types a backtick span
- **THEN** it SHALL render monospaced with a code background while the backticks remain in the text

### Requirement: Formatting commands and toolbar
`MarkdownEditor` SHALL provide formatting commands — bold, italic, strikethrough,
inline code, heading level, link, list, task item, block quote, code block —
invokable via a toolbar and via standard hardware-keyboard shortcuts, with toggle
semantics.

#### Scenario: Toggle bold off
- **WHEN** a selection already wrapped in `**…**` is bolded again
- **THEN** the `**` delimiters SHALL be removed

#### Scenario: Toggle list prefix
- **WHEN** the unordered-list command is applied to a line then applied again
- **THEN** the `- ` prefix SHALL be added and then removed

### Requirement: Interactive editing affordances
`MarkdownEditor` SHALL support tappable task checkboxes that toggle `[ ]`/`[x]`,
smart list continuation on Enter, Tab/Shift-Tab list indentation, and removal of
the marker when Enter is pressed on an empty list item.

#### Scenario: Checkbox toggles source
- **WHEN** the user taps the checkbox of a `- [ ]` task item
- **THEN** the underlying text SHALL become `- [x]`

#### Scenario: Empty item ends the list
- **WHEN** the user presses Enter on an empty list item
- **THEN** the marker SHALL be removed and the list ended

### Requirement: Wiki-link and image affordances
`MarkdownEditor` SHALL recognize wiki-links `[[…]]` and image embeds, resolving
display via `WikiLinkResolver` and `EmbeddedImageProvider`, and SHALL offer
wiki-link target completion when a resolver is provided.

#### Scenario: Wiki-link completion offered
- **WHEN** the user types `[[` and a `WikiLinkResolver` is configured
- **THEN** the editor SHALL present candidate targets

### Requirement: Spelling and grammar with suppression
`MarkdownEditor` SHALL support system spelling/grammar checking and SHALL suppress
it within code, math, and wiki-link spans.

#### Scenario: Suppress spellcheck in code
- **WHEN** the cursor is within an inline code span
- **THEN** spelling/grammar checking SHALL be suppressed for that span

### Requirement: Editor scrolling ergonomics
`MarkdownEditor` SHALL provide bottom overscroll so the caret is not pinned to the
bottom edge, and SHALL constrain body text to a readable column while allowing
wide content (tables, code) to break out.

#### Scenario: Bottom overscroll
- **WHEN** the caret is on the last line
- **THEN** the editor SHALL allow scrolling past it so the caret is not pinned to the bottom edge

### Requirement: Deferred stylus handwriting input
Standard touch and keyboard input SHALL be fully supported, while Android
stylus/handwriting (Scribble-equivalent) input is explicitly deferred and NOT
required for this capability's initial delivery.

#### Scenario: Keyboard and touch supported
- **WHEN** the user edits with a soft keyboard, hardware keyboard, or touch
- **THEN** all editing affordances SHALL work without requiring stylus handwriting support

### Requirement: Wiki-link completion context
The editor SHALL detect when the cursor sits within an unclosed `[[` wiki-link
and SHALL offer candidate targets from the configured `WikiLinkResolver`.

#### Scenario: Open wiki-link offers candidates
- **WHEN** the text is `see [[Pa` with the cursor at the end and a resolver is configured
- **THEN** the editor SHALL request completions for the prefix `Pa`

#### Scenario: Closed wiki-link offers nothing
- **WHEN** the cursor is after a completed `[[Page]]`
- **THEN** no completion context SHALL be detected

### Requirement: Spellcheck suppression regions
The editor SHALL suppress system spelling/autocorrect within inline code, fenced
code, math (`$…$`, `$$…$$`), and wiki-link (`[[…]]`) spans.

#### Scenario: Cursor in code span suppresses spellcheck
- **WHEN** the cursor is inside an inline code span
- **THEN** the computed suppressed regions SHALL contain the cursor position

### Requirement: Hardware-keyboard formatting shortcuts
The editor SHALL apply formatting on hardware-keyboard shortcuts: Ctrl/Cmd+B
(bold), Ctrl/Cmd+I (italic), and Ctrl/Cmd+K (link).

#### Scenario: Ctrl+B bolds the selection
- **WHEN** the user presses Ctrl+B with a selection
- **THEN** the selection SHALL be wrapped in `**…**`

