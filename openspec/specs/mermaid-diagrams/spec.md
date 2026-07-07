# mermaid-diagrams Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Native Mermaid rendering
`MarkdownView` SHALL render `BlockNode.Mermaid` nodes natively using a Compose
`Canvas` and a built-in layout engine. It MUST NOT use a `WebView` or JavaScript.

#### Scenario: Mermaid node renders on Canvas
- **WHEN** a document contains a `mermaid` block with a valid `flowchart`
- **THEN** it SHALL render as a native diagram drawn on a Compose `Canvas` with no WebView

### Requirement: Supported diagram types
The Mermaid renderer SHALL support 11 diagram types: flowchart, state diagram,
sequence diagram, class diagram, entity-relationship diagram, pie chart, gantt
chart, git graph, user journey, mindmap, and timeline.

#### Scenario: Each supported type renders
- **WHEN** a diagram of any of the 11 supported types is provided with valid syntax
- **THEN** it SHALL render natively rather than falling back to source

### Requirement: Flowchart shapes and edges
The flowchart renderer SHALL support node shapes (rectangle, rounded, stadium,
circle, diamond, hexagon, parallelogram, trapezoid, subroutine), edge variants
(solid, dashed, thick, with arrowheads and labels), subgraph grouping, and
self-loops.

#### Scenario: Shapes and labeled edges
- **WHEN** a flowchart declares a diamond node and a labeled dashed edge
- **THEN** both the shape and the labeled dashed edge SHALL render accordingly

### Requirement: Diagram styling and theming
The renderer SHALL honor inline style directives (fill, stroke, and text colors
in `#RGB`, `#RRGGBB`, or CSS named-color form) and SHALL fall back to the active
`MarkdownTheme` palette when no explicit style is present, adapting to light and
dark.

#### Scenario: Inline style honored
- **WHEN** a node declares an inline `fill:#ff0000` style
- **THEN** it SHALL render with that fill color

#### Scenario: CSS named color parsed
- **WHEN** a style uses a CSS named color such as `tomato`
- **THEN** the renderer SHALL resolve it to the correct color

### Requirement: Unsupported diagram fallback
When a diagram cannot be rendered, the renderer SHALL fall back to rendering the
diagram source as a highlighted code block rather than failing.

#### Scenario: Unrenderable diagram falls back to source
- **WHEN** a Mermaid block has syntax the renderer cannot lay out
- **THEN** it SHALL render the source as a code block without crashing

### Requirement: Sizing and overflow
A diagram SHALL fit the available width when possible and SHALL become scrollable
or zoomable when its intrinsic size exceeds the viewport, governed by
`MarkdownConfiguration.diagramSizing` with values `Scroll` (default) and
`FitToWidth`.

#### Scenario: Oversized diagram scrolls
- **WHEN** `diagramSizing` is `Scroll` and the diagram is larger than the viewport
- **THEN** it SHALL be pannable/scrollable within its container

#### Scenario: Fit to width
- **WHEN** `diagramSizing` is `FitToWidth`
- **THEN** the diagram SHALL scale to fit the available width

