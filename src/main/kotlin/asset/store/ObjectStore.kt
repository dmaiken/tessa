package io.image.store

import asset.StoreAssetRequest

interface ObjectStore {

    suspend fun persist(data: StoreAssetRequest, image: ByteArray): PersistResult

    /**
     * Delete an object by key. This method is idempotent and will not throw an exception if the object does not exist
     */
    suspend fun delete(bucket: String, key: String)
}

data class PersistResult(
    val key: String,
    val bucket: String,
    val url: String
)
