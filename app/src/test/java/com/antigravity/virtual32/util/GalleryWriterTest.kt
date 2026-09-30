package com.antigravity.virtual32.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryWriterTest {
    @Test
    fun testFileNameGeneration() {
        val fileName = GalleryWriter.generateFileName(batchId = "abcd123", timeStamp = "20260930_100000")
        assertEquals("VQ_20260930_100000_babcd123.jpg", fileName)
    }
}
