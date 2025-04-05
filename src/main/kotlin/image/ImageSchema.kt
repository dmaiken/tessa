package io.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.time.LocalDateTime
import java.util.*

data class Image(
    val id: UUID = UUID.randomUUID(),
    val fileName: String?,
    val type: String,
    val alt: String?,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(record: Record): Image = Image(
            id = record.getValue("id", UUID::class.java),
            fileName = record.getValue("file_name", String::class.java),
            type = record.getValue("type", String::class.java),
            alt = record.getValue("alt", String::class.java),
            createdAt = record.getValue("created_at", LocalDateTime::class.java)
        )
    }

    fun toResponse(): ImageResponse = ImageResponse(
        id = id,
        fileName = fileName,
        type = type,
        alt = alt,
        createdAt = createdAt
    )
}

interface ImageService {
    suspend fun createImage(image: StoreImageRequest): Image
    suspend fun fetchImage(id: UUID): Image?
}

class ImageServiceImpl(private val dslContext: DSLContext) : ImageService {

    override suspend fun createImage(image: StoreImageRequest) = withContext(Dispatchers.IO) {
        dslContext.insertInto(table("images"))
            .columns(
                field("id"),
                field("file_name"),
                field("type"),
                field("alt"),
                field("created_at")
            ).values(
                image.id,
                image.fileName,
                image.type,
                image.alt,
                image.createdAt
            ).awaitFirst()
        val result = fetchImage(image.id)!!

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
}