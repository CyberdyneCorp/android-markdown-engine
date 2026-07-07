# markdown-parsing Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: CommonMark block parsing
The parser SHALL parse all CommonMark block constructs into a typed, immutable
`MarkdownDocument` composed of `BlockNode` values: ATX and Setext headings
(levels 1–6), paragraphs, thematic breaks, indented and fenced code blocks,
block quotes, ordered and unordered lists (nested, loose and tight), and HTML
blocks. The parser MUST have no third-party runtime dependency.

#### Scenario: Fenced code block with language
- **WHEN** the source contains a fenced block ` ```swift ` with body lines
- **THEN** the parser SHALL emit a `BlockNode.CodeBlock` with `language = "swift"` and `content` equal to the verbatim body

#### Scenario: Nested list preserves ordinal start
- **WHEN** an ordered list starts at `3.` and contains a nested unordered list
- **THEN** the emitted `BlockNode.ListBlock` SHALL record the starting ordinal `3` and the nested list SHALL be a child of the correct item

#### Scenario: Tight vs loose list flagged
- **WHEN** a list has blank lines between items (loose) versus none (tight)
- **THEN** the emitted `BlockNode.ListBlock` SHALL expose a `tight: Boolean` flag reflecting which form was parsed

### Requirement: CommonMark inline parsing
The parser SHALL parse inline constructs into `InlineNode` values: emphasis,
strong, inline code spans, links (inline and reference), images, autolinks,
hard and soft line breaks, backslash escapes, and inline HTML.

#### Scenario: Emphasis and strong
- **WHEN** the source contains `*em*` and `**strong**`
- **THEN** the parser SHALL emit `InlineNode.Emphasis` and `InlineNode.Strong` nodes respectively

#### Scenario: Reference link resolves destination
- **WHEN** the source uses a reference link `[text][id]` with a matching `[id]: https://example.com` definition
- **THEN** the emitted `InlineNode.Link` SHALL carry `destination = "https://example.com"`

#### Scenario: Backslash escape yields literal
- **WHEN** the source contains `\*not emphasis\*`
- **THEN** the parser SHALL emit literal text `*not emphasis*` with no emphasis node

### Requirement: GitHub Flavored Markdown extensions
The parser SHALL parse GFM extensions: tables with per-column alignment, task
list items, strikethrough, and extended autolinks (bare URLs and emails).

#### Scenario: Table column alignment
- **WHEN** a table delimiter row is `|:---|:---:|---:|`
- **THEN** the emitted `BlockNode.Table` SHALL record column alignments left, center, right in order

#### Scenario: Task list item checked state
- **WHEN** a list item is `- [x] done`
- **THEN** the emitted task item SHALL have `checked = true`

#### Scenario: Strikethrough
- **WHEN** the source contains `~~deleted~~`
- **THEN** the parser SHALL emit an `InlineNode.Strikethrough` node

### Requirement: Math extension
The parser SHALL recognize LaTeX math and emit dedicated math nodes: inline math
delimited by `$…$` or `\(…\)` as `InlineNode.InlineMath`, and block math
delimited by `$$…$$` or `\[…\]` as `BlockNode.BlockMath`. Recognition MUST be
gated by the `math` extension being enabled in `MarkdownConfiguration`.

#### Scenario: Inline math body captured
- **WHEN** the source contains `text $E=mc^2$ text`
- **THEN** the parser SHALL emit an `InlineNode.InlineMath` with body `E=mc^2` surrounded by text nodes

#### Scenario: Currency is not math
- **WHEN** the source contains `it costs $5 and $7`
- **THEN** the parser SHALL NOT emit any math node and SHALL keep the text literal

#### Scenario: Math extension disabled
- **WHEN** the `math` extension is not present in `MarkdownConfiguration.enabledExtensions`
- **THEN** `$…$` SHALL be parsed as literal text

### Requirement: Mermaid extension
The parser SHALL parse a fenced code block whose info string is `mermaid` into a
`BlockNode.Mermaid` node carrying the verbatim diagram source, when the `mermaid`
extension is enabled.

#### Scenario: Mermaid fence becomes diagram node
- **WHEN** the source contains a ` ```mermaid ` fence with `flowchart TD` body
- **THEN** the parser SHALL emit a `BlockNode.Mermaid` (not a generic `CodeBlock`) with the source preserved verbatim

### Requirement: Additional document extensions
The parser SHALL parse YAML frontmatter, footnote definitions and references,
callout/admonition blocks, and wiki-style links, each gated by the corresponding
entry in `MarkdownConfiguration.enabledExtensions` where applicable.

#### Scenario: YAML frontmatter extracted as metadata
- **WHEN** the document begins with a `---` fenced YAML block
- **THEN** the parser SHALL expose the parsed key/value metadata on `MarkdownDocument` and exclude it from the body blocks

#### Scenario: Wiki-link target and display
- **WHEN** the source contains `[[Page Name|alias]]`
- **THEN** the parser SHALL emit an `InlineNode.WikiLink` with `target = "Page Name"` and `display = "alias"`

#### Scenario: Callout kind
- **WHEN** a block quote starts with `> [!NOTE]`
- **THEN** the parser SHALL emit a `BlockNode.Callout` with `kind = note` and the remaining content as children

#### Scenario: Footnote reference linked to definition
- **WHEN** the source contains a `[^1]` reference and a `[^1]:` definition
- **THEN** the parser SHALL link the reference node to its definition

### Requirement: Immutable model and source ranges
The `MarkdownDocument`, `BlockNode`, and `InlineNode` types SHALL be immutable
value types (Kotlin `data class`/`sealed` hierarchies) safe to share across
coroutines, and every node SHALL expose its source offset range as UTF-8 byte
offsets into the original source for editing and incremental updates.

#### Scenario: Source range exposed per node
- **WHEN** any node is produced by the parser
- **THEN** it SHALL expose a source range as UTF-8 byte offsets `[start, end)` into the original source

#### Scenario: Deterministic parse
- **WHEN** the same source string is parsed twice
- **THEN** the two resulting `MarkdownDocument` values SHALL be equal

### Requirement: Malformed input resilience
The parser SHALL NOT crash or throw on arbitrary input; it SHALL degrade
gracefully and emit literal text for unresolved constructs.

#### Scenario: Unterminated fence
- **WHEN** the source contains an unterminated ` ``` ` fence
- **THEN** the parser SHALL treat the remainder as code content and return a document without throwing

