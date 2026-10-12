package com.kulipai.luahook.ui.logcat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogHistoryTest {
    @Test
    fun repeatedSnapshotsAppendOnlyNewLines() {
        val history = LogHistory()
        history.appendSnapshot(listOf("a", "b"))

        assertTrue(history.appendSnapshot(listOf("a", "b")).isEmpty())
        assertEquals(listOf("c"), history.appendSnapshot(listOf("a", "b", "c")))
        assertEquals(listOf("a", "b", "c"), history.entries)
    }

    @Test
    fun rollingBufferPreservesLogsThatAreNoLongerInLogcat() {
        val history = LogHistory()
        history.appendSnapshot(listOf("a", "b", "c"))

        assertEquals(listOf("d"), history.appendSnapshot(listOf("b", "c", "d")))
        assertEquals(listOf("a", "b", "c", "d"), history.entries)
        assertTrue(history.appendSnapshot(listOf("d")).isEmpty())
        assertEquals(listOf("a", "b", "c", "d"), history.entries)
    }

    @Test
    fun emptySnapshotKeepsHistoryAndDoesNotDuplicateTheNextRead() {
        val history = LogHistory()
        history.appendSnapshot(listOf("a", "b"))

        assertTrue(history.appendSnapshot(emptyList()).isEmpty())
        assertEquals(listOf("a", "b"), history.entries)
        assertEquals(listOf("c"), history.appendSnapshot(listOf("b", "c")))
    }

    @Test
    fun identicalLogLinesRetainTheirOccurrences() {
        val history = LogHistory()
        history.appendSnapshot(listOf("same", "same"))

        assertEquals(listOf("same"), history.appendSnapshot(listOf("same", "same", "same")))
        assertEquals(listOf("same", "same", "same"), history.entries)
        assertEquals(listOf("next"), history.appendSnapshot(listOf("same", "same", "next")))
    }

    @Test
    fun nonOverlappingBufferIsAppended() {
        val history = LogHistory()
        history.appendSnapshot(listOf("a", "b"))

        assertEquals(listOf("c", "d"), history.appendSnapshot(listOf("c", "d")))
        assertEquals(listOf("a", "b", "c", "d"), history.entries)
    }

    @Test
    fun clearingHistoryAllowsFreshLogs() {
        val history = LogHistory()
        history.appendSnapshot(listOf("old"))
        history.clear()

        assertTrue(history.entries.isEmpty())
        assertEquals(listOf("new"), history.appendSnapshot(listOf("new")))
        assertEquals(listOf("new"), history.entries)
    }
}
