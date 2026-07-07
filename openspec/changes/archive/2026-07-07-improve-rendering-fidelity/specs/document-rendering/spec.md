# document-rendering

## ADDED Requirements

### Requirement: Inline media and modern links
`MarkdownView` SHALL render inline images and inline math as inline content
within flowing text (not alt-text fallback), and SHALL attach link click
behavior via `LinkAnnotation` rather than the deprecated `ClickableText`.

#### Scenario: Inline image renders in flow
- **WHEN** a paragraph mixes text and an inline image
- **THEN** the image SHALL render inline within the text flow

#### Scenario: Link uses annotation
- **WHEN** a link is rendered and tapped
- **THEN** the configured handler (or the platform URI handler) SHALL be invoked via a `LinkAnnotation`
