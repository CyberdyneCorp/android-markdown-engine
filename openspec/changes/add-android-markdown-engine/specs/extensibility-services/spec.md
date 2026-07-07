# extensibility-services

Mirrors iOS capability `extensibility-services`.

## ADDED Requirements

### Requirement: Zero-dependency core
The core module (`markdown-engine`) SHALL have no third-party runtime
dependencies and SHALL NOT import `WebView` or any JavaScript runtime. Heavy
integrations (syntax highlighting, LaTeX) SHALL be delivered as optional bridge
modules built on injectable service interfaces.

#### Scenario: Core builds without third-party deps
- **WHEN** an app depends only on `markdown-engine`
- **THEN** the build SHALL pull in no third-party or JavaScript dependencies

#### Scenario: Adding only the code bridge does not add math
- **WHEN** an app adds `markdown-engine-codeblocks`
- **THEN** the LaTeX bridge and its math library SHALL NOT be added to the graph

### Requirement: Service interfaces
The library SHALL define four service interfaces — `SyntaxHighlighter`,
`LatexRenderer`, `WikiLinkResolver`, and `EmbeddedImageProvider` — and SHALL
operate with sensible defaults when a service is not supplied.

#### Scenario: Defaults without services
- **WHEN** no services are supplied
- **THEN** code blocks SHALL render as plain monospaced text, math SHALL render as raw source, wiki-links SHALL render as plain text, and images SHALL load via the built-in default loader

#### Scenario: Injected highlighter routes all highlighting
- **WHEN** an app injects a custom `SyntaxHighlighter`
- **THEN** all fenced code highlighting SHALL be routed through it

### Requirement: Service container injection
The library SHALL provide a `MarkdownServices` container bundling the configured
services, suppliable to both `MarkdownView` and `MarkdownEditor` directly and via
a `CompositionLocal` (`LocalMarkdownServices`).

#### Scenario: Supply services to the view
- **WHEN** an app builds `MarkdownServices(syntaxHighlighter = …, latexRenderer = …)` and passes it to `MarkdownView`
- **THEN** rendering SHALL use those services

### Requirement: Optional bridge modules
The library SHALL ship optional modules implementing the service interfaces: a
code-highlighting bridge (`markdown-engine-codeblocks`) and a LaTeX bridge
(`markdown-engine-latex`).

#### Scenario: Bridge provides highlighting
- **WHEN** an app adds the code-highlighting bridge and configures its highlighter in `MarkdownServices`
- **THEN** fenced code SHALL be highlighted without the core module depending on the bridge

### Requirement: Configuration object
The library SHALL provide a `MarkdownConfiguration` controlling feature toggles
(`enabledExtensions`), interactivity (`interactiveCheckboxes`, link handling),
code-block options (`showCodeLineNumbers`, code copy), diagram sizing
(`diagramSizing`), and reading-width constraints.

#### Scenario: Enabled extensions gate parsing
- **WHEN** `MarkdownConfiguration.enabledExtensions` does not contain `Math`
- **THEN** inline `$…$` SHALL render as literal text

#### Scenario: Enabling editable checkboxes
- **WHEN** `MarkdownConfiguration.interactiveCheckboxes` is true
- **THEN** task items SHALL render as toggleable controls
