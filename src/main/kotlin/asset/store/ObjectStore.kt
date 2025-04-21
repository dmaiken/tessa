package io.image.store

import io.image.StoreAssetRequest

interface ObjectStore {

    suspend fun persist(data: StoreAssetRequest, image: ByteArray): PersistResult
}

data class PersistResult(
    val key: String,
    val bucket: String,
    val url: String
)
