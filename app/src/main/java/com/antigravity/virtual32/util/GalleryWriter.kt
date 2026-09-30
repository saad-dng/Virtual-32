package com.antigravity.virtual32.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GalleryWriter(private val context: Context) {

    companion object {
        fun generateFileName(batchId: String, timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())): String {
            return "VQ_${timeStamp}_b${batchId}.jpg"
        }
    }

    fun savePhoto(jpeg: ByteArray, source: String, batchId: String = UUID.randomUUID().toString().substring(0, 8)): String? {
        val fileName = generateFileName(batchId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Virtual32")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

            try {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(jpeg)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri.toString()
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                return null
            }
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val vqDir = File(picturesDir, "Virtual32")
            if (!vqDir.exists()) {
                vqDir.mkdirs()
            }
            val file = File(vqDir, fileName)
            try {
                FileOutputStream(file).use { os ->
                    os.write(jpeg)
                }
                // Notify media scanner
                val uri = android.net.Uri.fromFile(file)
                val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                context.sendBroadcast(intent)
                return uri.toString()
            } catch (e: Exception) {
                return null
            }
        }
    }
}
