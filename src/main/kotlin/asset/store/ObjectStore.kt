package io.image.store

import asset.StoreAssetRequest

interface ObjectStore {

    suspend fun persist(data: StoreAssetRequest, image: ByteArray): PersistResult
}

data class PersistResult(
    val key: String,
    val bucket: String,
    val url: String
)
