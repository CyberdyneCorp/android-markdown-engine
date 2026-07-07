# Platform and release

## Why

Final roadmap milestone: a Wear OS render-only subset, platform-idiomatic
hardware-keyboard shortcuts in the editor, and Maven publishing so the library
is consumable — paralleling the iOS product distribution.

## What Changes

- **Wear OS subset**: a `markdown-engine-wear` module exposing `WearMarkdownView`,
  a render-only surface with a constrained configuration (no editor; math/Mermaid
  degrade to source) suitable for a small screen.
- **Keyboard shortcuts**: the editor handles Ctrl/Cmd+B (bold), +I (italic),
  +K (link).
- **Maven publishing**: the four library modules publish AARs under
  `com.cyberdyne.markdown` coordinates paralleling the iOS products.

## Capabilities

### Modified Capabilities
- `platform-support` — Wear OS render-only subset; publishable artifacts.
- `markdown-editor` — hardware-keyboard formatting shortcuts.

## Impact

- New `markdown-engine-wear` module; `maven-publish` config on library modules;
  editor key handling. Verified by build + `publishToMavenLocal`.

## Non-goals

- Full Wear OS UX (tiles/complications); real device signing/upload to a registry.
