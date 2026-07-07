package com.cyberdyne.markdown.engine.rendering

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.model.SourceRange
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Compose UI tests for [MarkdownView]. Run on device/emulator (connectedCheck) or
 * via a Compose-capable Robolectric setup in CI.
 */
class MarkdownViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersHeadingAndParagraph() {
        composeRule.setContent {
            MarkdownView("# Hello\n\nSome body text.")
        }
        composeRule.onNodeWithText("Hello").assertIsDisplayed()
        composeRule.onNodeWithText("Some body text.").assertIsDisplayed()
    }

    @Test
    fun interactiveCheckboxEmitsToggle() {
        var toggled: Pair<SourceRange, Boolean>? = null
        composeRule.setContent {
            MarkdownView(
                markdown = "- [ ] task",
                configuration = MarkdownConfiguration(interactiveCheckboxes = true),
                onCheckboxToggle = { range, checked -> toggled = range to checked },
            )
        }
        composeRule.onNodeWithText("task").assertIsDisplayed()
        composeRule.onNode(androidx.compose.ui.test.hasClickAction()).performClick()
        assertNotNull(toggled)
        assertEquals(true, toggled?.second)
    }

    @Test
    fun linkClickInvokesHandler() {
        var clicked: String? = null
        composeRule.setContent {
            MarkdownView(
                markdown = "See [the site](https://example.com).",
                onLinkClick = { clicked = it },
            )
        }
        composeRule.onNodeWithText("the site", substring = true).performClick()
        assertEquals("https://example.com", clicked)
    }
}
