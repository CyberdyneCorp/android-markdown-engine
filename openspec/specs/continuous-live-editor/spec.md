# continuous-live-editor Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Continuous live editing surface
The library SHALL provide a single continuous scrolling "Live" editing surface
where Markdown is edited in place, inline formatting is rendered live, and block
elements are rendered inline (not raw source, not discrete cards). Editing SHALL
reconstruct the shared Markdown source, which remains the source of truth.

#### Scenario: One continuous rendered surface
- **WHEN** the editor is in Live mode
- **THEN** it SHALL present one continuous rendered surface whose edits update the shared Markdown source

### Requirement: Reveal-on-active-line inline styling
The Live surface SHALL render inline formatting (headings, bold, italic,
strikethrough, inline code) live and hide the syntax markers by collapsing them to
zero width off the active line, revealing the full source only on the line
containing the cursor.

#### Scenario: Markers hidden off active line
- **WHEN** a line contains `**bold**` and the cursor is on a different line
- **THEN** it SHALL show the rendered word "bold" with the `**` markers hidden

#### Scenario: Markers revealed on active line
- **WHEN** the cursor moves onto that line
- **THEN** the `**` markers SHALL be revealed and editable

#### Scenario: Heading renders on type
- **WHEN** the user types `## ` at the start of a line
- **THEN** the line SHALL immediately render as a heading

### Requirement: Inline-rendered block elements
The Live surface SHALL render block elements (lists/checklists, fenced code, math,
Mermaid, tables, images, video) inline as live Compose composables (not static
snapshots), so async and Canvas/LaTeX content renders, and SHALL open a per-type
visual editor when a block is tapped, applying changes back to the Markdown.

#### Scenario: Rich blocks render inline
- **WHEN** the document contains a checklist, a LaTeX block, a Mermaid diagram, an image, and a video
- **THEN** each SHALL render inline as a live composable

#### Scenario: Tap opens per-type editor
- **WHEN** the user taps an inline block
- **THEN** its per-type visual editor SHALL open and changes SHALL apply back to the Markdown source

### Requirement: Live editor toolbar
The Live editor SHALL provide a toolbar with a heading menu, inline formatting
(bold, italic, strikethrough, inline code, link), and a complete Insert menu:
bulleted/numbered/checklist, quote, table, code, math, image, video, and all
Mermaid diagram types via a Diagram submenu.

#### Scenario: Insert menu offers every block and diagram type
- **WHEN** the user opens the Insert menu
- **THEN** it SHALL offer every supported block type and every Mermaid diagram type

