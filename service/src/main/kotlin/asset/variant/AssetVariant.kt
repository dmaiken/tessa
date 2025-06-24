package io.asset.variant

import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_VARIANT_CREATED_AT
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ATTRIBUTES
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.OBJECT_STORE_BUCKET
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.OBJECT_STORE_KEY
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ORIGINAL_VARIANT
import kotlinx.serialization.json.Json
import org.jooq.Record
import java.time.LocalDateTime

data class AssetVariant(
    val objectStoreBucket: String,
    val objectStoreKey: String,
    val attributes: ImageVariantAttributes,
    val isOriginalVariant: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object Factory {
        fun from(record: Record): AssetVariant {
            return AssetVariant(
                objectStoreBucket = record.get(OBJECT_STORE_BUCKET),
                objectStoreKey = record.get(OBJECT_STORE_KEY),
                attributes = Json.decodeFromString(record.get(ATTRIBUTES).data()),
                isOriginalVariant = record.get(ORIGINAL_VARIANT),
                createdAt = record.get(ASSET_VARIANT_CREATED_AT),
            )
        }
    }
}
