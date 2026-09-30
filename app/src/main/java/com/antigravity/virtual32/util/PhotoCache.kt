package com.antigravity.virtual32.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class PhotoCache(private val context: Context) {
    private val cacheDir = File(context.filesDir, "photos")
    private val MAX_PHOTOS = 50

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun saveInternal(jpeg: ByteArray, fileName: String): String? {
        try {
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { it.write(jpeg) }
            enforceCap()
            return file.absolutePath
        } catch (e: Exception) {
            return null
        }
    }

    private fun enforceCap() {
        val files = cacheDir.listFiles() ?: return
        if (files.size > MAX_PHOTOS) {
            files.sortedBy { it.lastModified() }
                .take(files.size - MAX_PHOTOS)
                .forEach { it.delete() }
        }
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
