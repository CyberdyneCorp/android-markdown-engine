# wysiwyg-editor Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Block-based WYSIWYG editing surface
The library SHALL provide a block-based WYSIWYG editor that presents the document
as a stack of blocks, each rendered WYSIWYG via the Markdown renderer with no raw
Markdown syntax shown, while the underlying document remains Markdown source.

#### Scenario: Blocks render, not source
- **WHEN** a document is opened in the WYSIWYG editor
- **THEN** each block SHALL render visually and no raw Markdown syntax SHALL be shown

#### Scenario: Visual change updates Markdown
- **WHEN** the user makes a visual edit
- **THEN** the underlying Markdown source SHALL update accordingly

### Requirement: Block management
The WYSIWYG editor SHALL let the user insert a new block via a `+` menu, reorder
blocks, and delete a block, updating the underlying Markdown.

#### Scenario: Insert a block
- **WHEN** the user picks a block type from the `+` menu
- **THEN** an editable block of that type SHALL be inserted and reflected in the Markdown

#### Scenario: Reorder and delete
- **WHEN** the user reorders or deletes a block
- **THEN** the block order/content in the Markdown SHALL update to match

### Requirement: Text block visual formatting
The editor SHALL let the user edit text blocks (paragraph, heading, list item,
quote, task) inline with a toolbar offering bold, italic, strikethrough, inline
code, link, heading level, bullet/ordered list, and task checkbox.

#### Scenario: Bold wraps source
- **WHEN** the user applies Bold to selected text
- **THEN** the serialized Markdown SHALL wrap it in `**…**`

#### Scenario: Paragraph to heading
- **WHEN** the user changes a paragraph to H2
- **THEN** it SHALL serialize with a `## ` prefix

### Requirement: Visual table editor
The editor SHALL provide a grid table editor to add/remove rows and columns, edit
cells, and set per-column alignment, serializing to a GFM table.

#### Scenario: Edit table serializes to GFM
- **WHEN** the user adds a column, edits a cell, and sets its alignment
- **THEN** the block SHALL serialize to a GFM table with that structure and alignment

### Requirement: Visual code editor
The editor SHALL provide a code-block editor with a language picker and a live
syntax-highlighted preview.

#### Scenario: Pick language and edit
- **WHEN** the user selects a language and edits code
- **THEN** the preview SHALL be highlighted and the block SHALL serialize with that language info string

### Requirement: Image and video insertion
The editor SHALL let the user insert an image or video by URL (or via a picker)
with alt/caption.

#### Scenario: Insert image
- **WHEN** the user inserts an image with a URL and alt text
- **THEN** it SHALL serialize to `![alt](url)`

### Requirement: Visual math editor
The editor SHALL provide a LaTeX editor with a live rendered preview for math
blocks.

#### Scenario: Edit math with preview
- **WHEN** the user edits LaTeX in the math editor
- **THEN** a live preview SHALL render and the block SHALL serialize to `$$…$$`

### Requirement: Diagram and chart blocks
The editor SHALL render Mermaid blocks and allow editing their source with a live
preview.

#### Scenario: Edit Mermaid with live preview
- **WHEN** the user edits a Mermaid block's source
- **THEN** a live diagram preview SHALL update and the block SHALL serialize back to a `mermaid` fence

