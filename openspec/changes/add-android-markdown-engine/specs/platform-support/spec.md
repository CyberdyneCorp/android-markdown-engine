# platform-support

Mirrors iOS capability `platform-support`. iOS/macOS/watchOS map to Android
phone/tablet and an optional Wear OS constrained subset.

## ADDED Requirements

### Requirement: Supported platforms and minimums
The library SHALL target Android phone and tablet at a minimum of API 26
(Android 8.0), be written in Kotlin, and be distributed as Android library (AAR)
Gradle modules. An optional Wear OS target MAY provide a constrained render-only
subset (the watchOS analog).

#### Scenario: Builds as Android library modules
- **WHEN** the project is built
- **THEN** it SHALL produce AAR artifacts for the core and feature modules targeting API 26+

### Requirement: Rendering capability per platform
Full Markdown rendering (including code, tables, math, and Mermaid) SHALL be
available on phone and tablet; a Wear OS target SHALL render a constrained subset
suitable for a small screen (headings, paragraphs, lists, quotes, inline
formatting, links, code blocks).

#### Scenario: Constrained subset on Wear OS
- **WHEN** a document is rendered on the Wear OS target
- **THEN** the core subset SHALL render and heavy diagrams SHALL degrade to a simplified form or source substitution

### Requirement: Editor availability
`MarkdownEditor` SHALL be available on phone and tablet; the Wear OS target SHALL
be render-only and SHALL NOT expose the editor API.

#### Scenario: Editor absent on Wear OS
- **WHEN** the Wear OS artifact's API surface is inspected
- **THEN** `MarkdownEditor` SHALL NOT be present

### Requirement: Platform-idiomatic interaction
The library SHALL provide platform-appropriate interaction: touch and long-press
context menus on phones/tablets, hardware-keyboard shortcuts where a keyboard is
attached, and compact navigation on Wear OS.

#### Scenario: Link activation by tap
- **WHEN** the user taps a link on a phone or tablet
- **THEN** the configured link-open handler SHALL be invoked

### Requirement: Concurrency safety
The `MarkdownDocument` model and `MarkdownTheme` SHALL be immutable value types
safe to share across coroutines, and parsing SHALL be performable off the main
thread (e.g. on `Dispatchers.Default`) without data races.

#### Scenario: Parse off the main thread
- **WHEN** a large document is parsed on `Dispatchers.Default`
- **THEN** it SHALL complete without data races and the resulting immutable document SHALL be safe to hand to the UI thread
