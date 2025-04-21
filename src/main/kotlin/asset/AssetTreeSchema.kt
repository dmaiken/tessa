package io.image

import io.image.store.ObjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import org.jooq.postgres.extensions.bindings.LtreeBinding
import org.jooq.postgres.extensions.types.Ltree.ltree
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.time.LocalDateTime
import java.util.*

data class Asset(
    val id: UUID = UUID.randomUUID(),
    val bucket: String,
    val storeKey: String?,
    val url: String,
    val mimeType: String,
    val alt: String?,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(record: Record): Asset = Asset(
            id = record.getValue("id", UUID::class.java),
            bucket = record.getValue("bucket", String::class.java),
            storeKey = record.getValue("store_key", String::class.java),
            url = record.getValue("url", String::class.java),
            mimeType = record.getValue("mime_type", String::class.java),
            alt = record.getValue("alt", String::class.java),
            createdAt = record.getValue("created_at", LocalDateTime::class.java)
        )
    }

    fun toResponse(): AssetResponse = AssetResponse(
        id = id,
        bucket = bucket,
        storeKey = storeKey,
        type = mimeType,
        alt = alt,
        createdAt = createdAt
    )
}

interface AssetService {
    suspend fun store(data: StoreAssetRequest, content: ByteArray): Asset
    suspend fun fetch(id: UUID): Asset?
}

class AssetServiceImpl(
    private val dslContext: DSLContext,
    private val objectStore: ObjectStore
) : AssetService {

    companion object {
        const val ROOT_PATH = "asset"
    }

    override suspend fun store(data: StoreAssetRequest, content: ByteArray): Asset {
        if (!validate(content)) {
            throw InvalidImageException("Not an image type")
        }
        val persistResult = objectStore.persist(data, content)

        dslContext.insertInto(table("asset_tree"))
            .set(field("id"), data.id)
            .set(field("path", SQLDataType.VARCHAR.asConvertedDataType(LtreeBinding())), ltree(ROOT_PATH))
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

    private fun validate(image: ByteArray): Boolean {
        return URLConnection.guessContentTypeFromStream(ByteArrayInputStream(image))
            ?.startsWith("image/") == true
    }
}