package asset

import kotlinx.serialization.Serializable

@Serializable
data class StoreAssetRequest(
    val fileName: String?,
    val type: String,
    val alt: String?,
)
