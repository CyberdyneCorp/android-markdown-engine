package com.cyberdyne.markdown.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.cyberdyne.markdown.editor.MarkdownEditorState
import com.cyberdyne.markdown.editor.MarkdownLiveEditor
import com.cyberdyne.markdown.editor.MarkdownWysiwygEditor
import com.cyberdyne.markdown.editor.rememberWysiwygState
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import org.junit.Rule
import org.junit.Test

class EditorGalleryTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1800),
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    @Test fun liveEditor() {
        val text = "# Heading\n\nSome **bold** and *italic* text on this line.\n\nAnother line with `code` and ~~strike~~."
        paparazzi.snapshot("live-editor") {
            GalleryFrame(MarkdownTheme.Light) {
                // Cursor placed on the middle line so its markers reveal while others hide.
                val state = remember { MarkdownEditorState(text).apply { value = TextFieldValue(text, TextRange(22)) } }
                MarkdownLiveEditor(state = state, modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test fun wysiwygEditor() {
        paparazzi.snapshot("wysiwyg-editor") {
            GalleryFrame(MarkdownTheme.Light) {
                val state = rememberWysiwygState(
                    "# Title\n\nA paragraph with **bold** text.\n\n- one\n- two\n\n| Feature | Status |\n|---|---|\n| Parse | done |",
                )
                MarkdownWysiwygEditor(state = state, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
