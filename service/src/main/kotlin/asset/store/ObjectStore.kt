package asset.store

import asset.StoreAssetRequest
import io.asset.AssetAndVariants
import java.io.OutputStream

interface ObjectStore {
    suspend fun persist(
        data: StoreAssetRequest,
        image: ByteArray,
    ): PersistResult

    suspend fun fetch(
        bucket: String,
        key: String,
        stream: OutputStream,
    ): FetchResult

    /**
     * Delete an object by key. This method is idempotent and will not throw an exception if the object does not exist
     */
    suspend fun delete(
        bucket: String,
        key: String,
    )

    suspend fun deleteAll(
        bucket: String,
        keys: List<String>,
    )

    fun generateObjectUrl(assetAndVariant: AssetAndVariants): String
}

data class PersistResult(
    val key: String,
    val bucket: String,
)

data class FetchResult(
    val found: Boolean,
    val contentLength: Long,
) {
    companion object {
        fun notFound() = FetchResult(false, 0)

        fun found(contentLength: Long) = FetchResult(true, contentLength)
    }
}
