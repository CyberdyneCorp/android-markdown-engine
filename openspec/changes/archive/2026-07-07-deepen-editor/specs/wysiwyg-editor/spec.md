# wysiwyg-editor

## ADDED Requirements

### Requirement: Visual table and code editors
The WYSIWYG editor SHALL provide a grid table editor (edit cells, add/remove rows
and columns, set per-column alignment) and a code editor with a language field,
each serializing back to Markdown.

#### Scenario: Table grid round-trips
- **WHEN** a GFM table block is loaded into the grid editor and re-serialized
- **THEN** the output SHALL be a GFM table with the same cells and alignments

#### Scenario: Add a column
- **WHEN** the user adds a column to the grid
- **THEN** the serialized table SHALL have one more column in the header and each row
