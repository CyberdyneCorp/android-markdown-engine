# Harden CommonMark/GFM parsing

## Why

The parser was built to the documented scenarios, not the full CommonMark/GFM
edge-case surface. Before layering more features on top, we harden it with a
conformance test corpus so "looks done" and "is correct" converge, and fix the
edge cases that corpus surfaces.

## What Changes

- Add a structural conformance corpus (`ParserConformanceTest`) exercising known
  CommonMark/GFM edge cases: emphasis delimiter runs (rule of 3, intraword),
  nested/mismatched emphasis, ATX/Setext boundary cases, list start/loose
  detection, fenced-code info strings and indentation, reference-link matching,
  escapes inside constructs, and GFM table row/column mismatches.
- Fix parser defects the corpus surfaces.

## Capabilities

### Modified Capabilities
- `markdown-parsing` — adds a conformance requirement (no behavior removed).

## Impact

- `markdown-engine` parser (`InlineParser`, `BlockParser`) edge-case fixes.
- New JVM tests only; no public API change.

## Non-goals

- Not byte-exact HTML output parity with the reference `cmark` (we have no HTML
  renderer); conformance is asserted structurally against the AST.
- Not the full 649-example spec suite in this pass — a curated high-signal subset.
