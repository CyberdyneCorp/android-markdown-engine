# code-syntax-highlighting Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Syntax-highlighted code blocks
`MarkdownView` SHALL render fenced code blocks with language-aware highlighting
via the injectable `SyntaxHighlighter` service, and SHALL render unhighlighted
monospaced text (using theme code colors) when no highlighter is configured.

#### Scenario: Highlight a known language
- **WHEN** a `SyntaxHighlighter` is configured and a fenced block declares `swift`
- **THEN** the block SHALL render with language-aware coloring

#### Scenario: No highlighter falls back to monospaced
- **WHEN** no highlighter is configured
- **THEN** the code SHALL render as plain monospaced text with theme code colors and no error

### Requirement: Language identification and aliases
Before invoking the highlighter, the library SHALL map common language aliases to
their canonical names, including `py`→`python`, `c++`→`cpp`, `sh`→`bash`, and
`ts`→`typescript`.

#### Scenario: Alias resolved to canonical language
- **WHEN** a fenced block declares `py`
- **THEN** the highlighter SHALL be invoked with the canonical language `python`

#### Scenario: Unknown language degrades gracefully
- **WHEN** a fenced block declares an unrecognized language
- **THEN** the block SHALL render as plain monospaced text with no error

### Requirement: Code block presentation
Code blocks SHALL render on a distinct surface background, preserve whitespace and
line breaks exactly, and scroll horizontally when lines exceed the width. When
enabled via `MarkdownConfiguration`, code blocks SHALL show line numbers and a
copy affordance that copies the verbatim code to the system clipboard.

#### Scenario: Long line scrolls horizontally
- **WHEN** a code line exceeds the available width
- **THEN** the code block SHALL scroll horizontally within its own container

#### Scenario: Line numbers and copy control
- **WHEN** `showCodeLineNumbers` and code copy are enabled
- **THEN** the block SHALL display line numbers and a copy control that places the verbatim code on the clipboard

### Requirement: Optional highlighter bridge module
The library SHALL provide an optional `markdown-engine-codeblocks` module that
implements `SyntaxHighlighter` using a native Kotlin/JVM highlighter with a
configurable code theme, so that core-only consumers pull in no highlighter and
no JavaScript runtime.

#### Scenario: Core-only build excludes the highlighter
- **WHEN** an app depends only on `markdown-engine`
- **THEN** no highlighter library or JavaScript runtime SHALL be present

#### Scenario: Bridge supplies configurable theme
- **WHEN** an app configures the bridge highlighter with a named code theme
- **THEN** highlighting SHALL use that theme

