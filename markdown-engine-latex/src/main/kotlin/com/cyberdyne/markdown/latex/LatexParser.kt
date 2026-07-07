package com.cyberdyne.markdown.latex

/** Parsed LaTeX math expression tree. Pure data. */
sealed interface MathNode {
    data class Row(val items: List<MathNode>) : MathNode
    data class Symbol(val text: String) : MathNode
    data class Sup(val base: MathNode, val exponent: MathNode) : MathNode
    data class Sub(val base: MathNode, val subscript: MathNode) : MathNode
    data class Frac(val numerator: MathNode, val denominator: MathNode) : MathNode
    data class Sqrt(val body: MathNode) : MathNode
}

/**
 * A small, dependency-free LaTeX math parser covering the common math-mode subset:
 * symbols, super/subscripts, `\frac`, `\sqrt`, groups, Greek letters, and common
 * operators. Never throws — unparseable input degrades to literal symbols.
 */
class LatexParser(private val s: String) {
    private var i = 0

    fun parse(): MathNode = parseRow(topLevel = true)

    private fun parseRow(topLevel: Boolean): MathNode {
        val items = mutableListOf<MathNode>()
        while (i < s.length && s[i] != '}') {
            val atom = parseAtom() ?: break
            var node = atom
            while (i < s.length && (s[i] == '^' || s[i] == '_')) {
                val sup = s[i] == '^'; i++
                val script = parseGroup()
                node = if (sup) MathNode.Sup(node, script) else MathNode.Sub(node, script)
            }
            items += node
        }
        return if (items.size == 1) items[0] else MathNode.Row(items)
    }

    private fun parseAtom(): MathNode? {
        if (i >= s.length) return null
        return when (val c = s[i]) {
            '{' -> { i++; val r = parseRow(false); if (i < s.length && s[i] == '}') i++; r }
            '}' -> null
            '\\' -> parseCommand()
            ' ', '\n', '\t' -> { i++; parseAtom() }
            '^', '_' -> MathNode.Symbol("") // handled by caller via previous atom
            else -> { i++; MathNode.Symbol(c.toString()) }
        }
    }

    private fun parseCommand(): MathNode {
        i++ // consume backslash
        val start = i
        while (i < s.length && s[i].isLetter()) i++
        val name = if (i > start) s.substring(start, i) else s.getOrNull(i)?.also { i++ }?.toString() ?: ""
        return when (name) {
            "frac", "dfrac", "tfrac" -> MathNode.Frac(parseGroup(), parseGroup())
            "sqrt" -> MathNode.Sqrt(parseGroup())
            else -> MathNode.Symbol(SYMBOLS[name] ?: name)
        }
    }

    private fun parseGroup(): MathNode {
        while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\t')) i++
        return if (i < s.length && s[i] == '{') {
            i++; val r = parseRow(false); if (i < s.length && s[i] == '}') i++; r
        } else {
            parseAtom() ?: MathNode.Symbol("")
        }
    }

    companion object {
        /** LaTeX command → Unicode symbol. */
        val SYMBOLS: Map<String, String> = mapOf(
            "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε",
            "zeta" to "ζ", "eta" to "η", "theta" to "θ", "iota" to "ι", "kappa" to "κ",
            "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ", "pi" to "π",
            "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "phi" to "φ", "chi" to "χ",
            "psi" to "ψ", "omega" to "ω",
            "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ", "Pi" to "Π",
            "Sigma" to "Σ", "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",
            "times" to "×", "div" to "÷", "pm" to "±", "mp" to "∓", "cdot" to "·",
            "leq" to "≤", "geq" to "≥", "neq" to "≠", "approx" to "≈", "equiv" to "≡",
            "infty" to "∞", "partial" to "∂", "nabla" to "∇", "int" to "∫", "sum" to "∑",
            "prod" to "∏", "sqrt" to "√", "rightarrow" to "→", "leftarrow" to "←",
            "Rightarrow" to "⇒", "Leftarrow" to "⇐", "in" to "∈", "notin" to "∉",
            "subset" to "⊂", "supset" to "⊃", "cup" to "∪", "cap" to "∩",
            "forall" to "∀", "exists" to "∃", "emptyset" to "∅", "angle" to "∠",
        )
    }
}
