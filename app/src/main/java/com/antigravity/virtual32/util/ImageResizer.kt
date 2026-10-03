package com.antigravity.virtual32.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageResizer {
    fun downscaleIfNeeded(jpeg: ByteArray, maxSide: Int = 2048): ByteArray {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
        
        val width = options.outWidth
        val height = options.outHeight
        val longest = max(width, height)
        
        if (longest <= maxSide) {
            return jpeg
        }
        
        val ratio = maxSide.toFloat() / longest.toFloat()
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
        
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: return jpeg
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        
        bitmap.recycle()
        if (scaled != bitmap) scaled.recycle()
        
        return out.toByteArray()
    }

    fun downscaleForSession(jpeg: ByteArray, maxSide: Int = 1600, quality: Int = 85): ByteArray {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
        
        val width = options.outWidth
        val height = options.outHeight
        val longest = max(width, height)
        
        if (longest <= 0) return jpeg
        
        val (newWidth, newHeight) = if (longest > maxSide) {
            val ratio = maxSide.toFloat() / longest.toFloat()
            (width * ratio).toInt() to (height * ratio).toInt()
        } else {
            width to height
        }
        
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
        
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: return jpeg
        val scaled = if (bitmap.width != newWidth || bitmap.height != newHeight) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        
        bitmap.recycle()
        if (scaled != bitmap) scaled.recycle()
        
        return out.toByteArray()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
