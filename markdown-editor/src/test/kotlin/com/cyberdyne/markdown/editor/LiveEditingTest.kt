package com.cyberdyne.markdown.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveEditingTest {

    @Test fun transformRemovesHiddenRanges() {
        val map = OffsetMap(listOf(0..1, 6..7))
        assertEquals("bold", map.transform("**bold**"))
    }

    @Test fun offsetRoundTripsForVisibleChars() {
        val text = "**bold**" // hidden ** at [0,1] and [6,7]; visible = "bold"
        val map = OffsetMap(listOf(0..1, 6..7))
        // Visible chars b,o,l,d are original 2,3,4,5 -> transformed 0,1,2,3
        for ((orig, trans) in listOf(2 to 0, 3 to 1, 4 to 2, 5 to 3)) {
            assertEquals(trans, map.originalToTransformed(orig))
            assertEquals(orig, map.transformedToOriginal(trans))
        }
    }

    @Test fun boundsMapToEnds() {
        val map = OffsetMap(listOf(0..1, 6..7))
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(4, map.originalToTransformed(8)) // end of "**bold**" -> end of "bold"
        assertEquals(4, map.transform("**bold**").length)
    }

    @Test fun mappingIsMonotonic() {
        val map = OffsetMap(listOf(2..3, 8..9))
        val text = "ab**cd**ef" // len 10
        var prev = -1
        for (o in 0..text.length) {
            val t = map.originalToTransformed(o)
            assertTrue(t >= prev, "not monotonic at $o")
            prev = t
        }
    }

    @Test fun hiddenRangesOnlyOffActiveLine() {
        val text = "**bold**\nplain"
        // cursor on line 2 (inside "plain") -> line 1 markers hidden
        val offLine = LiveMarkers.hiddenRanges(text, 11)
        assertEquals(listOf(0..1, 6..7), offLine)
        // cursor inside the bold on line 1 -> nothing hidden
        assertTrue(LiveMarkers.hiddenRanges(text, 3).isEmpty())
    }

    @Test fun headingPrefixHiddenOffActiveLine() {
        val text = "## Title\nbody"
        val hidden = LiveMarkers.hiddenRanges(text, 10) // cursor in "body"
        assertTrue(hidden.contains(0..2)) // "## " prefix
    }
}
