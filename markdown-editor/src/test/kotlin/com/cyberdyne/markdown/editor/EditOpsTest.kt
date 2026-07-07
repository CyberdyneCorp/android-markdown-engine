package com.cyberdyne.markdown.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditOpsTest {

    @Test fun wrapSelectionAddsDelimiters() {
        val r = EditOps.wrapSelection("say hello world", 4, 9, "**")
        assertEquals("say **hello** world", r.text)
    }

    @Test fun wrapSelectionTogglesOff() {
        val r = EditOps.wrapSelection("say **hello** world", 4, 13, "**")
        assertEquals("say hello world", r.text)
    }

    @Test fun wrapEmptyInsertsPair() {
        val r = EditOps.wrapSelection("ab", 1, 1, "*")
        assertEquals("a**b", r.text)
        assertEquals(2, r.selStart)
    }

    @Test fun toggleBulletPrefixOnAndOff() {
        val on = EditOps.toggleLinePrefix("a\nb", 0, 3, "- ")
        assertEquals("- a\n- b", on.text)
        val off = EditOps.toggleLinePrefix(on.text, 0, on.text.length, "- ")
        assertEquals("a\nb", off.text)
    }

    @Test fun setHeadingAndToggle() {
        val h2 = EditOps.setHeading("Title", 0, 0, 2)
        assertEquals("## Title", h2.text)
        val off = EditOps.setHeading(h2.text, 3, 3, 2)
        assertEquals("Title", off.text)
        val changed = EditOps.setHeading("## Title", 3, 3, 3)
        assertEquals("### Title", changed.text)
    }

    @Test fun continueBulletList() {
        val r = EditOps.continueList("- item", 6)!!
        assertEquals("- item\n- ", r.text)
    }

    @Test fun continueOrderedListIncrements() {
        val r = EditOps.continueList("1. first", 8)!!
        assertEquals("1. first\n2. ", r.text)
    }

    @Test fun enterOnEmptyItemRemovesMarker() {
        val r = EditOps.continueList("- ", 2)!!
        assertEquals("", r.text)
    }

    @Test fun continueReturnsNullOutsideList() {
        assertNull(EditOps.continueList("plain text", 5))
    }

    @Test fun indentAndOutdent() {
        val inR = EditOps.indentLines("a\nb", 0, 3, outdent = false)
        assertEquals("  a\n  b", inR.text)
        val outR = EditOps.indentLines(inR.text, 0, inR.text.length, outdent = true)
        assertEquals("a\nb", outR.text)
    }

    @Test fun toggleTaskCheckbox() {
        val r = EditOps.toggleTask("- [ ] todo", 2)!!
        assertEquals("- [x] todo", r.text)
        val back = EditOps.toggleTask(r.text, 2)!!
        assertEquals("- [ ] todo", back.text)
    }

    @Test fun insertLinkWrapsSelection() {
        val r = EditOps.insertLink("see docs here", 4, 8, "https://x.com")
        assertEquals("see [docs](https://x.com) here", r.text)
    }
}
