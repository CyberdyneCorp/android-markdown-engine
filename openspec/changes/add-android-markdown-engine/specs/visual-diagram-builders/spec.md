# visual-diagram-builders

Mirrors iOS capability `visual-diagram-builders`.

## ADDED Requirements

### Requirement: Visual flowchart builder
The WYSIWYG editor SHALL provide a form-based flowchart builder to choose a
direction, add/edit/remove nodes (id, label, shape) and edges (from, to, optional
label), serializing to a `flowchart <dir>` Mermaid block. Selecting a `flowchart`
or `graph` block SHALL populate the builder for editing.

#### Scenario: Build a flowchart
- **WHEN** the user adds two nodes, an edge, and a direction in the builder
- **THEN** it SHALL serialize to a `flowchart <dir>` Mermaid block with those nodes and edge

#### Scenario: Edit an existing flowchart
- **WHEN** the user selects an existing `flowchart` or `graph` block
- **THEN** the builder SHALL be populated with its nodes, edges, and direction

### Requirement: Pie-chart builder
The editor SHALL provide a form-based pie-chart builder to set a title and
add/edit/remove slices (label + numeric value), serializing to a `pie` block.

#### Scenario: Build a pie chart
- **WHEN** the user sets a title and slices
- **THEN** it SHALL serialize to a `pie title …` block with `"label" : value` lines

### Requirement: Sequence, mindmap, and gantt builders
The editor SHALL provide form-based builders for sequence diagrams (participants +
messages), mindmaps (indented nodes), and gantt charts, serializing to
`sequenceDiagram`, `mindmap`, and `gantt` blocks respectively.

#### Scenario: Build a sequence diagram
- **WHEN** the user adds participants and a message
- **THEN** it SHALL serialize to a `sequenceDiagram` with `participant` lines and messages

#### Scenario: Build a mindmap
- **WHEN** the user adds nodes with indentation
- **THEN** it SHALL serialize to a `mindmap` block whose indentation reflects the hierarchy

#### Scenario: Build a gantt chart
- **WHEN** the user adds a title and tasks
- **THEN** it SHALL serialize to a `gantt` block with `title`, `section`, and task lines

### Requirement: Source-editor fallback for unbuilt types
The editor SHALL provide a source editor with a live preview for Mermaid diagram
types without a visual builder yet (class, state, ER, git graph, journey,
timeline).

#### Scenario: Unsupported type uses source editor
- **WHEN** the user selects a `classDiagram` block and no visual builder exists for it
- **THEN** a source editor with live preview SHALL be shown

### Requirement: Remaining diagram builders
The editor SHALL provide visual builders for the remaining diagram types — class
diagrams, state diagrams, ER diagrams, git graphs, user journeys, and timelines —
so all 11 Mermaid types are eventually visually buildable.

#### Scenario: Build a class diagram
- **WHEN** the user adds classes, members, and a relationship
- **THEN** it SHALL serialize to a `classDiagram` block

#### Scenario: Build a git graph
- **WHEN** the user adds commit, branch, checkout, and merge steps
- **THEN** it SHALL serialize to a `gitGraph` block
