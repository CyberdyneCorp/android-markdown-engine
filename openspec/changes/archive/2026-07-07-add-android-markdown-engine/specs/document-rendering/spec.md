# document-rendering

Mirrors iOS capability `document-rendering`.

## ADDED Requirements

### Requirement: Compose Markdown view
The library SHALL provide a `@Composable MarkdownView` that renders either a raw
Markdown `String` or a pre-parsed `MarkdownDocument` natively, using Jetpack
Compose. It MUST NOT use a `WebView` or any JavaScript runtime.

#### Scenario: Render from string
- **WHEN** an app calls `MarkdownView("# Title\n\nBody")`
- **THEN** the composable SHALL parse and render the heading and paragraph natively

#### Scenario: Render from parsed document
- **WHEN** an app passes an already-parsed `MarkdownDocument` to `MarkdownView`
- **THEN** the composable SHALL render it without re-parsing the source

### Requirement: Block element rendering
`MarkdownView` SHALL render all block elements: headings 1–6 with distinct
typography, paragraphs, block quotes, thematic breaks, ordered/unordered/task
lists with nesting, and GFM tables honoring per-column alignment.

#### Scenario: Heading hierarchy
- **WHEN** a document contains headings of levels 1 through 3
- **THEN** each SHALL render with the decreasing type scale defined by the active `MarkdownTheme`

#### Scenario: Table alignment honored
- **WHEN** a table has left/center/right aligned columns
- **THEN** each cell SHALL render with its column's alignment

#### Scenario: Nested list indentation
- **WHEN** a list contains nested sublists
- **THEN** each level SHALL render with correct indentation and the marker for that level

### Requirement: Inline element rendering
`MarkdownView` SHALL render inline elements with correct flow and wrapping:
emphasis, strong, strikethrough, inline code, links, autolinks, images,
wiki-links, footnote references, and line breaks.

#### Scenario: Mixed inline formatting
- **WHEN** a paragraph mixes bold, italic, inline code, and a link
- **THEN** all SHALL render inline with correct styling and wrap within the reading width

#### Scenario: Link tap invokes handler
- **WHEN** the user taps a rendered link
- **THEN** `MarkdownView` SHALL invoke the configured link-open handler with the destination URL

### Requirement: Interactive task checkboxes
Task list items SHALL render as toggleable controls when
`MarkdownConfiguration.interactiveCheckboxes` is enabled, and toggling SHALL emit
a change carrying the toggled item's source range and its new checked state.

#### Scenario: Toggle checkbox emits change
- **WHEN** interactive checkboxes are enabled and the user toggles a task item
- **THEN** `MarkdownView` SHALL emit a change event with the item's source range and the new `checked` value

### Requirement: Image loading
`MarkdownView` SHALL load images via the configured `EmbeddedImageProvider`,
showing a placeholder while loading and on failure, and using the image `alt`
text as the accessibility (`contentDescription`) label.

#### Scenario: Remote image via provider
- **WHEN** an image references a remote URL and a provider is configured
- **THEN** the provider SHALL resolve the image and the placeholder SHALL be shown until it loads

#### Scenario: Image failure placeholder
- **WHEN** image loading fails
- **THEN** a failure placeholder SHALL be shown instead of crashing

### Requirement: Wide-content overflow handling
Content wider than the available width (tables, code blocks, diagrams) SHALL
scroll horizontally within its own container while surrounding prose stays within
the reading width; wide content MUST NOT be clipped and MUST NOT force the whole
page to scroll horizontally.

#### Scenario: Wide table scrolls in its own container
- **WHEN** a table is wider than the viewport
- **THEN** only that table SHALL scroll horizontally and the surrounding text SHALL remain within reading width

### Requirement: Accessibility
`MarkdownView` SHALL expose heading semantics with levels, image alt text,
individually focusable links, and SHALL respect the system font scale
(Dynamic Type equivalent).

#### Scenario: Heading announced with level
- **WHEN** TalkBack focuses a level-2 heading
- **THEN** it SHALL be announced as a heading at level 2

#### Scenario: Font scale respected
- **WHEN** the system font scale is increased
- **THEN** prose SHALL scale accordingly

### Requirement: Video block rendering
A block whose sole content is a video-bearing image SHALL render as a video
embed: a linked thumbnail `[![alt](thumb)](videoURL)` renders as a tappable video
thumbnail, an image whose source is a direct video file renders as an inline
native player, and a non-video linked image renders unchanged.

#### Scenario: Linked thumbnail becomes video embed
- **WHEN** a block is `[![alt](thumb.png)](https://youtu.be/x)`
- **THEN** it SHALL render as a tappable video thumbnail with a play overlay

#### Scenario: Direct video source inline player
- **WHEN** a block is an image whose source ends in `.mp4`
- **THEN** it SHALL render as an inline native player

#### Scenario: Non-video linked image unchanged
- **WHEN** a linked image points to a non-video URL
- **THEN** it SHALL render as an ordinary linked image
