# custom-block-renderers

Mirrors iOS capability `custom-block-renderers`.

## ADDED Requirements

### Requirement: Host-overridable block rendering
`MarkdownView` SHALL let a host register a custom `@Composable` renderer for a
given block kind via a `blockRenderers` registry parameter mapping a `BlockKind`
to a `@Composable (BlockNode, MarkdownTheme) -> Unit`. Registered kinds SHALL use
the host renderer; all other kinds SHALL render with the built-in composables.

#### Scenario: Register a renderer for one kind
- **WHEN** a host registers a renderer for `BlockKind.Callout`
- **THEN** every callout SHALL render with the host composable, receiving the `BlockNode` and the resolved `MarkdownTheme`

#### Scenario: Other kinds stay built-in
- **WHEN** a host registers a renderer only for `BlockKind.CodeBlock`
- **THEN** headings, lists, and all other kinds SHALL still render with built-in composables

#### Scenario: Multiple overrides take effect
- **WHEN** a host registers renderers for several different block kinds
- **THEN** all of those overrides SHALL be applied simultaneously
