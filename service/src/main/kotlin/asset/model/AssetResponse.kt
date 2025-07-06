package asset.model

import io.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class AssetResponse(
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
