# latex-math-rendering

Mirrors iOS capability `latex-math-rendering`.

## ADDED Requirements

### Requirement: Inline and block math rendering
`MarkdownView` SHALL render LaTeX math nodes natively (via Compose `Canvas`/text,
never a `WebView`) using the injectable `LatexRenderer` service. Inline math
SHALL flow within prose at the surrounding baseline and font size; block math
SHALL render centered, larger, and standalone on its own line.

#### Scenario: Inline math flows with text
- **WHEN** a paragraph contains inline math `E=mc^2`
- **THEN** it SHALL render inline at the surrounding font size and baseline

#### Scenario: Block math standalone
- **WHEN** a document contains a block math node
- **THEN** it SHALL render centered on its own line at a larger size

### Requirement: Math feature coverage
The math rendering SHALL support standard LaTeX math-mode constructs: fractions,
superscripts and subscripts, radicals, integrals and large operators, matrices,
Greek letters, and common operators and symbols.

#### Scenario: Fraction with superscript
- **WHEN** the math body is `\frac{x^2}{y}`
- **THEN** it SHALL render as a fraction whose numerator has a superscript

### Requirement: Invalid LaTeX fallback
Unparseable math SHALL render as its raw LaTeX source in monospaced text and MUST
NOT crash.

#### Scenario: Unbalanced braces
- **WHEN** the math body has unbalanced braces
- **THEN** the raw LaTeX source SHALL be shown as monospaced text without crashing

### Requirement: Theme-aware math color
Rendered math SHALL adopt the active `MarkdownTheme` text color and adapt to
light and dark appearance.

#### Scenario: Dark mode text color
- **WHEN** the app is in dark mode
- **THEN** rendered math SHALL use the theme's dark text color

### Requirement: Optional LaTeX bridge module
The library SHALL provide an optional `markdown-engine-latex` module implementing
`LatexRenderer` with a native Kotlin/JVM math typesetter, so the core module stays
free of the math dependency.

#### Scenario: Core-only build excludes math library
- **WHEN** an app depends only on `markdown-engine`
- **THEN** no LaTeX/math typesetting library SHALL be present

#### Scenario: Bridge renders natively
- **WHEN** an app adds the LaTeX bridge and configures it in `MarkdownServices`
- **THEN** math SHALL render via native typesetting with no WebView
