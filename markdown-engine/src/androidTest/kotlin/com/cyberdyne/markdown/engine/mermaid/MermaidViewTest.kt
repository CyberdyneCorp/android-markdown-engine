package com.cyberdyne.markdown.engine.mermaid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import org.junit.Rule
import org.junit.Test

/** Compose UI tests for native Mermaid rendering vs. source fallback. */
class MermaidViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun supportedFlowchartRendersNatively() {
        // A supported diagram draws on Canvas; its source text must NOT appear as a text node.
        composeRule.setContent {
            MarkdownView("```mermaid\nflowchart TD\n    A[Start] --> B[End]\n```")
        }
        composeRule.onNodeWithText("flowchart TD", substring = true).assertDoesNotExist()
    }

    @Test
    fun unsupportedDiagramFallsBackToSource() {
        composeRule.setContent {
            MarkdownView("```mermaid\nclassDiagram\n    class Animal\n```")
        }
        // Fallback renders the source in a code block (text nodes present).
        composeRule.onNodeWithText("classDiagram", substring = true).assertIsDisplayed()
    }
}
