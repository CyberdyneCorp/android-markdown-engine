# live-editor-inline-lists

Mirrors iOS capability `live-editor-inline-lists`.

## ADDED Requirements

### Requirement: Flat lists render as inline-editable text
Flat lists SHALL render as inline-editable text in the Live editor — bulleted,
ordered, and GFM task lists whose every item is a single paragraph — with styled
markers, not read-only block attachments. Nested or multi-block list items SHALL
remain tap-to-edit blocks.

#### Scenario: Flat bulleted list editable in place
- **WHEN** a flat bulleted list `- a` / `- b` / `- c` is shown in the Live editor
- **THEN** it SHALL be editable in place with a styled marker per item

#### Scenario: Multi-block item stays a block
- **WHEN** a list item contains multiple blocks
- **THEN** that item SHALL remain a tap-to-edit block attachment

### Requirement: Enter continues and ends lists
Pressing Enter at the end of a non-empty list item SHALL insert a new item with
the next marker (ordered markers increment); pressing Enter on an empty item SHALL
remove the marker and end the list.

#### Scenario: Enter continues a bulleted list
- **WHEN** the caret is at the end of `- item` and Enter is pressed
- **THEN** a new `- ` item SHALL be inserted

#### Scenario: Ordered marker increments
- **WHEN** the caret is at the end of `1. item` and Enter is pressed
- **THEN** a new `2. ` item SHALL be inserted

#### Scenario: Enter on empty item ends the list
- **WHEN** Enter is pressed on an empty list item
- **THEN** the marker SHALL be removed

### Requirement: Tab indents a list item
Pressing Tab at a list item SHALL increase its indentation by one level.

#### Scenario: Tab indents
- **WHEN** the caret is in a list item and Tab is pressed
- **THEN** the item's indentation SHALL increase by one level

### Requirement: Tapping a checkbox toggles it
Tapping a task-list checkbox in the Live editor SHALL toggle the item between
unchecked and checked in the underlying Markdown.

#### Scenario: Toggle task item
- **WHEN** the user taps the `[ ]` of `- [ ] task`
- **THEN** the underlying text SHALL become `- [x] task`
