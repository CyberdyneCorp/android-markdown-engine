# Deepen the editor

## Why

Editor depth items remain: wiki-link completion, spellcheck suppression in
code/math/wiki spans, comfortable scrolling, and richer WYSIWYG per-type editors.

## What Changes

- **Wiki-link completion**: detect an open `[[` context at the cursor and surface
  candidate targets from a `WikiLinkResolver`.
- **Spellcheck suppression**: compute code/math/wiki-link regions and disable
  spellcheck/autocorrect when the cursor is inside one.
- **Scrolling ergonomics**: bottom overscroll and a readable content column.
- **WYSIWYG per-type editors**: a visual table grid editor and a code editor with
  a language field, driven by pure grid/round-trip helpers.

## Capabilities

### Modified Capabilities
- `markdown-editor` — wiki-link completion, spellcheck suppression, overscroll.
- `wysiwyg-editor` — visual table and code block editors.

## Impact

- `markdown-editor` module only. Pure detection/region/grid logic is unit-tested;
  UI is compile-verified.

## Non-goals

- Inline live block composables inside the live editor flow (tracked separately).
