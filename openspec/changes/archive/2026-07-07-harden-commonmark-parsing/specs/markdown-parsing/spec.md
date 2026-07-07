# markdown-parsing

## ADDED Requirements

### Requirement: Conformance corpus
The parser SHALL be validated against a structural conformance corpus covering
CommonMark and GFM edge cases, and SHALL satisfy every case in that corpus.

#### Scenario: Emphasis rule-of-three
- **WHEN** the source contains `**a*b*c**`
- **THEN** the parser SHALL emit a `Strong` node whose children include an `Emphasis` node for `b`

#### Scenario: Intraword emphasis with underscores
- **WHEN** the source contains `a_b_c`
- **THEN** the parser SHALL NOT emit an `Emphasis` node (underscores are intraword)

#### Scenario: Escaped delimiters do not open spans
- **WHEN** the source contains `\*\*not bold\*\*`
- **THEN** the parser SHALL emit literal text `**not bold**` with no `Strong` node

#### Scenario: Fenced code preserves indentation
- **WHEN** an indented fenced code block is parsed
- **THEN** the fence's leading indentation SHALL be stripped from the content by the fence's own indent, and inner indentation preserved

#### Scenario: GFM table with ragged rows
- **WHEN** a table row has fewer cells than the header
- **THEN** the parser SHALL still emit a `Table` with the header's column count and pad missing cells
