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
    val `class`: AssetClass,
    val alt: String?,
    val entryId: Long,
    val variants: List<AssetVariantResponse>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
)

@Serializable
data class AssetVariantResponse(
    val bucket: String,
    val storeKey: String,
    val imageAttributes: ImageAttributeResponse,
)

@Serializable
data class ImageAttributeResponse(
    val height: Int,
    val width: Int,
    val mimeType: String,
)

enum class AssetClass {
    IMAGE,
}
