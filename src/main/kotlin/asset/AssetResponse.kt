package io.image

import io.serializers.LocalDateSerializer
import io.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AssetResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val bucket: String,
    val storeKey: String?,
    val type: String,
    val alt: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDateTime
)
