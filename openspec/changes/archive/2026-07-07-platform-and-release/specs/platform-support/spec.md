# platform-support

## ADDED Requirements

### Requirement: Wear OS render-only subset
The library SHALL provide a `WearMarkdownView` that renders the core Markdown
subset with a constrained configuration and SHALL NOT expose the editor. Heavy
constructs (Mermaid, math) SHALL degrade to source rather than render.

#### Scenario: Wear view renders core subset
- **WHEN** `WearMarkdownView` renders a document with a Mermaid block
- **THEN** the core text SHALL render and the Mermaid block SHALL degrade to source

### Requirement: Publishable artifacts
The four library modules SHALL be publishable as Maven artifacts under the
`com.cyberdyne.markdown` group.

#### Scenario: Publish to Maven local
- **WHEN** `publishToMavenLocal` runs
- **THEN** each library module SHALL produce a release AAR with POM coordinates
