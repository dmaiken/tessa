package asset

import io.image.AssetResponse
import org.jooq.Record
import java.time.LocalDateTime
import java.util.*

data class Asset(
    val id: UUID = UUID.randomUUID(),
    val bucket: String,
    val storeKey: String,
    val url: String,
    val mimeType: String,
    val alt: String?,
    val height: Int,
    val width: Int,
    val entryId: Long,
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
            height = record.getValue("height", Int::class.java),
            width = record.getValue("width", Int::class.java),
            entryId = record.getValue("entry_id", Long::class.java),
            createdAt = record.getValue("created_at", LocalDateTime::class.java)
        )
    }

    fun toResponse(): AssetResponse = AssetResponse(
        id = id,
        bucket = bucket,
        storeKey = storeKey,
        type = mimeType,
        alt = alt,
        height = height,
        width = width,
        entryId = entryId,
        createdAt = createdAt
    )
}