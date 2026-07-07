# theming-customization Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Semantic theme tokens
The library SHALL provide a `MarkdownTheme` immutable value type of semantic
tokens covering background and surface colors, text colors (primary, secondary,
tertiary), accent colors, borders, and typography. It SHALL provide built-in
`MarkdownTheme.Light` and `MarkdownTheme.Dark` themes.

#### Scenario: Default theme adapts to appearance
- **WHEN** no explicit theme is supplied and the system is in dark mode
- **THEN** `MarkdownView` SHALL render with the dark token values

#### Scenario: Apply a custom theme
- **WHEN** an app passes a custom `MarkdownTheme` to `MarkdownView(theme = …)`
- **THEN** rendering SHALL use that theme's token values

### Requirement: Per-element styling
`MarkdownTheme` SHALL allow customization of heading type scale, paragraph
spacing, list indentation and markers, block-quote styling, code block and
inline-code colors, table borders and zebra striping, and link color.

#### Scenario: Customize code block background
- **WHEN** a theme overrides the code block surface color
- **THEN** rendered code blocks SHALL use that color

#### Scenario: Customize heading scale
- **WHEN** a theme overrides the heading type scale
- **THEN** headings SHALL render at the overridden sizes

### Requirement: Consistent theming across subsystems
Code highlighting, LaTeX math, and Mermaid diagrams SHALL each derive their
default colors from the active `MarkdownTheme`.

#### Scenario: Diagram inherits theme colors
- **WHEN** a Mermaid diagram has no explicit inline styles
- **THEN** it SHALL render using the active theme's palette and adapt to light/dark

### Requirement: Compose environment integration
`MarkdownTheme` SHALL be injectable through a `CompositionLocal`
(`LocalMarkdownTheme`) so nested `MarkdownView`/`MarkdownEditor` composables
inherit it without explicit passing.

#### Scenario: Inherit theme from environment
- **WHEN** a `MarkdownTheme` is provided via `LocalMarkdownTheme`
- **THEN** a nested `MarkdownView` with no explicit `theme` argument SHALL use the provided theme

