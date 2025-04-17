package io.image

import io.image.store.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.time.LocalDateTime
import java.util.*

data class Image(
    val id: UUID = UUID.randomUUID(),
    val bucket: String,
    val storeKey: String?,
    val url: String,
    val type: String,
    val alt: String?,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(record: Record): Image = Image(
            id = record.getValue("id", UUID::class.java),
            bucket = record.getValue("bucket", String::class.java),
            storeKey = record.getValue("store_key", String::class.java),
            url = record.getValue("url", String::class.java),
            type = record.getValue("type", String::class.java),
            alt = record.getValue("alt", String::class.java),
            createdAt = record.getValue("created_at", LocalDateTime::class.java)
        )
    }

    fun toResponse(): ImageResponse = ImageResponse(
        id = id,
        bucket = bucket,
        storeKey = storeKey,
        type = type,
        alt = alt,
        createdAt = createdAt
    )
}

interface ImageService {
    suspend fun storeImage(data: StoreImageRequest, content: ByteArray): Image
    suspend fun fetchImage(id: UUID): Image?
}

class ImageServiceImpl(
    private val dslContext: DSLContext,
    private val imageStore: ImageStore
) : ImageService {

    override suspend fun storeImage(data: StoreImageRequest, content: ByteArray) = withContext(Dispatchers.IO) {
        if (!validateImage(content)) {
            throw InvalidImageException("Not an image type")
        }
        val persistResult = imageStore.persist(data, content)
        dslContext.insertInto(table("images"))
            .columns(
                field("id"),
                field("bucket"),
                field("store_key"),
                field("url"),
                field("type"),
                field("alt"),
                field("created_at")
            ).values(
                data.id,
                persistResult.bucket,
                persistResult.key,
                persistResult.url,
                data.type,
                data.alt,
                data.createdAt
            ).awaitFirst()
        val result = fetchImage(data.id)!!

        result
    }

    override suspend fun fetchImage(id: UUID): Image? = withContext(Dispatchers.IO) {
        dslContext.select()
            .from(table("images"))
            .where(field("id").eq(id))
            .awaitFirstOrNull()?.let {
                Image.from(it)
            }
    }

    private fun validateImage(image: ByteArray): Boolean {
        return URLConnection.guessContentTypeFromStream(ByteArrayInputStream(image))
            ?.startsWith("image/") == true
    }
}