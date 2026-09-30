package com.antigravity.virtual32.receiver.pipeline

interface PhotoPipeline {
    suspend fun processPhoto(jpeg: ByteArray, source: String = "ESP"): String
}

class FakePhotoPipeline : PhotoPipeline {
    override suspend fun processPhoto(jpeg: ByteArray, source: String): String {
        return "{\"status\":\"ok\",\"count\":3}"
    }
}
