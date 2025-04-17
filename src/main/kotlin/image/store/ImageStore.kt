package io.image.store

import io.image.StoreImageRequest

interface ImageStore {

    suspend fun persist(data: StoreImageRequest, image: ByteArray): PersistResult
}

data class PersistResult(
    val key: String,
    val bucket: String,
    val url: String
)
