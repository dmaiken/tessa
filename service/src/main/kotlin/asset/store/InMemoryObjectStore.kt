package asset.store

import asset.model.AssetAndVariants
import asset.model.StoreAssetRequest
import java.io.OutputStream
import java.util.UUID

class InMemoryObjectStore() : ObjectStore {
    companion object {
        const val DEFAULT_PORT = 8080
        const val BUCKET = "assets"
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

    override fun generateObjectUrl(assetAndVariant: AssetAndVariants): String {
        return "http://localhost:$DEFAULT_PORT/objectStore/${assetAndVariant.getOriginalVariant().objectStoreBucket}" +
            "/${assetAndVariant.getOriginalVariant().objectStoreKey}"
    }

    fun clearObjectStore() {
        store.clear()
    }
}
