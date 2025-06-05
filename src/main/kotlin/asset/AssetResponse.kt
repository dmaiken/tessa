package asset

import io.serializers.LocalDateTimeSerializer
import io.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class AssetResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val bucket: String,
    val storeKey: String,
    val type: String,
    val alt: String?,
    val height: Int,
    val width: Int,
    val entryId: Long,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
)
