# latex-math-rendering

## ADDED Requirements

### Requirement: Matrices, delimiters, and limits
The LaTeX renderer SHALL parse and render matrix environments
(`matrix`, `pmatrix`, `bmatrix`), sized delimiters (`\left…\right`), and large
operators carrying limits (subscript/superscript).

#### Scenario: Matrix parsed
- **WHEN** the source is `\begin{pmatrix} a & b \\ c & d \end{pmatrix}`
- **THEN** the parser SHALL emit a matrix node with two rows of two cells

#### Scenario: Sized delimiters parsed
- **WHEN** the source is `\left( x \right)`
- **THEN** the parser SHALL emit a delimited node wrapping `x`
