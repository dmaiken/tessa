package asset.variant

import kotlinx.serialization.Serializable

@Serializable
data class ImageVariantAttributes(
    val height: Int,
    val width: Int,
    val mimeType: String,
)
