# editor-toolbar-customization Specification

## Purpose
TBD - created by archiving change add-android-markdown-engine. Update Purpose after archive.
## Requirements
### Requirement: Host-defined editor toolbar
`MarkdownEditor` SHALL accept a host-provided list of `MarkdownToolbarItem` via a
`toolbar` parameter. When no `toolbar` argument is supplied, it SHALL show the
default set of formatting items. A `showsToolbar = false` parameter SHALL hide the
toolbar entirely.

#### Scenario: Default toolbar
- **WHEN** `MarkdownEditor` is used with no `toolbar` argument
- **THEN** it SHALL show the built-in default formatting items

#### Scenario: Custom toolbar in order
- **WHEN** a `toolbar` list of `MarkdownToolbarItem` is provided
- **THEN** exactly those items SHALL be shown in the given order

#### Scenario: Toolbar hidden
- **WHEN** `showsToolbar = false`
- **THEN** no toolbar SHALL be shown

### Requirement: Toolbar item catalog
`MarkdownToolbarItem` SHALL be a sealed type providing built-in items `Bold`,
`Italic`, `InlineCode`, `Divider`, `Heading(level)`, `BulletList`, `TaskList`,
`Quote`, `Link`, `Indent`, `Outdent`, a `Menu(items)` submenu, and a
`Custom(id, icon, label, action)` item whose action receives the
`MarkdownEditorController`.

#### Scenario: Custom item runs host action
- **WHEN** a `Custom` toolbar item is tapped
- **THEN** its host action SHALL run and receive the editor's `MarkdownEditorController`

#### Scenario: Controller exposes formatting commands
- **WHEN** a host action calls `controller.toggleBold()` or `controller.toggleItalic()`
- **THEN** the corresponding formatting SHALL be applied to the editor selection

