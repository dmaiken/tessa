package asset.store

import asset.variant.AssetVariant
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class InMemoryObjectStore() : ObjectStore {
    companion object {
        const val DEFAULT_PORT = 8080
        const val BUCKET = "assets"
    }

    private val store = mutableMapOf<String, ByteArray>()

    override suspend fun persist(asset: InputStream): PersistResult {
        val key = UUID.randomUUID().toString()
        store.put(key, asset.readAllBytes())

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

    override fun generateObjectUrl(variant: AssetVariant): String {
        return "http://localhost:$DEFAULT_PORT/objectStore/${variant.objectStoreBucket}" +
            "/${variant.objectStoreKey}"
    }

    fun clearObjectStore() {
        store.clear()
    }
}
