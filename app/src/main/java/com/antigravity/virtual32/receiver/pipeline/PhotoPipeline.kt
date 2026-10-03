package com.antigravity.virtual32.receiver.pipeline

interface PhotoPipeline {
    suspend fun processPhoto(jpeg: ByteArray, source: String = "ESP"): String =
        processPhotos(listOf(jpeg), source)

    suspend fun processPhotos(
        jpegs: List<ByteArray>,
        source: String = "ESP",
        cachedPaths: List<String> = emptyList(),
        galleryUris: List<String> = emptyList()
    ): String = processPhoto(jpegs.firstOrNull() ?: ByteArray(0), source)
}

class FakePhotoPipeline : PhotoPipeline {
    override suspend fun processPhoto(jpeg: ByteArray, source: String): String {
        return "{\"status\":\"ok\",\"count\":3,\"pages\":1,\"warnings\":[]}"
    }

    override suspend fun processPhotos(
        jpegs: List<ByteArray>,
        source: String,
        cachedPaths: List<String>,
        galleryUris: List<String>
    ): String {
        return "{\"status\":\"ok\",\"count\":3,\"pages\":${jpegs.size},\"warnings\":[]}"
    }
}
