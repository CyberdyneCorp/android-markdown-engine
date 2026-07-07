# mermaid-diagrams

## ADDED Requirements

### Requirement: Inline color styling
The Mermaid renderer SHALL parse inline node style directives and apply their
fill, stroke, and text colors, accepting `#RGB`, `#RRGGBB`, and CSS named-color
forms, and SHALL fall back to the active theme palette when no explicit style is
given.

#### Scenario: Hex fill applied
- **WHEN** a flowchart contains `style A fill:#ff0000`
- **THEN** node `A` SHALL render with a red fill

#### Scenario: CSS named color resolved
- **WHEN** a style uses `stroke:tomato`
- **THEN** the renderer SHALL resolve it to the corresponding RGB color
