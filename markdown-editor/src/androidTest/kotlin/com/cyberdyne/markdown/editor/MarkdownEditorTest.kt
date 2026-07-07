package com.cyberdyne.markdown.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultToolbarShownWhenNoneProvided() {
        composeRule.setContent {
            MarkdownEditor(state = rememberMarkdownEditorState("hi"))
        }
        composeRule.onNodeWithText("B").assertIsDisplayed() // Bold button from defaults
    }

    @Test
    fun toolbarHiddenWhenDisabled() {
        composeRule.setContent {
            MarkdownEditor(state = rememberMarkdownEditorState("hi"), showsToolbar = false)
        }
        composeRule.onNodeWithText("B").assertDoesNotExist()
    }

    @Test
    fun customItemRunsAction() {
        var ran = false
        composeRule.setContent {
            MarkdownEditor(
                state = rememberMarkdownEditorState(""),
                toolbar = listOf(
                    MarkdownToolbarItem.Custom(id = "x", icon = "★", label = "Star") { ran = true },
                ),
            )
        }
        composeRule.onNodeWithText("★").performClick()
        assertTrue(ran)
    }

    @Test
    fun boldButtonWrapsState() {
        val state = MarkdownEditorState("")
        composeRule.setContent { MarkdownEditor(state = state) }
        composeRule.onNodeWithText("B").performClick()
        assertEquals("****", state.text)
    }
}
