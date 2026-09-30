package com.antigravity.virtual32.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX binding and silent single-frame JPEG capture matching OV3660 sensor behavior.
 * Enforces hardware digital twin fidelity: no shutter sound, no capture animation, no flash.
 */
class CameraCaptureManager(private val context: Context) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val TAG = "CameraCaptureManager"
    }

    /**
     * Binds CameraX Preview and ImageCapture use cases to the provided LifecycleOwner and PreviewView.
     * Uses the selected OV3660 resolution preset.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        resolution: Ov3660Resolution,
        jpegQuality: Int = JpegQualityPreset.DEFAULT.qualityPercentage,
        onError: (Throwable) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Resolution strategy aligned with OV3660 sensor aspect ratio (4:3)
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            resolution.size,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Low latency capture mode avoids pre-capture AF/AE delays and sounds
                val effectiveQuality = jpegQuality.coerceIn(1, 100)
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(resolutionSelector)
                    .setJpegQuality(effectiveQuality)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d(TAG, "Camera bound successfully with resolution: ${resolution.label}")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                onError(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Executes a silent hardware capture standing in for GPIO 1 trigger.
     * Returns raw JPEG bytes directly from memory without saving to disk or playing audio.
     */
    fun captureFrameSilently(
        onSuccess: (ByteArray, Int, Int) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera capture is not initialized"))
            return
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val width = image.width
                        val height = image.height

                        Log.d(TAG, "Silent capture success: ${bytes.size} bytes (${width}x${height})")
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(bytes, width, height)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed extracting JPEG bytes", e)
                        ContextCompat.getMainExecutor(context).execute {
                            onError(e)
                        }
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
