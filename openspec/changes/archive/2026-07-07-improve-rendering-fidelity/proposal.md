# Improve rendering fidelity

## Why

Three rendering gaps remain from the baseline: Mermaid ignores inline color
directives, the LaTeX renderer lacks matrices/limits/delimiters, and inline
images/math fall back to text while links use the deprecated `ClickableText`.

## What Changes

- **Mermaid inline styles**: parse `style`/`classDef` directives and inline node
  color syntax (`fill`, `stroke`, `color`) in `#RGB`/`#RRGGBB`/CSS-named form,
  with theme-palette fallback; apply them when rendering flowchart nodes.
- **Extended LaTeX**: parse and render matrices (`\begin{matrix|pmatrix|bmatrix}`),
  sized delimiters (`\left…\right`), and large operators with limits.
- **Inline content**: render inline images and inline math via Compose
  `InlineTextContent`; migrate link rendering from deprecated `ClickableText` to
  `LinkAnnotation`.

## Capabilities

### Modified Capabilities
- `mermaid-diagrams` — inline style directives honored.
- `latex-math-rendering` — matrices/delimiters/limits coverage.
- `document-rendering` — inline images/math content and modern link annotations.

## Impact

- `markdown-engine`: `MermaidParser`/`MermaidView`, `InlineText`; `markdown-engine-latex`: `LatexParser`/`NativeLatexRenderer`.
- Pure color parsing and LaTeX structure parsing are unit-tested; rendering is compile-verified.

## Non-goals

- Full CSS color spec (named subset only); full LaTeX (amsmath environments beyond matrices).
