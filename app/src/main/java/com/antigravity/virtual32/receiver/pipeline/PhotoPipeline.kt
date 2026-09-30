package com.antigravity.virtual32.receiver.pipeline

interface PhotoPipeline {
    suspend fun processPhoto(jpeg: ByteArray): String
}

class FakePhotoPipeline : PhotoPipeline {
    override suspend fun processPhoto(jpeg: ByteArray): String {
        return "{\"status\":\"ok\",\"count\":3}"
    }
}
