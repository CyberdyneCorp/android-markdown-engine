# markdown-serialization

Mirrors iOS capability `markdown-serialization`.

## ADDED Requirements

### Requirement: Document-to-Markdown serialization
The library SHALL serialize a parsed `MarkdownDocument` back to Markdown via
`MarkdownDocument.toMarkdown()`, covering every block kind the parser produces:
headings, paragraphs, ordered/unordered/task lists, block quotes, thematic
breaks, code blocks, math blocks, tables, images, and raw/passthrough blocks such
as Mermaid.

#### Scenario: Serialized output is valid Markdown
- **WHEN** `MarkdownDocument.toMarkdown()` is called on a parsed document
- **THEN** the output SHALL be valid Markdown that re-parses to an equivalent model

#### Scenario: Inline delimiters reproduced
- **WHEN** a document contains bold, italic, strikethrough, inline code, and a link
- **THEN** the serialized output SHALL reproduce each with the correct Markdown delimiters

### Requirement: Round-trip fidelity
Parsing then serializing then parsing (`parse → toMarkdown → parse`) SHALL
preserve document structure so that visual edits persist without structural loss.

#### Scenario: Representative document round-trips
- **WHEN** a document containing headings, emphasis, lists, a task list, a table, a fenced code block, a math block, and an image is parsed, serialized, and re-parsed
- **THEN** the re-parsed model SHALL have block and inline structure equal to the original

### Requirement: Single-block serialization
The library SHALL serialize an individual block to its Markdown fragment via
`BlockNode.toMarkdown()` (and `InlineNode.toMarkdown()` for inline fragments), so
a WYSIWYG editor can regenerate just the edited block.

#### Scenario: Single edited table block
- **WHEN** `BlockNode.toMarkdown()` is called on an edited table block
- **THEN** the output SHALL be the Markdown for just that table
