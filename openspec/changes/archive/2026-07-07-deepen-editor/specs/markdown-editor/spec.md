# markdown-editor

## ADDED Requirements

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
