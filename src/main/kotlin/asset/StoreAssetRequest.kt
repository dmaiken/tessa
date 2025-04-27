package asset

import io.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class StoreAssetRequest(
    val fileName: String?,
    val type: String,
    val alt: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDateTime = LocalDateTime.now()
)