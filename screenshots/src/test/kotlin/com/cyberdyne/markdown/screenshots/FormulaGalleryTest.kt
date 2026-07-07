package com.cyberdyne.markdown.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import org.junit.Rule
import org.junit.Test

class FormulaGalleryTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2200),
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    @Test fun formulas() {
        paparazzi.snapshot("formulas") {
            GalleryFrame(MarkdownTheme.Light) {
                MarkdownView(markdown = FORMULAS, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    private companion object {
        val FORMULAS = """
            ## Formula sheet

            Mass–energy equivalence:

            ${'$'}${'$'}
            E = mc^2
            ${'$'}${'$'}

            Quadratic roots:

            ${'$'}${'$'}
            \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
            ${'$'}${'$'}

            Sum of squares:

            ${'$'}${'$'}
            \sum_{i=1}^{n} i^2 = \frac{n(n+1)(2n+1)}{6}
            ${'$'}${'$'}

            Gaussian integral:

            ${'$'}${'$'}
            \int_0^\infty e^{-x^2} dx = \frac{\sqrt{\pi}}{2}
            ${'$'}${'$'}

            Greek & operators:

            ${'$'}${'$'}
            \alpha + \beta \leq \gamma \times \delta
            ${'$'}${'$'}

            Identity matrix:

            ${'$'}${'$'}
            \begin{pmatrix} 1 & 0 \\ 0 & 1 \end{pmatrix}
            ${'$'}${'$'}

            Grouped and nested radical:

            ${'$'}${'$'}
            \left( \frac{a + b}{c} \right)^2 + \sqrt{1 + \sqrt{1 + x}}
            ${'$'}${'$'}
        """.trimIndent()
    }
}
