package app.applister.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicGuardsTest {

    @Test
    fun exportFormat_indicesAreStable() {
        assertEquals(0, Constants.ExportFormat.MARKDOWN)
        assertEquals(1, Constants.ExportFormat.PLAIN_TEXT)
        assertEquals(2, Constants.ExportFormat.JSON)
        assertEquals(3, Constants.ExportFormat.HTML)
    }

    @Test
    fun exportFormat_coerceClampsCorruptPrefs() {
        assertEquals(Constants.ExportFormat.MARKDOWN, Constants.ExportFormat.coerce(-1))
        assertEquals(Constants.ExportFormat.MARKDOWN, Constants.ExportFormat.coerce(99))
        assertEquals(Constants.ExportFormat.JSON, Constants.ExportFormat.coerce(2))
    }

    @Test
    fun exportFormat_onlyJsonIsRestorable() {
        assertTrue(Constants.ExportFormat.isRestorable(Constants.ExportFormat.JSON))
        assertFalse(Constants.ExportFormat.isRestorable(Constants.ExportFormat.MARKDOWN))
        assertFalse(Constants.ExportFormat.isRestorable(Constants.ExportFormat.PLAIN_TEXT))
        assertFalse(Constants.ExportFormat.isRestorable(Constants.ExportFormat.HTML))
        assertFalse(Constants.ExportFormat.isRestorable(99))
    }

    @Test
    fun exportFormat_extensionAndMimeAgree() {
        assertEquals("json", Constants.ExportFormat.extension(Constants.ExportFormat.JSON))
        assertEquals("application/json", Constants.ExportFormat.mimeType(Constants.ExportFormat.JSON))
        assertEquals("md", Constants.ExportFormat.extension(Constants.ExportFormat.MARKDOWN))
        assertEquals("html", Constants.ExportFormat.extension(Constants.ExportFormat.HTML))
        assertEquals("txt", Constants.ExportFormat.extension(Constants.ExportFormat.PLAIN_TEXT))
    }

    @Test
    fun retention_pendingInsertCountsTowardLimit() {
        // prune condition must be: size + pending - MAX > 0
        fun overBy(size: Int, pending: Int) = size + pending - Constants.MAX_AUTO_BACKUPS
        assertEquals(0, overBy(29, 1)) // 29 + new row = 30, nothing to prune
        assertEquals(1, overBy(30, 1)) // 30 + new row = 31, prune exactly 1
        assertEquals(0, overBy(30, 0))
    }
}
