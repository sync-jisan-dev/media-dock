package com.mediadock.app

import com.mediadock.app.core.common.FormattingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingUtilsTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", FormattingUtils.formatBytes(0))
        assertEquals("1.0 KB", FormattingUtils.formatBytes(1024))
        assertEquals("1.5 MB", FormattingUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.0 GB", FormattingUtils.formatBytes((2L * 1024 * 1024 * 1024)))
    }

    @Test
    fun testFormatSpeed() {
        assertEquals("2.5 MB/s", FormattingUtils.formatSpeed((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", FormattingUtils.formatDuration(0))
        assertEquals("00:45", FormattingUtils.formatDuration(45))
        assertEquals("05:30", FormattingUtils.formatDuration(330))
        assertEquals("01:15:20", FormattingUtils.formatDuration(4520))
    }

    @Test
    fun testSanitizeFilename() {
        assertEquals("my_cool_video", FormattingUtils.sanitizeFilename("my:cool*video"))
        assertEquals("sunset_photo", FormattingUtils.sanitizeFilename("sunset<photo>?"))
        assertEquals("fallback", FormattingUtils.sanitizeFilename("   ", fallback = "fallback"))
    }

    @Test
    fun testIsValidUrl() {
        assertTrue(FormattingUtils.isValidUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(FormattingUtils.isValidUrl("http://example.com/media.mp4"))
        assertFalse(FormattingUtils.isValidUrl("ftp://invalid.com/file"))
        assertFalse(FormattingUtils.isValidUrl("not a url"))
        assertFalse(FormattingUtils.isValidUrl(null))
        assertFalse(FormattingUtils.isValidUrl("http://"))
    }
}
