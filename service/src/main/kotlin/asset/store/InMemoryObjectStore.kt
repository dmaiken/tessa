package io.asset.store

import asset.StoreAssetRequest
import asset.store.FetchResult
import asset.store.ObjectStore
import asset.store.PersistResult
import io.asset.AssetAndVariant
import java.io.OutputStream
import java.util.UUID

class InMemoryObjectStore() : ObjectStore {
    companion object {
        const val DEFAULT_PORT = 8080
        const val BUCKET = "bucket"
    }

    private val store = mutableMapOf<String, ByteArray>()

    override suspend fun persist(
        data: StoreAssetRequest,
        image: ByteArray,
    ): PersistResult {
        val key = UUID.randomUUID().toString()
        store.put(key, image)

        return PersistResult(
            key = key,
            bucket = BUCKET,
        )
    }

    override suspend fun fetch(
        bucket: String,
        key: String,
        stream: OutputStream,
    ): FetchResult {
        if (bucket != BUCKET) {
            return FetchResult(
                found = false,
                contentLength = 0,
            )
        }
        return store[key]?.let {
            stream.write(it)
            FetchResult(
                found = true,
                contentLength = it.size.toLong(),
            )
        } ?: FetchResult(
            found = false,
            contentLength = 0,
        )
    }

    override suspend fun delete(
        bucket: String,
        key: String,
    ) {
        if (bucket != BUCKET) {
            return
        }
        store.remove(key)
    }

    override suspend fun deleteAll(
        bucket: String,
        keys: List<String>,
    ) {
        if (bucket != BUCKET) {
            return
        }
        keys.forEach { delete(bucket, it) }
    }

    override fun generateObjectUrl(assetAndVariant: AssetAndVariant): String {
        return "http://localhost:$DEFAULT_PORT/objectStore/${assetAndVariant.variant.objectStoreBucket}" +
            "/${assetAndVariant.variant.objectStoreKey}"
    }

    fun clearObjectStore() {
        store.clear()
    }
}
