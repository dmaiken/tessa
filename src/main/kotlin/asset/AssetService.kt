package io.asset

import asset.Asset
import io.image.ImageProcessor
import io.image.InvalidImageException
import io.image.StoreAssetRequest
import io.image.store.ObjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import org.jooq.postgres.extensions.bindings.LtreeBinding
import org.jooq.postgres.extensions.types.Ltree.ltree
import java.util.*

interface AssetService {
    suspend fun store(data: StoreAssetRequest, content: ByteArray): Asset
    suspend fun fetch(id: UUID): Asset?
}

class AssetServiceImpl(
    private val mimeTypeDetector: MimeTypeDetector,
    private val dslContext: DSLContext,
    private val objectStore: ObjectStore,
    private val imageProcessor: ImageProcessor,
) : AssetService {

    companion object {
        const val ROOT_PATH = "asset"
    }

    override suspend fun store(data: StoreAssetRequest, content: ByteArray): Asset {
        val mimeType = mimeTypeDetector.detect(content)
        if (!validate(mimeType)) {
            throw InvalidImageException("Not an image type")
        }
        val preProcessed = imageProcessor.preprocess(content, mimeType)
        val persistResult = objectStore.persist(data, preProcessed.image)

        dslContext.insertInto(table("asset_tree"))
            .set(field("id"), data.id)
            .set(field("path", SQLDataType.VARCHAR.asConvertedDataType(LtreeBinding())), ltree(ROOT_PATH))
            .set(field("height"), preProcessed.attributes.height)
            .set(field("width"), preProcessed.attributes.width)
            .set(field("bucket"), persistResult.bucket)
            .set(field("store_key"), persistResult.key)
            .set(field("url"), persistResult.url)
            .set(field("mime_type"), data.type)
            .set(field("alt"), data.alt)
            .set(field("created_at"), data.createdAt)
            .awaitFirst()
        return fetch(data.id)!!
    }

    override suspend fun fetch(id: UUID): Asset? = withContext(Dispatchers.IO) {
        dslContext.select()
            .from(table("asset_tree"))
            .where(field("id").eq(id))
            .awaitFirstOrNull()?.let {
                Asset.from(it)
            }
    }

    private fun validate(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }
}