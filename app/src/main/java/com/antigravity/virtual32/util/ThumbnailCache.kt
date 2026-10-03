package com.antigravity.virtual32.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = max(1024, maxMemory / 8) // 1/8th of memory

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun getThumbnail(path: String, targetSize: Int = 160): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${path}_$targetSize"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val file = File(path)
        if (!file.exists()) return@withContext null

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            val longest = max(options.outWidth, options.outHeight)
            var inSampleSize = 1
            if (longest > targetSize) {
                while (longest / (inSampleSize * 2) >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun DownscaledThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetSize: Int = 160,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(imagePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imagePath) {
        bitmap = ThumbnailCache.getThumbnail(imagePath, targetSize)
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
